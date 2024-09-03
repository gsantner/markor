function enableLineNumbers() {
    document.body.classList.add("line-numbers");
}

function adjustLineNumbers() {
    const preElements = document.querySelectorAll("pre[class*='language-']");
    let fontWidth = -1;

    preElements.forEach((element) => {
        let codeElement = element.querySelector("code");
        if (codeElement) {
            const maxNumber = codeElement.textContent.split("\n").length - 1;
            if (maxNumber == 0) {
                return;
            }

            if (fontWidth == -1) {
                const canvasContext = document.createElement("canvas").getContext("2d");
                canvasContext.font = window.getComputedStyle(codeElement, null).getPropertyValue("font");
                fontWidth = canvasContext.measureText("0").width;
            }

            const digits = getNumberDigits(maxNumber);
            element.style.paddingLeft = 2 * fontWidth + digits * fontWidth - digits + "px";
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
