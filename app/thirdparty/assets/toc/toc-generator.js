let headers = [];
let reloaded = true;

function isReloaded() {
    if (reloaded) {
        reloaded = false;
        return true;
    }
    return false;
}

function generate() {
    headers = document.querySelectorAll("h1, h2, h3, h4, h5, h6");
    if (headers.length == 0) {
        return "";
    }

    let padding = [0, 12, 24, 36, 48, 60, 72];
    let container = document.createElement("div");
    for (let i = 0, id = 0, level = 0; i < headers.length; i++, id++) {
        level = parseInt(headers[i].nodeName.charAt(1));
        let div = document.createElement("div");
        div.setAttribute("style", "margin-left:" + padding[level - 1] + "px");
        div.classList.add("title");

        let a = document.createElement("a");
        a.innerText = headers[i].innerText;
        a.setAttribute("href", "#a" + id);
        headers[i].setAttribute("id", "a" + id);

        div.appendChild(a);
        container.appendChild(div);
    }

    let id = locate();
    if (id.length > 0) {
        let current = container.querySelector("a[href='#" + id + "']");
        current.classList.add("current");
    }

    let html = "<html><head>";
    html += "<link href='file:///android_asset/toc/toc.css' rel='stylesheet' type='text/css'>";
    html += "</head><body>";
    html += container.innerHTML;
    html += "<script src='file:///android_asset/toc/toc.js'></script>";
    html += "</body></html>";

    return html;
}

function locate() {
    if (headers.length == 0) {
        return "";
    }
    if (headers.length == 1) {
        return headers[0].id;
    }

    let i = 1;
    for (; i < headers.length; i++) {
        let lastHeaderTop = headers[i - 1].getBoundingClientRect().top;
        let currentHeaderTop = headers[i].getBoundingClientRect().top;

        if (lastHeaderTop < 0) {
            if (currentHeaderTop >= 0) {
                if (currentHeaderTop < window.innerHeight) {
                    return headers[i].id;
                } else {
                    return headers[i - 1].id;
                }
            }
        } else {
            if (lastHeaderTop < window.innerHeight) {
                return headers[i - 1].id;
            } else {
                return "";
            }
        }
    }
    return headers[i - 1].id;
}
