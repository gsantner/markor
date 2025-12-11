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

function getFontWidth(codeElement) {
    const canvasContext = document.createElement("canvas").getContext("2d");
    canvasContext.font = window.getComputedStyle(codeElement, null).getPropertyValue("font");
    return canvasContext.measureText("0").width;
}

function adjustLayout(codeElement, codeFontWidth) {
    const maxNumber = codeElement.textContent.split("\n").length - 1;
    if (maxNumber == 0) {
        return;
    }

    const digits = getNumberDigits(maxNumber);
    const padding = 12 + (digits * codeFontWidth);
    codeElement.parentNode.style.paddingLeft = padding + "px";
}
