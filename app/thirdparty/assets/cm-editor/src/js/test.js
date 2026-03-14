import Dashboard from './dashboard.js'

// editorBridge.setText("hello");

export function callback() {
    if (typeof callbackInterface === 'undefined') {
        console.log("'callbackInterface' is undefined");
    } else {
        callbackInterface.callback('$focus');
    }
}

let dashboard;

// For development
export function toggleDashboard(show) {
    if (show) {
        if (dashboard == null) {
            dashboard = setupDashboard();
        }
        dashboard.show();
    } else if (dashboard) {
        dashboard.hide();
    }
}

function setupDashboard() {
    const dashboard = new Dashboard();
    dashboard.addButton('setText', () => editorBridge.setText('// Hello'));
    dashboard.addButton('getText', () => console.log(editorBridge.getText()));
    dashboard.addButton('undo', () => editorBridge.undo());
    dashboard.addButton('redo', () => editorBridge.redo());
    return dashboard;
}
