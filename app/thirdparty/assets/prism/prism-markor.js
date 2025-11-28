function usePrism() {
    let elements = document.querySelectorAll("pre > code");
    elements.forEach(function (element) {
        let attribute = element.getAttribute("class");
        if (attribute == null || attribute.indexOf("language-") == -1) {
            element.classList.add("language-text");
            Prism.highlightElement(element);
        }
    });
}

function wrapCode() {
    let preElements = document.querySelectorAll("pre");
    preElements.forEach((element) => {
        element.style.whiteSpace = "pre-wrap";
        element.style.overflowWrap = "break-word";
    });
}
