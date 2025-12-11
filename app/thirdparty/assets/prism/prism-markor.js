function usePrism(arg1, arg2) {
    const codeElements = document.querySelectorAll("pre > code");
    if (codeElements.length == 0) {
        return;
    }
    const wrapWords = arg1 === "true";
    const lineNumbers = arg2 === "true";
    const codeFontWidth = getFontWidth(codeElements[0]);

    codeElements.forEach((codeElement) => {
        if (wrapWords) {
            codeElement.parentNode.style.whiteSpace = "pre-wrap";
            codeElement.parentNode.style.overflowWrap = "break-word";
        }

        if (codeElement.getAttribute("class") == null) {
            codeElement.classList.add("language-text");
            if (!lineNumbers) {
                Prism.highlightElement(codeElement);
            }
        }

        if (lineNumbers) {
            adjustLayout(codeElement, codeFontWidth);
            codeElement.parentNode.classList.add("line-numbers");
            Prism.highlightElement(codeElement);
        }
    });
}

function refreshPrism() {
    const codeElements = document.querySelectorAll("pre > code");
    if (codeElements.length == 0) {
        return;
    }
    const codeFontWidth = getFontWidth(codeElements[0]);
    codeElements.forEach((codeElement) => {
        adjustLayout(codeElement, codeFontWidth);
        Prism.highlightElement(codeElement);
    });
}
