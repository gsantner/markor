.../app/src/main/java/net/gsantner/markor/format/csv/README.md

Last updated 2023-02-21 by k3b

## Information about csv files in markor

The android editor app [markor](https://github.com/gsantner/markor)
can handle [csv](https://en.wikipedia.org/wiki/Comma-separated_values) files since version ??? **TODO**

### Features

* Automatic discovering of `csv-field-delimiter-char` from found csv-header. 
  * Currently implemented: `;,:|` and `<tab>`
* Automatic discovering of `csv-quote-char` from found csv-header.
    * Currently implemented: `"'`
* empty lines are skipped
* comments: Lines that start with **`#`** that do not contain `csv-field-delimiter-char` or `csv-quote-char` are skipped.
  * Example # this is a comment 
* column content may or may not be sorrounded with `csv-quote-char` .
* column content must be sorrounded with `csv-quote-char` if content contains `csv-field-delimiter-char` or `<cr>` or `<nl>` .
  * Example ...;"A column may contain a ; char ";... 
* column content may contain `csv-quote-char` : Example: "This text contains a "" char" 
* Markor Syntax Highlighting: Not implemented yet **TODO**. See [ticket 1987](https://github.com/gsantner/markor/issues/1987)
* Markor Preview/Export-html/pdf: Implementation completed. Waiting for merge .  See [ticket 1980](https://github.com/gsantner/markor/issues/1980)

### Requirements

* csv must have a csv-header-line within the first 8000 chars of the csv
* if csv uses `csv-quote-char` = **`'`** then at least one header column name must be sourrounden by `'` 
  * # Example: redefining  `csv-field-delimiter-char` and `csv-quote-char` through csv-header-line
  * 'column1'|column2|column3
