function usePrism(arg1, arg2) {
    const wrapWords = arg1 === "true";
    const lineNumbers = arg2 === "true";
    const codeElements = document.querySelectorAll("pre > code");

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
            adjustLayout(codeElement);
            codeElement.parentNode.classList.add("line-numbers");
            Prism.highlightElement(codeElement);
        }
    });
}
