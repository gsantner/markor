let highlighted = document.createElement("a");

window.onload = function () {
    document.querySelectorAll(".title").forEach((element) => {
        element.addEventListener("click", function (event) {
            event.stopPropagation();
            element.querySelector("a").click();
        });
    });

    document.querySelectorAll("a").forEach((element) => {
        element.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            if (element != highlighted) {
                element.style.color = "black";
                element.style.fontWeight = "bold";
                element.classList.add("current");

                highlighted.style.color = "gray";
                highlighted.style.fontWeight = "normal";
                highlighted.classList.remove("current");
                highlighted = element;
            }

            let href = element.getAttribute("href");
            if (href) {
                scrollIntoView(href);
            }
        });
    });

    highlight();
};

function highlight() {
    let element = document.querySelector(".current");
    if (element) {
        highlighted = element;
        element.style.color = "black";
        element.style.fontWeight = "bold";
        element.parentNode.scrollIntoView();
    }
}

function highlightById(id) {
    if (id.length == 0) {
        return;
    }

    let elements = document.querySelectorAll(".current");
    elements.forEach((element) => {
        element.style.color = "gray";
        element.style.fontWeight = "normal";
        element.classList.remove("current");
    });

    let current = document.querySelector("a[href='#" + id + "']");
    current.classList.add("current");

    highlight();
}

function scrollIntoView(href) {
    injectedObject.run(href);
}
