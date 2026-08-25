# CodeMirror Editor Support for Markor

## 1. Development

### Install Node.js Module Dependencies

```shell
npm install
```

### Start Development Server

```shell
npm run dev
```

### Project Structure

`./src`: source code directory;

`./src/js/editor-bridge.js`: the main code file for implementing CodeMirror feature support.

### Build

```shell
npm run build
```

Build output directory: `/app/thirdparty/assets/cm-editor`.

Based on these build files, Java class `net.gsantner.markor.frontend.textview.CodeMirrorEditor` can create the CodeMirror view for Android.

## 2. About CodeMirror

### Features

[Features](https://codemirror.net/)

### Examples

[CodeMirror Examples](https://codemirror.net/examples/)

### Language Support

[Language Support](https://codemirror.net/#languages)

### Huge Document Support

[CodeMirror Huge Document Demo](https://codemirror.net/examples/million/)

### Who is using CodeMirror?

[Sponsors](https://codemirror.net/#sponsors)
