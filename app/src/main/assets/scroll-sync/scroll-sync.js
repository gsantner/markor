/**
 * Scroll to the target element by source line number (generally the first visible line number).
 * Params: lineNumber - the line number of source code in the editor.
 */
function edit2Preview(lineNumber) {
    let increment = 0;
    let direction = 0;
    let number = lineNumber;
    while (number > 0) {
        if (direction > 0) {
            number = lineNumber + increment;
            direction = -1;
        } else if (direction < 0) {
            number = lineNumber - increment;
            direction = 1;
            increment++;
        } else {
            direction = 1;
            increment++;
        }

        const elements = document.querySelectorAll("[data-line='" + number + "']");
        if (elements == null) {
            continue;
        }

        let completed = false;
        for (let i = 0; i < elements.length; i++) {
            const element = elements[i];
            if (element.offsetHeight > 0) {
                element.scrollIntoView();
                completed = true;
                break;
            }
        }

        if (completed) {
            break;
        }
    }
}

/**
 * Find the target line number, that is the value of data-line attribute of target element (generally the first visible element).
 * Return: -1 if the target element cannot not be found.
 */
function preview2Edit() {
    const elements = document.querySelectorAll("[data-line]");
    if (elements == null || elements.length == 0) {
        return -1;
    }

    const TOP_MARGIN = -20;
    const BOTTOM_MARGIN = window.innerHeight;
    for (let i = 0; i < elements.length; i++) {
        const element = elements[i];
        const top = element.getBoundingClientRect().top;
        const bottom = element.getBoundingClientRect().bottom;
        if (top > TOP_MARGIN && bottom > 0 && bottom < BOTTOM_MARGIN) {
            return parseInt(element.getAttribute("data-line"));
        }
    }

    return -1;
}
