import './css/style.css'
import EditorBridge from './js/editor-bridge.js'
import Dashboard from './js/dashboard.js'

document.querySelector('#app').innerHTML = "<div id='editor'></div><div id='dashboard'></div>"

const bridge = new EditorBridge(document.querySelector('#editor'));

// Expose editor bridge
window.editorBridge = bridge;

// For development
function setupDashboard() {
    const dashboard = new Dashboard();
    dashboard.addButton('setText', () => editorBridge.setText('// Hello'));
    dashboard.addButton('getText', () => console.log(editorBridge.getText()));
    dashboard.addButton('undo', () => editorBridge.undo());
    dashboard.addButton('redo', () => editorBridge.redo());
    dashboard.show();
}

// setupDashboard();
