import { EditorView, basicSetup } from "codemirror"
import { EditorState } from "@codemirror/state"
import { history, undo, redo } from "@codemirror/commands";
import { html } from "@codemirror/lang-html"
import { oneDark } from "@codemirror/theme-one-dark"
import { isDarkMode } from "./theme.js";
// import { callback } from "./test";

class EditorBridge {
  constructor(element) {
    const isDarkTheme = isDarkMode();

    const theme = EditorView.theme({
      "&": { height: "100%", fontSize: "16px" },
      ".cm-scroller": { overflow: "auto" }
    }, { dark: isDarkTheme });

    const exts = [
      basicSetup,
      theme,
      history(),
      html(),
      EditorView.lineWrapping
    ];

    if (isDarkTheme) {
      exts.push(oneDark);
    }

    const state = EditorState.create({
      doc: "",
      extensions: exts
    });

    this.editor = new EditorView({ state, parent: element });

    // this.setText(`<!DOCTYPE html>\n<!-- Hello, CodeMirror! -->`);
  }

  setText(text) {
    // https://stackoverflow.com/questions/72716094/how-to-programmatically-change-the-editors-value-in-codemirror-6/
    this.editor.dispatch({
      changes: { from: 0, to: this.editor.state.doc.length, insert: text }
    });
  }

  getText() {
    // https://stackoverflow.com/questions/72982051/how-to-get-the-text-value-of-a-codemirror-6-editor/
    return this.editor.state.doc.toString();
  }

  undo() {
    undo(this.editor);
  }

  redo() {
    redo(this.editor);
  }
}

export default EditorBridge;
