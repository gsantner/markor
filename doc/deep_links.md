# Deep links

Use `markor://open?path=...` to open a file or folder in Markor. Absolute paths
are supported; relative paths are resolved against the notebook directory.
URL-encode the path when it contains spaces or other reserved characters.

Add `show=true` to show the target in Markor's file browser. Files are revealed
in their containing folder; folders are opened in the browser. The target is
not opened in the editor, and editor options such as `line` and `view` are ignored.

```text
markor://open?path=notes.md&show=true
markor://open?path=projects&show=true
markor://open?path=notes.md&show=true&line=10&view=true
```

The `show` option also works on `file://` links opened from a document preview.
Omitting it, setting it to `false`, or providing an invalid value preserves the
usual link behavior.
