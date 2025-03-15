function enableLineNumbers() {
    let elements = document.querySelectorAll("pre:has(code)");
    elements.forEach(function (element) {
        element.classList.add("line-numbers");
    });
    Prism.highlightAll();
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

function adjustLineNumbers() {
    let fontWidth = -1;
    const codeElements = document.querySelectorAll("pre > code");
    codeElements.forEach((element) => {
        const maxNumber = element.textContent.split("\n").length - 1;
        if (maxNumber == 0) {
            return;
        }

        if (fontWidth == -1) {
            const canvasContext = document.createElement("canvas").getContext("2d");
            canvasContext.font = window.getComputedStyle(element, null).getPropertyValue("font");
            fontWidth = canvasContext.measureText("0").width;
        }

        const digits = getNumberDigits(maxNumber);
        element.parentNode.style.paddingLeft = 2 * fontWidth + digits * fontWidth - digits + "px";
    });
}
