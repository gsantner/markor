/**
 * Dashboard is used for development, not production.
 */
class Dashboard {
    constructor() {
        this.buttonList = document.createElement('ol');
        this.buttonList.style.listStyleType = 'none';
        this.buttonList.style.padding = 0;
        this.buttonList.style.margin = 0;
        this.#getDashboard().appendChild(this.buttonList);
    }

    #getDashboard() {
        return document.getElementById('dashboard');
    }

    addButton(text, onclick) {
        let button = document.createElement('button');
        button.innerText = text;
        button.onclick = onclick;

        let li = document.createElement('li');
        li.appendChild(button);

        this.buttonList.appendChild(li);
    }

    show() {
        let dashboard = this.#getDashboard();
        if (dashboard.children.length > 0) {
            dashboard.style.display = 'block';
        }
    }

    hide() {
        let dashboard = this.#getDashboard();
        if (dashboard.style.display != 'none') {
            dashboard.style.display = 'none';
        }
    }
}

export default Dashboard;
