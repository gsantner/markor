import { EditorView, minimalSetup } from "codemirror";
import { EditorState, Compartment } from "@codemirror/state";
import { undo, redo, undoDepth, redoDepth } from "@codemirror/commands";
import { lineNumbers, highlightActiveLine, highlightActiveLineGutter } from "@codemirror/view";
import { language, bracketMatching } from "@codemirror/language";
import { css } from "@codemirror/lang-css";
import { javascript, javascriptLanguage, scopeCompletionSource } from "@codemirror/lang-javascript";
import { html } from "@codemirror/lang-html";
import { markdown } from "@codemirror/lang-markdown";
import { oneDark } from "@codemirror/theme-one-dark";
import { readText, onTextChanged } from "./callback-interface.js"

class EditorBridge {
  constructor(element) {
    this.themeCompartment = new Compartment();
    this.lineWrappingCompartment = new Compartment();
    this.lineNumbersCompartment = new Compartment();
    this.languageCompartment = new Compartment();
    this.completionCompartment = new Compartment();

    this.lineWrappingEnabled = true;
    this.lineNumbersEnabled = false;
    this.codeLanguage = "";

    this.basicStyle = {
      "&": { height: "100%", fontSize: "16px" },
      ".cm-scroller": { overflow: "auto" }
    };
    this.basicTheme = EditorView.theme(this.basicStyle, { dark: this.shouldDarkMode() });

    const state = EditorState.create({
      doc: "",
      extensions: this.loadExtensions()
    });

    this.editorView = new EditorView({ state, parent: element });
    this.config();
  }

  loadExtensions() {
    const basicExtensions = [
      minimalSetup,
      bracketMatching(),
      highlightActiveLine(), highlightActiveLineGutter(),
      this.themeCompartment.of(this.basicTheme),
      this.lineWrappingCompartment.of([]),
      this.lineNumbersCompartment.of([]),
      this.languageCompartment.of([]),
      this.completionCompartment.of([])
    ];

    if (this.shouldDarkMode()) {
      basicExtensions.push(oneDark);
    }

    // onTextChanged callback setup
    const handleTextChange = this.debounce((newText) => {
      onTextChanged(newText, this.getUndoDepth(), this.getRedoDepth());
    }, 500);

    basicExtensions.push(EditorView.updateListener.of((update) => {
      if (update.docChanged) {
        handleTextChange(update.state.doc.toString());
      }
    }));

    return basicExtensions;
  }

  config() {
    this.setLineWrapping(this.lineWrappingEnabled);
    this.setLineNumbers(this.lineNumbersEnabled);
    this.setCodeLanguage(this.codeLanguage);
  }

  shouldDarkMode() {
    let content = document.querySelector('meta[name="theme"]').getAttribute('content');
    return content === "dark";
  }

  focus() {
    this.editorView.focus();
  }

  requestMeasure() {
    this.editorView.requestMeasure();
  }

  setLineWrapping(enabled) {
    this.editorView.dispatch({
      effects: this.lineWrappingCompartment.reconfigure(enabled ? EditorView.lineWrapping : [])
    });
    this.lineWrappingEnabled = enabled;
  }

  setLineNumbers(enabled) {
    this.editorView.dispatch({
      effects: this.lineNumbersCompartment.reconfigure(enabled ? lineNumbers() : [])
    });
    this.lineNumbersEnabled = enabled;
  }

  setFontSize(fontSize) {
    this.basicStyle["&"].fontSize = fontSize;
    const theme = EditorView.theme(this.basicStyle, { dark: this.shouldDarkMode() });
    this.editorView.dispatch({
      effects: this.themeCompartment.reconfigure([theme])
    });
  }

  /**
   * Set code language.
   * @param {*} lang the code language name
   */
  setCodeLanguage(lang) {
    const currentLanguage = this.getLanguage();
    if (currentLanguage === lang) {
      return;
    }

    if (currentLanguage === "javascript") {
      this.clearInjectedCompletion();
    }

    if (lang === "css") {
      this.reconfigureLanguage(css);
    } else if (lang === "javascript") {
      this.reconfigureLanguage(javascript);
      this.injectJavaScriptCompletion();
    } else if (lang === "html") {
      this.reconfigureLanguage(html);
    } else if (lang === "markdown") {
      this.reconfigureLanguage(markdown);
    } else {
      this.reconfigureLanguage(null);
    }

    this.codeLanguage = lang;
  }

  /**
   * Get current code language name.
   * @return the name of current code language
   */
  getLanguage() {
    // const lang = this.editorView.state.facet(language);
    if (this.codeLanguage && this.codeLanguage.length > 0) {
      return this.codeLanguage;
    } else {
      return "plain-text";
    }
  }

  /**
   * Reconfigure code language.
   * @param {*} language the LanguageSupport object
   */
  reconfigureLanguage(language) {
    if (language) {
      this.editorView.dispatch({
        effects: this.languageCompartment.reconfigure(language())
      });
    } else {
      this.editorView.dispatch({
        effects: this.languageCompartment.reconfigure([])
      });
    }
  }

  /**
   * Inject JavaScript completion.
   */
  injectJavaScriptCompletion() {
    const completion = javascriptLanguage.data.of({
      autocomplete: scopeCompletionSource(window)
    });
    this.editorView.dispatch({
      effects: this.completionCompartment.reconfigure(completion)
    });
  }

  /**
   * Clear injected language completion.
   */
  clearInjectedCompletion() {
    this.editorView.dispatch({
      effects: this.completionCompartment.reconfigure([])
    });
  }

  setText(text) {
    this.editorView.dispatch({
      changes: { from: 0, to: this.editorView.state.doc.length, insert: text }
    });
  }

  /**
   * Set text and reset state.
   * @param {*} text the text
   */
  reset(text) {
    const newState = EditorState.create({
      doc: text,
      extensions: this.loadExtensions()
    });
    this.editorView.setState(newState);
    this.config();
  }

  /**
   * Load text and reset state.
   * This method can load large text with file size less than 2 MB.
   * 
   * @param {*} path the text file path
   */
  async loadText(path) {
    this.setText(readText(path));
  }

  insert(start, end, text) {
    // https://stackoverflow.com/questions/72716094/how-to-programmatically-change-the-editors-value-in-codemirror-6/
    this.editorView.dispatch({
      changes: {
        from: start, to: end, insert: text
      }
    });
  }

  insertAtCursor(text) {
    const selectionHead = this.editorView.state.selection.main.head;

    this.editorView.dispatch({
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

  getText() {
    // https://stackoverflow.com/questions/72982051/how-to-get-the-text-value-of-a-codemirror-6-editor/
    return this.editorView.state.doc.toString();
  }

  length() {
    return this.editorView.state.doc.length;
  }

  undo() {
    undo(this.editorView);
  }

  redo() {
    redo(this.editorView);
  }

  getUndoDepth() {
    return undoDepth(this.editorView.state);
  }

  getRedoDepth() {
    return redoDepth(this.editorView.state);
  }

  moveCursor(distance) {
    const head = this.editorView.state.selection.main.head;
    let newHead = head + distance;
    if (newHead < 0) {
      newHead = 0;
    }

    this.editorView.dispatch({
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
