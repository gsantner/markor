function wrapCodeBlockWords() {
    let preElements = document.querySelectorAll("pre[class*='language-']");
    preElements.forEach((element) => {
        element.style.wordWrap = "break-word";
        element.style.whiteSpace = "pre-wrap";
    });

    let codeElements = document.querySelectorAll("pre[class*='language-'] > code");
    codeElements.forEach((element) => {
        element.style.wordWrap = "break-word";
        element.style.whiteSpace = "pre-wrap";
    });
}
