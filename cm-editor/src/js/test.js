import Dashboard from './dashboard.js'

export function callback() {
    if (typeof callbackInterface === 'undefined') {
        console.log("'callbackInterface' is undefined");
    } else {
        callbackInterface.callback('$focus');
    }
}

let dashboard;

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

let logElement;

export function getLog() {
    if (dashboard == null) {
        dashboard = setupDashboard();
    }
    return {
        set: (text) => {
            if (dashboard.isHide()) {
                return;
            }
            if (logElement) {
                logElement.value = text;
            }
        },
        append: (text) => {
            if (logElement) {
                logElement.value += text + '\n';
            }
        }
    };
}

// For development
function setupDashboard() {
    const dashboard = new Dashboard();

    dashboard.addButton('setText', () => editorBridge.setText('// Hello'));
    dashboard.addButton('getText', () => console.log(editorBridge.getText()));
    dashboard.addButton('undo', () => editorBridge.undo());
    dashboard.addButton('redo', () => editorBridge.redo());

    logElement = dashboard.addView('textarea', 'log');
    logElement.readOnly = true;
    logElement.style.width = '100px';
    logElement.style.height = '100px';
    logElement.style.marginTop = '12px';
    dashboard.addButton('clear', () => getLog().set(''));

    return dashboard;
}
