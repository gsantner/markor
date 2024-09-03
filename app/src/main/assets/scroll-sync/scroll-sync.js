/**
 * Scroll to the target element by source line number (generally the first visible line number).
 * Params: lineNumber - the line number of source code in editor.
 */
function editor2Preview(lineNumber) {
    let range = 0;
    let direction = 0;
    let number = lineNumber;
    while (number > 0) {
        if (direction > 0) {
            number = lineNumber + range;
            direction = -1;
        } else if (direction < 0) {
            number = lineNumber - range;
            direction = 1;
            range++;
        } else {
            direction = 1;
            range++;
        }

        let elements = document.querySelectorAll("[data-line='" + number + "']");
        if (elements == null) {
            continue;
        }

        let completed = false;
        for (let i = 0; i < elements.length; i++) {
            let element = elements[i];
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
 * Params: lineNumber - as a base position.
 * Return: 0 if the line number found is around lineNumber (no need to adjust for this minor scrolling), or the target element cannot not be found.
 */
function preview2Editor(lineNumber) {
    let elements = document.querySelectorAll("[data-line]");
    if (elements == null || elements.length == 0) {
        return 0;
    }

    for (let i = 0; i < elements.length; i++) {
        let element = elements[i];
        let top = element.getBoundingClientRect().top;
        let bottom = element.getBoundingClientRect().bottom;
        if (top > 0 && bottom < window.innerHeight) {
            let number = parseInt(element.getAttribute("data-line"));
            if (number > lineNumber - 3 && number < lineNumber + 3) {
                return 0;
            } else {
                return number;
            }
        }
    }
    return 0;
}
