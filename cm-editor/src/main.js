import './css/style.css';
import EditorBridge from './js/editor-bridge.js';
import { toggleDashboard } from './js/dashboard.js';

window.onload = function () {
    document.querySelector('#app').innerHTML = "<div id='editor'></div><div id='dashboard'></div>";

    // Only expose editor bridge
    window.editorBridge = new EditorBridge(document.querySelector('#editor'));

    // You can set it "true" then run "npm run dev" for development/test on a browser
    toggleDashboard(false);
};
