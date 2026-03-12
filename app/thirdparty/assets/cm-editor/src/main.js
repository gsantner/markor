import './css/style.css'
import EditorBridge from './js/editor-bridge.js'
import { setupDashboard } from './js/test.js'

document.querySelector('#app').innerHTML = "<div id='editor'></div><div id='dashboard'></div>"

const bridge = new EditorBridge(document.querySelector('#editor'));

// Expose editor bridge
window.editorBridge = bridge;

// setupDashboard();
