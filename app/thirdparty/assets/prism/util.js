function wrapCodeBlockWords() {
    let preElements = document.querySelectorAll("pre[class*='language-']");
    preElements.forEach((element) => {
        element.style.whiteSpace = "pre-wrap";
        element.style.overflowWrap = "break-word";
    });
}
