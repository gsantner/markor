/*#######################################################
 *
 *   Maintained 2023.12.27 by Li Guanglin
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

let headers = [];
let reloaded = true;

function generate() {
    headers = document.querySelectorAll("h1, h2, h3, h4, h5, h6");
    if (headers.length == 0) {
        return "";
    }

    let padding = [0, 12, 24, 36, 48, 60, 72];
    let level = 0;
    let id = 0;
    let contents = document.createElement("div");
    for (let i = 0; i < headers.length; i++) {
        level = parseInt(headers[i].nodeName.charAt(1));
        let title = document.createElement("div");
        title.setAttribute("style", "margin-left:" + padding[level - 1] + "px");
        title.classList.add("title");

        let a = document.createElement("a");
        a.innerText = headers[i].innerText;
        a.setAttribute("href", "#a" + id);
        headers[i].setAttribute("id", "a" + id);
        id++;

        title.appendChild(a);
        contents.appendChild(title);
    }

    id = locate();
    if (id.length > 0) {
        let current = contents.querySelector("a[href='#" + id + "']");
        current.classList.add("current");
    }

    let html = "<html><head>";
    html += "<link href='file:///android_asset/contents/contents.css' rel='stylesheet' type='text/css'>";
    html += "</head><body>";
    html += contents.innerHTML;
    html += "<script src='file:///android_asset/contents/contents.js'></script>";
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

    const MARGIN = 4;
    let innerHeight = window.innerHeight;
    let lastHeader = headers[0];
    for (let i = 1; i < headers.length; i++) {
        let headerTop = headers[i].getBoundingClientRect().top;
        let lastHeaderTop = lastHeader.getBoundingClientRect().top;

        if (lastHeaderTop < MARGIN) {
            if (headerTop < MARGIN) {
                continue;
            } else {
                if (headerTop < innerHeight + MARGIN) {
                    return headers[i].id;
                } else {
                    return lastHeader.id;
                }
            }
        } else {
            return lastHeader.id;
        }
    }
}

function isReloaded() {
    let temp = reloaded;
    reloaded = false;
    return temp;
}
