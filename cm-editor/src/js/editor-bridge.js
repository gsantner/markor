import { EditorView, basicSetup, minimalSetup } from "codemirror";
import { EditorState, Compartment } from "@codemirror/state";
import { undo, redo, undoDepth, redoDepth, defaultKeymap, history, historyKeymap } from "@codemirror/commands";
import {
  lineNumbers,
  keymap,
  highlightSpecialChars,
  drawSelection,
  dropCursor,
  rectangularSelection,
  crosshairCursor,
  highlightActiveLine, highlightActiveLineGutter
} from "@codemirror/view";
import { language, bracketMatching, foldGutter, foldKeymap, indentOnInput, syntaxHighlighting, defaultHighlightStyle } from "@codemirror/language";
import { closeBrackets, closeBracketsKeymap, autocompletion, completionKeymap } from "@codemirror/autocomplete";
import { css } from "@codemirror/lang-css";
import { javascript, javascriptLanguage, scopeCompletionSource } from "@codemirror/lang-javascript";
import { html } from "@codemirror/lang-html";
import { markdown } from "@codemirror/lang-markdown";
import { oneDark } from "@codemirror/theme-one-dark";
import { isDarkMode } from "./theme.js";
import { getLog } from "./dashboard.js";
import { readText, onTextChanged } from "./callback-interface.js"
import { lintKeymap } from "@codemirror/lint";

class EditorBridge {
  constructor(element) {
    this.lineNumbersCompartment = new Compartment();
    this.languageCompartment = new Compartment();
    this.completionCompartment = new Compartment();
    this.themeCompartment = new Compartment();
    this.lineWrappingCompartment = new Compartment();

    this.lineNumbersEnabled = false;

    const isDarkTheme = isDarkMode();
    this.style = {
      "&": { height: "100%", fontSize: "16px" },
      ".cm-scroller": { overflow: "auto" }
    };
    const theme = EditorView.theme(this.style, { dark: isDarkTheme });

    const blurHandler = EditorView.domEventHandlers({
      blur(event, view) {
        view.contentDOM.focus({ preventScroll: true });
        return false;
      }
    });

    javascriptLanguage.data.of({
      autocomplete: scopeCompletionSource(window)
    });

    /*
    const basicExtensions = [
      highlightSpecialChars(),
      history(),
      drawSelection(),
      dropCursor(),
      EditorState.allowMultipleSelections.of(true),
      indentOnInput(),
      syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
      bracketMatching(),
      closeBrackets(),
      autocompletion(),
      rectangularSelection(),
      crosshairCursor(),
      keymap.of([
        ...closeBracketsKeymap,
        ...defaultKeymap,
        ...historyKeymap,
        ...completionKeymap,
        ...lintKeymap
      ])
    ]; */

    // The function that handles the saved content
    this.handleTextChange = this.debounce((newText) => {
      // getLog().append("Text changed: " + newText);
      onTextChanged(newText, this.getUndoDepth(), this.getRedoDepth());
    }, 500); // Waits for 500ms of silence before running

    this.editorExtensions = [
      minimalSetup,
      bracketMatching(),
      highlightActiveLine(), highlightActiveLineGutter(),
      this.themeCompartment.of(theme),
      this.lineNumbersCompartment.of([]),
      this.languageCompartment.of([]),
      this.completionCompartment.of([]),
      this.lineWrappingCompartment.of([EditorView.lineWrapping]),
      blurHandler,
      EditorView.updateListener.of((update) => {
        if (update.docChanged) {
          this.handleTextChange(update.state.doc.toString());
        }
      })
    ];

    if (isDarkTheme) {
      this.editorExtensions.push(oneDark);
    }

    const that = this;
    const state = EditorState.create({
      doc: "",
      extensions: that.editorExtensions
    });

    this.view = new EditorView({ state, parent: element });
  }

  focus() {
    this.view.focus();
  }

  requestMeasure() {
    this.view.requestMeasure();
  }

  /**
   * Get current code language name.
   * @return the name of current code language
   */
  getLanguageName() {
    const lang = this.view.state.facet(language);
    return lang ? lang.name : "plain-text";
  }

  setLineNumbers(enabled) {
    const that = this;
    if (enabled) {
      that.view.dispatch({
        effects: that.lineNumbersCompartment.reconfigure(lineNumbers())
      });
    } else {
      that.view.dispatch({
        effects: that.lineNumbersCompartment.reconfigure([])
      });
    }
    this.lineNumbersEnabled = enabled;
  }

  setFontSize(fontSize) {
    this.style["&"].fontSize = fontSize;
    const theme = EditorView.theme(this.style, { dark: isDarkMode() });

    const that = this;
    this.view.dispatch({
      effects: that.themeCompartment.reconfigure([theme])
    });
  }

  setLineWrapping(enabled) {
    const that = this;
    that.view.dispatch({
      effects: that.lineWrappingCompartment.reconfigure(enabled ? EditorView.lineWrapping : [])
    });
  }

  /**
   * Reconfigure code language.
   * @param {*} language the LanguageSupport object
   */
  reconfigureLanguage(language) {
    const that = this;
    if (language) {
      this.view.dispatch({
        effects: that.languageCompartment.reconfigure(language())
      });
    } else {
      this.view.dispatch({
        effects: that.languageCompartment.reconfigure([])
      });
    }
  }

  /**
   * Set code language.
   * @param {*} name the code language name
   */
  setCodeLanguage(name) {
    const currentLanguage = this.getLanguageName;
    if (currentLanguage === name) {
      return;
    }

    if (currentLanguage === "javascript") {
      this.clearInjectedCompletion();
    }

    if (name === "css") {
      this.reconfigureLanguage(css);
    } else if (name === "javascript") {
      this.reconfigureLanguage(javascript);
      this.injectJavaScriptCompletion();
    } else if (name === "html") {
      this.reconfigureLanguage(html);
    } else if (name === "markdown") {
      this.reconfigureLanguage(markdown);
    } else {
      this.reconfigureLanguage(null);
    }
  }

  /**
   * Inject JavaScript completion.
   */
  injectJavaScriptCompletion() {
    const that = this;
    const completion = javascriptLanguage.data.of({
      autocomplete: scopeCompletionSource(window)
    });
    this.view.dispatch({
      effects: that.completionCompartment.reconfigure(completion)
    });
  }

  /**
   * Clear injected language completion.
   */
  clearInjectedCompletion() {
    const that = this;
    this.view.dispatch({
      effects: that.completionCompartment.reconfigure([])
    });
  }

  /**
   * Set text and reset state.
   * @param {*} text the text
   */
  resetText(text) {
    const that = this;
    const newState = EditorState.create({
      doc: text,
      extensions: that.exts
    });
    this.view.setState(newState);
  }

  /**
   * Load text and reset state.
   * This method can load large text with file size less than 2 MB.
   * 
   * @param {*} path the text file path
   */
  async loadText(path) {
    this.resetText(readText(path));
  }

  insert(start, end, text) {
    // https://stackoverflow.com/questions/72716094/how-to-programmatically-change-the-editors-value-in-codemirror-6/
    this.view.dispatch({
      changes: {
        from: start, to: end, insert: text
      }
    });
  }

  insertAtCursor(text) {
    const selectionHead = this.view.state.selection.main.head;

    this.view.dispatch({
      changes: {
        from: selectionHead,
        to: selectionHead,
        insert: text
      },
      selection: {
        anchor: selectionHead + text.length
      },
      // scrollIntoView: true
    });
  }

  setText(text) {
    this.insert(0, this.length(), text);
  }

  getText() {
    // https://stackoverflow.com/questions/72982051/how-to-get-the-text-value-of-a-codemirror-6-editor/
    return this.view.state.doc.toString();
  }

  length() {
    return this.view.state.doc.length;
  }

  undo() {
    undo(this.view);
  }

  redo() {
    redo(this.view);
  }

  getUndoDepth() {
    return undoDepth(this.view.state);
  }

  getRedoDepth() {
    return redoDepth(this.view.state);
  }

  moveCursor(distance) {
    const head = this.view.state.selection.main.head;
    let newHead = head + distance;
    if (newHead < 0) {
      newHead = 0;
    }

    this.view.dispatch({
      selection: {
        anchor: newHead
      },
      // scrollIntoView: true
    });
  }

  debounce(func, delay) {
    let timeout;
    return (...args) => {
      clearTimeout(timeout);
      timeout = setTimeout(() => func(...args), delay);
    };
  }
}

export default EditorBridge;
