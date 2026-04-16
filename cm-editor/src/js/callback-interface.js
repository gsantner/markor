function getCallbackInterface() {
    if (typeof callbackInterface === 'undefined') {
        console.log("'callbackInterface' is undefined");
        return null;
    } else {
        return callbackInterface;
    }
}

export function focus() {
    const callback = getCallbackInterface();
    if (callback) {
        callbackInterface.focus();
    }
}

export function readText(path) {
    const callback = getCallbackInterface();
    if (callback) {
        return callbackInterface.readText(path);
    } else {
        return "Error";
    }
}
