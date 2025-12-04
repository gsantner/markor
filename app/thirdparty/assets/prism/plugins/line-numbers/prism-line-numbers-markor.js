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

let codeFontWidth = -1;
function adjustLayout(codeElement) {
    const maxNumber = codeElement.textContent.split("\n").length - 1;
    if (maxNumber == 0) {
        return;
    }

    if (codeFontWidth == -1) {
        const canvasContext = document.createElement("canvas").getContext("2d");
        canvasContext.font = window.getComputedStyle(codeElement, null).getPropertyValue("font");
        codeFontWidth = canvasContext.measureText("0").width;
    }

    const digits = getNumberDigits(maxNumber);
    codeElement.parentNode.style.paddingLeft = 2 * codeFontWidth + digits * codeFontWidth - digits + "px";
}
