import { EditorView, basicSetup } from "codemirror";
import { EditorState } from "@codemirror/state";
import { undo, redo, undoDepth } from "@codemirror/commands";
import { html } from "@codemirror/lang-html";
import { oneDark } from "@codemirror/theme-one-dark";
import { isDarkMode } from "./theme.js";
// import { callback } from "./test";

class EditorBridge {
  constructor(element) {
    const isDarkTheme = isDarkMode();

    const theme = EditorView.theme({
      "&": { height: "100%", fontSize: "16px" },
      ".cm-scroller": { overflow: "auto" }
    }, { dark: isDarkTheme });

    this.exts = [
      basicSetup,
      theme,
      html()
    ];

    // this.exts.push(EditorView.lineWrapping);

    if (isDarkTheme) {
      exts.push(oneDark);
    }

    const that = this;
    const state = EditorState.create({
      doc: "",
      extensions: that.exts
    });

    this.view = new EditorView({ state, parent: element });

    // this.setText(`<!DOCTYPE html>\n<!-- Hello, CodeMirror! -->`);

    this.#setup();
  }

  #setup() {
    const that = this;
    document.querySelector('.cm-gutters').addEventListener('click', function () {
      that.view.contentDOM.focus({ preventScroll: true });
    });
  }

  focus() {
    this.view.contentDOM.focus();
  }

  /**
   * Set text and reset state.
   * @param {*} text the text
   */
  reset(text) {
    const that = this;
    const newState = EditorState.create({
      doc: text,
      extensions: that.exts
    });
    this.view.setState(newState);
  }

  insert(text, start, end) {
    // https://stackoverflow.com/questions/72716094/how-to-programmatically-change-the-editors-value-in-codemirror-6/
    this.view.dispatch({
      changes: {
        from: start, to: end, insert: text
      }
    });
  }

  setText(text) {
    this.insert(text, 0, this.length());
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
}

export default EditorBridge;
