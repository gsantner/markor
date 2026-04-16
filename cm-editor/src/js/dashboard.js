/**
 * Dashboard is used for development, not production.
 */
class Dashboard {
    #viewList;

    constructor() {
        this.#viewList = document.createElement('ol');
        this.#viewList.style.listStyleType = 'none';
        this.#viewList.style.padding = 0;
        this.#viewList.style.margin = 0;
        this.#getDashboard().appendChild(this.#viewList);
    }

    #getDashboard() {
        return document.getElementById('dashboard');
    }

    addView(name, id, text) {
        let element = document.createElement(name);
        if (id) {
            element.id = id;
        }
        if (text) {
            element.innerText = text;
        }

        let li = document.createElement('li');
        li.appendChild(element);
        this.#viewList.appendChild(li);

        return element;
    }

    addButton(text, onclick) {
        this.addView('button', null, text).onclick = onclick;
    }

    isHide() {
        return this.#getDashboard().style.display === 'none';
    }

    show() {
        let dashboard = this.#getDashboard();
        if (dashboard.children.length > 0) {
            dashboard.style.display = 'block';
        }
    }

    hide() {
        let dashboard = this.#getDashboard();
        dashboard.style.display = 'none';
    }
}

//////

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

// For development, test buttons can be added here
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
