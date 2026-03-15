import './css/style.css';
import EditorBridge from './js/editor-bridge.js';
import { toggleDashboard } from './js/test.js';

document.querySelector('#app').innerHTML = "<div id='editor'></div><div id='dashboard'></div>";

// Just expose editor bridge
window.editorBridge = new EditorBridge(document.querySelector('#editor'));

toggleDashboard(false);
