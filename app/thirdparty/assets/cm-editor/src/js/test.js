import Dashboard from './dashboard.js'

// editorBridge.setText("hello");

export function callback() {
    if (typeof callbackInterface === 'undefined') {
        console.log("'callbackInterface' is undefined");
    } else {
        callbackInterface.callback('Hello from JavaScript');
    }
}

// For development
export function setupDashboard() {
    const dashboard = new Dashboard();
    dashboard.addButton('setText', () => editorBridge.setText('// Hello'));
    dashboard.addButton('getText', () => console.log(editorBridge.getText()));
    dashboard.addButton('undo', () => editorBridge.undo());
    dashboard.addButton('redo', () => editorBridge.redo());
    dashboard.show();
}
