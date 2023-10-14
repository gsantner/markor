function adjustLineNumbers() {
    let preElements = document.querySelectorAll("pre[class*='language-']");

    preElements.forEach((element) => {
        let codeElement = element.querySelector("code");
        if (codeElement) {
            let maxNumber = codeElement.textContent.split("\n").length - 1;
            if (maxNumber > 0) {
                element.style.paddingLeft = 1.1 + getNumberDigits(maxNumber) * 0.5 + "em";
            }
        }
    });
}

function getNumberDigits(number) {
    if (number < 10) {
        return 1;
    } else if (number < 100) {
        return 2;
    } else if (number < 1000) {
        return 3;
    } else if (number < 10000) {
        return 4;
    } else {
        return 5;
    }
}
