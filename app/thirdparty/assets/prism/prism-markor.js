function usePrism(arg1, arg2) {
    const codeElements = document.querySelectorAll("pre > code");
    if (codeElements.length == 0) {
        return;
    }
    const wrapWords = arg1 === "true";
    const lineNumbers = arg2 === "true";
    const codeFontWidth = getFontWidth(codeElements[0]);

    codeElements.forEach((codeElement) => {
        codeElement.parentNode.style.paddingLeft = "8px";
        codeElement.parentNode.style.paddingRight = "8px";
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
            preparePaddings(codeElement, codeFontWidth);
            codeElement.parentNode.classList.add("line-numbers");
            Prism.highlightElement(codeElement);
        }
    });
}

function refreshLineNumbers() {
    const codeElements = document.querySelectorAll("pre > code");
    if (codeElements.length == 0) {
        return;
    }
    const codeFontWidth = getFontWidth(codeElements[0]);
    codeElements.forEach((codeElement) => {
        preparePaddings(codeElement, codeFontWidth);
        Prism.highlightElement(codeElement);
    });
}

function setLineNumbers(enabled) {
    const codeElements = document.querySelectorAll("pre > code");
    if (codeElements.length == 0) {
        return;
    }
    const lineNumbers = enabled === "true";

    if (lineNumbers) {
        const codeFontWidth = getFontWidth(codeElements[0]);
        codeElements.forEach((codeElement) => {
            preparePaddings(codeElement, codeFontWidth);
            codeElement.parentNode.classList.add("line-numbers");
            Prism.highlightElement(codeElement);
        });
    } else {
        codeElements.forEach((codeElement) => {
            codeElement.parentNode.classList.remove("line-numbers");
            codeElement.parentNode.style.paddingLeft = "8px";
            Prism.highlightElement(codeElement);
        });
    }
}

function setWrapWords(enabled) {
    const codeElements = document.querySelectorAll("pre > code");
    if (codeElements.length == 0) {
        return;
    }
    const wrapWords = enabled === "true";

    if (wrapWords) {
        codeElements.forEach((codeElement) => {
            codeElement.parentNode.style.whiteSpace = "pre-wrap";
            codeElement.parentNode.style.overflowWrap = "break-word";
            Prism.highlightElement(codeElement);
        });
    } else {
        codeElements.forEach((codeElement) => {
            codeElement.parentNode.style.whiteSpace = null;
            codeElement.parentNode.style.overflowWrap = null;
            Prism.highlightElement(codeElement);
        });
    }
}
