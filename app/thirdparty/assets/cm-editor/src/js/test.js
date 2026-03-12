// editorBridge.setText("hello");

export function callback() {
    if (typeof callbackInterface === 'undefined') {
        console.log("'callbackInterface' is undefined");
    } else {
        callbackInterface.callback('Hello from JavaScript');
    }
}
