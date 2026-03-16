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

export default Dashboard;
