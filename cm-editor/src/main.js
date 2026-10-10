import "./css/style.css";
import EditorBridge from "./js/editor-bridge.js";
import { showDashboard } from "./js/dashboard.js";

window.onload = function () {
    document.getElementById("app").innerHTML = "<div id='editor'></div><div id='dashboard'></div>";

    // Only expose editor bridge
    window.editorBridge = new EditorBridge(document.getElementById("editor"));

    // You can set it "true" then run "npm run dev" for development/test on a browser
    showDashboard(false);
};
