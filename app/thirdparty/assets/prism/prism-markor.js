function usePrismCodeBlock() {
    elements = document.querySelectorAll("pre > code");
    elements.forEach(function (element) {
        if (!element.classList.contains("language-")) {
            element.classList.add("language-text");
        }
    });
}

function wrapCodeBlockWords() {
    let preElements = document.querySelectorAll("pre[class*='language-']");
    preElements.forEach((element) => {
        element.style.whiteSpace = "pre-wrap";
        element.style.overflowWrap = "break-word";
    });
}
