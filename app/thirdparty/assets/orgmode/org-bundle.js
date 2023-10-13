(function(){function r(e,n,t){function o(i,f){if(!n[i]){if(!e[i]){var c="function"==typeof require&&require;if(!f&&c)return c(i,!0);if(u)return u(i,!0);var a=new Error("Cannot find module '"+i+"'");throw a.code="MODULE_NOT_FOUND",a}var p=n[i]={exports:{}};e[i][0].call(p.exports,function(r){var n=e[i][1][r];return o(n||r)},p,p.exports,r,e,n,t)}return n[i].exports}for(var u="function"==typeof require&&require,i=0;i<t.length;i++)o(t[i]);return o}return r})()({1:[function(require,module,exports){
if (typeof exports !== "undefined") {
  function exportModule(module) {
    for (var exportedName in module) {
      if (module.hasOwnProperty(exportedName)) {
        exports[exportedName] = module[exportedName];
      }
    }
  }

  exportModule(require("./org/parser.js"));
  exportModule(require("./org/lexer.js"));
  exportModule(require("./org/node.js"));
  exportModule(require("./org/parser.js"));
  exportModule(require("./org/stream.js"));
  exportModule(require("./org/converter/html.js"));
}

},{"./org/converter/html.js":3,"./org/lexer.js":4,"./org/node.js":5,"./org/parser.js":6,"./org/stream.js":7}],2:[function(require,module,exports){
var Node = require("../node.js").Node;

function Converter() {
}

Converter.prototype = {
  exportOptions: {
    headerOffset: 1,
    exportFromLineNumber: false,
    suppressSubScriptHandling: false,
    suppressAutoLink: false,
    // HTML
    translateSymbolArrow: false,
    suppressCheckboxHandling: false,
    // { "directive:": function (node, childText, auxData) {} }
    customDirectiveHandler: null,
    // e.g., "org-js-"
    htmlClassPrefix: null,
    htmlIdPrefix: null
  },

  untitled: "Untitled",
  result: null,

  // TODO: Manage TODO lists

  initialize: function (orgDocument, exportOptions) {
    this.orgDocument = orgDocument;
    this.documentOptions = orgDocument.options || {};
    this.exportOptions = exportOptions || {};

    this.headers = [];
    this.headerOffset =
      typeof this.exportOptions.headerOffset === "number" ? this.exportOptions.headerOffset : 1;
    this.sectionNumbers = [0];
  },

  createTocItem: function (headerNode, parentTocs) {
    var childTocs = [];
    childTocs.parent = parentTocs;
    var tocItem = { headerNode: headerNode, childTocs: childTocs };
    return tocItem;
  },

  computeToc: function (exportTocLevel) {
    if (typeof exportTocLevel !== "number")
      exportTocLevel = Infinity;

    var toc = [];
    toc.parent = null;

    var previousLevel = 1;
    var currentTocs = toc;  // first

    for (var i = 0; i < this.headers.length; ++i) {
      var headerNode = this.headers[i];

      if (headerNode.level > exportTocLevel)
        continue;

      var levelDiff = headerNode.level - previousLevel;
      if (levelDiff > 0) {
        for (var j = 0; j < levelDiff; ++j) {
          if (currentTocs.length === 0) {
            // Create a dummy tocItem
            var dummyHeader = Node.createHeader([], {
              level: previousLevel + j
            });
            dummyHeader.sectionNumberText = "";
            currentTocs.push(this.createTocItem(dummyHeader, currentTocs));
          }
          currentTocs = currentTocs[currentTocs.length - 1].childTocs;
        }
      } else if (levelDiff < 0) {
        levelDiff = -levelDiff;
        for (var k = 0; k < levelDiff; ++k) {
          currentTocs = currentTocs.parent;
        }
      }

      currentTocs.push(this.createTocItem(headerNode, currentTocs));

      previousLevel = headerNode.level;
    }

    return toc;
  },

  convertNode: function (node, recordHeader, insideCodeElement) {
    if (!insideCodeElement) {
      if (node.type === Node.types.directive) {
        if (node.directiveName === "example" ||
            node.directiveName === "src") {
          insideCodeElement = true;
        }
      } else if (node.type === Node.types.preformatted) {
        insideCodeElement = true;
      }
    }

    if (typeof node === "string") {
      node = Node.createText(null, { value: node });
    }

    var childText = node.children ? this.convertNodesInternal(node.children, recordHeader, insideCodeElement) : "";
    var text;

    var auxData = this.computeAuxDataForNode(node);

    switch (node.type) {
    case Node.types.header:
      // Parse task status
      var taskStatus = null;
      if (childText.indexOf("TODO ") === 0)
        taskStatus = "todo";
      else if (childText.indexOf("DONE ") === 0)
        taskStatus = "done";

      // Compute section number
      var sectionNumberText = null;
      if (recordHeader) {
        var thisHeaderLevel = node.level;
        var previousHeaderLevel = this.sectionNumbers.length;
        if (thisHeaderLevel > previousHeaderLevel) {
          // Fill missing section number
          var levelDiff = thisHeaderLevel - previousHeaderLevel;
          for (var j = 0; j < levelDiff; ++j) {
            this.sectionNumbers[thisHeaderLevel - 1 - j] = 0; // Extend
          }
        } else if (thisHeaderLevel < previousHeaderLevel) {
          this.sectionNumbers.length = thisHeaderLevel; // Collapse
        }
        this.sectionNumbers[thisHeaderLevel - 1]++;
        sectionNumberText = this.sectionNumbers.join(".");
        node.sectionNumberText = sectionNumberText; // Can be used in ToC
      }

      text = this.convertHeader(node, childText, auxData,
                                taskStatus, sectionNumberText);

      if (recordHeader)
        this.headers.push(node);
      break;
    case Node.types.orderedList:
      text = this.convertOrderedList(node, childText, auxData);
      break;
    case Node.types.unorderedList:
      text = this.convertUnorderedList(node, childText, auxData);
      break;
    case Node.types.definitionList:
      text = this.convertDefinitionList(node, childText, auxData);
      break;
    case Node.types.listElement:
      if (node.isDefinitionList) {
        var termText = this.convertNodesInternal(node.term, recordHeader, insideCodeElement);
        text = this.convertDefinitionItem(node, childText, auxData,
                                          termText, childText);
      } else {
        text = this.convertListItem(node, childText, auxData);
      }
      break;
    case Node.types.paragraph:
      text = this.convertParagraph(node, childText, auxData);
      break;
    case Node.types.preformatted:
      text = this.convertPreformatted(node, childText, auxData);
      break;
    case Node.types.table:
      text = this.convertTable(node, childText, auxData);
      break;
    case Node.types.tableRow:
      text = this.convertTableRow(node, childText, auxData);
      break;
    case Node.types.tableCell:
      if (node.isHeader)
        text = this.convertTableHeader(node, childText, auxData);
      else
        text = this.convertTableCell(node, childText, auxData);
      break;
    case Node.types.horizontalRule:
      text = this.convertHorizontalRule(node, childText, auxData);
      break;
      // ============================================================ //
      // Inline
      // ============================================================ //
    case Node.types.inlineContainer:
      text = this.convertInlineContainer(node, childText, auxData);
      break;
    case Node.types.bold:
      text = this.convertBold(node, childText, auxData);
      break;
    case Node.types.italic:
      text = this.convertItalic(node, childText, auxData);
      break;
    case Node.types.underline:
      text = this.convertUnderline(node, childText, auxData);
      break;
    case Node.types.code:
      text = this.convertCode(node, childText, auxData);
      break;
    case Node.types.dashed:
      text = this.convertDashed(node, childText, auxData);
      break;
    case Node.types.link:
      text = this.convertLink(node, childText, auxData);
      break;
    case Node.types.directive:
      switch (node.directiveName) {
      case "quote":
        text = this.convertQuote(node, childText, auxData);
        break;
      case "example":
        text = this.convertExample(node, childText, auxData);
        break;
      case "src":
        text = this.convertSrc(node, childText, auxData);
        break;
      case "html":
      case "html:":
        text = this.convertHTML(node, childText, auxData);
        break;
      default:
        if (this.exportOptions.customDirectiveHandler &&
            this.exportOptions.customDirectiveHandler[node.directiveName]) {
          text = this.exportOptions.customDirectiveHandler[node.directiveName](
            node, childText, auxData
          );
        } else {
          text = childText;
        }
      }
      break;
    case Node.types.text:
      text = this.convertText(node.value, insideCodeElement);
      break;
    default:
      throw Error("Unknown node type: " + node.type);
    }

    if (typeof this.postProcess === "function") {
      text = this.postProcess(node, text, insideCodeElement);
    }

    return text;
  },

  convertText: function (text, insideCodeElement) {
    var escapedText = this.escapeSpecialChars(text, insideCodeElement);

    if (!this.exportOptions.suppressSubScriptHandling && !insideCodeElement) {
      escapedText = this.makeSubscripts(escapedText, insideCodeElement);
    }
    if (!this.exportOptions.suppressAutoLink) {
      escapedText = this.linkURL(escapedText);
    }

    return escapedText;
  },

  // By default, ignore html
  convertHTML: function (node, childText, auxData) {
    return childText;
  },

  convertNodesInternal: function (nodes, recordHeader, insideCodeElement) {
    var nodesTexts = [];
    for (var i = 0; i < nodes.length; ++i) {
      var node = nodes[i];
      var nodeText = this.convertNode(node, recordHeader, insideCodeElement);
      nodesTexts.push(nodeText);
    }
    return this.combineNodesTexts(nodesTexts);
  },

  convertHeaderBlock: function (headerBlock, recordHeader) {
    throw Error("convertHeaderBlock is not implemented");
  },

  convertHeaderTree: function (headerTree, recordHeader) {
    return this.convertHeaderBlock(headerTree, recordHeader);
  },

  convertNodesToHeaderTree: function (nodes, nextBlockBegin, blockHeader) {
    var childBlocks = [];
    var childNodes = [];

    if (typeof nextBlockBegin === "undefined") {
      nextBlockBegin = 0;
    }
    if (typeof blockHeader === "undefined") {
      blockHeader = null;
    }

    for (var i = nextBlockBegin; i < nodes.length;) {
      var node = nodes[i];

      var isHeader = node.type === Node.types.header;

      if (!isHeader) {
        childNodes.push(node);
        i = i + 1;
        continue;
      }

      // Header
      if (blockHeader && node.level <= blockHeader.level) {
        // Finish Block
        break;
      } else {
        // blockHeader.level < node.level
        // Begin child block
        var childBlock = this.convertNodesToHeaderTree(nodes, i + 1, node);
        childBlocks.push(childBlock);
        i = childBlock.nextIndex;
      }
    }

    // Finish block
    return {
      header: blockHeader,
      childNodes: childNodes,
      nextIndex: i,
      childBlocks: childBlocks
    };
  },

  convertNodes: function (nodes, recordHeader, insideCodeElement) {
    return this.convertNodesInternal(nodes, recordHeader, insideCodeElement);
  },

  combineNodesTexts: function (nodesTexts) {
    return nodesTexts.join("");
  },

  getNodeTextContent: function (node) {
    if (node.type === Node.types.text)
      return this.escapeSpecialChars(node.value);
    else
      return node.children ? node.children.map(this.getNodeTextContent, this).join("") : "";
  },

  // @Override
  escapeSpecialChars: function (text) {
    throw Error("Implement escapeSpecialChars");
  },

  // http://daringfireball.net/2010/07/improved_regex_for_matching_urls
  urlPattern: /\b(?:https?:\/\/|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}\/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?«»“”‘’])/ig,

  // @Override
  linkURL: function (text) {
    var self = this;
    return text.replace(this.urlPattern, function (matched) {
      if (matched.indexOf("://") < 0)
        matched = "http://" + matched;
      return self.makeLink(matched);
    });
  },

  makeLink: function (url) {
    throw Error("Implement makeLink");
  },

  makeSubscripts: function (text) {
    if (this.documentOptions["^"] === "{}")
      return text.replace(/\b([^_ \t]*)_{([^}]*)}/g,
                          this.makeSubscript);
    else if (this.documentOptions["^"])
      return text.replace(/\b([^_ \t]*)_([^_]*)\b/g,
                          this.makeSubscript);
    else
      return text;
  },

  makeSubscript: function (match, body, subscript) {
    throw Error("Implement makeSubscript");
  },

  stripParametersFromURL: function (url) {
    return url.replace(/\?.*$/, "");
  },

  imageExtensionPattern: new RegExp("(" + [
    "bmp", "png", "jpeg", "jpg", "gif", "tiff",
    "tif", "xbm", "xpm", "pbm", "pgm", "ppm", "svg"
  ].join("|") + ")$", "i")
};

if (typeof exports !== "undefined")
  exports.Converter = Converter;

},{"../node.js":5}],3:[function(require,module,exports){
var Converter = require("./converter.js").Converter;
var Node = require("../node.js").Node;

function ConverterHTML(orgDocument, exportOptions) {
  this.initialize(orgDocument, exportOptions);
  this.result = this.convert();
}

ConverterHTML.prototype = {
  __proto__: Converter.prototype,

  convert: function () {
    var title = this.orgDocument.title ? this.convertNode(this.orgDocument.title) : this.untitled;
    var titleHTML = this.tag("h" + Math.max(Number(this.headerOffset), 1), title);
    var contentHTML = this.convertNodes(this.orgDocument.nodes, true /* record headers */);
    var toc = this.computeToc(this.documentOptions["toc"]);
    var tocHTML = this.tocToHTML(toc);

    return {
      title: title,
      titleHTML: titleHTML,
      contentHTML: contentHTML,
      tocHTML: tocHTML,
      toc: toc,
      toString: function () {
        return titleHTML + tocHTML + "\n" + contentHTML;
      }
    };
  },

  tocToHTML: function (toc) {
    function tocToHTMLFunction(tocList) {
      var html = "";
      for (var i = 0; i < tocList.length; ++i) {
        var tocItem = tocList[i];
        var sectionNumberText = tocItem.headerNode.sectionNumberText;
        var sectionNumber = this.documentOptions.num ?
              this.inlineTag("span", sectionNumberText, {
                "class": "section-number"
              }) : "";
        var header = this.getNodeTextContent(tocItem.headerNode);
        var headerLink = this.inlineTag("a", sectionNumber + header, {
          href: "#header-" + sectionNumberText.replace(/\./g, "-")
        });
        var subList = tocItem.childTocs.length ? tocToHTMLFunction.call(this, tocItem.childTocs) : "";
        html += this.tag("li", headerLink + subList);
      }
      return this.tag("ul", html);
    }

    return tocToHTMLFunction.call(this, toc);
  },

  computeAuxDataForNode: function (node) {
    while (node.parent &&
           node.parent.type === Node.types.inlineContainer) {
      node = node.parent;
    }
    var attributesNode = node.previousSibling;
    var attributesText = "";
    while (attributesNode &&
           attributesNode.type === Node.types.directive &&
           attributesNode.directiveName === "attr_html:") {
      attributesText += attributesNode.directiveRawValue + " ";
      attributesNode = attributesNode.previousSibling;
    }
    return attributesText;
  },

  // Method to construct org-js generated class
  orgClassName: function (className) {
    return this.exportOptions.htmlClassPrefix ?
      this.exportOptions.htmlClassPrefix + className
      : className;
  },

  // Method to construct org-js generated id
  orgId: function (id) {
    return this.exportOptions.htmlIdPrefix ?
      this.exportOptions.htmlIdPrefix + id
      : id;
  },

  // ----------------------------------------------------
  // Node conversion
  // ----------------------------------------------------

  convertHeader: function (node, childText, auxData,
                           taskStatus, sectionNumberText) {
    var headerAttributes = {};

    if (taskStatus) {
      childText = this.inlineTag("span", childText.substring(0, 4), {
        "class": "task-status " + taskStatus
      }) + childText.substring(5);
    }

    if (sectionNumberText) {
      childText = this.inlineTag("span", sectionNumberText, {
        "class": "section-number"
      }) + childText;
      headerAttributes["id"] = "header-" + sectionNumberText.replace(/\./g, "-");
    }

    if (taskStatus)
      headerAttributes["class"] = "task-status " + taskStatus;

    return this.tag("h" + (this.headerOffset + node.level),
                    childText, headerAttributes, auxData);
  },

  convertOrderedList: function (node, childText, auxData) {
    return this.tag("ol", childText, null, auxData);
  },

  convertUnorderedList: function (node, childText, auxData) {
    return this.tag("ul", childText, null, auxData);
  },

  convertDefinitionList: function (node, childText, auxData) {
    return this.tag("dl", childText, null, auxData);
  },

  convertDefinitionItem: function (node, childText, auxData,
                                   term, definition) {
    return this.tag("dt", term) + this.tag("dd", definition);
  },

  convertListItem: function (node, childText, auxData) {
    if (this.exportOptions.suppressCheckboxHandling) {
      return this.tag("li", childText, null, auxData);
    } else {
      var listItemAttributes = {};
      var listItemText = childText;
      // Embed checkbox
      if (/^\s*\[(X| |-)\]([\s\S]*)/.exec(listItemText)) {
        listItemText = RegExp.$2 ;
        var checkboxIndicator = RegExp.$1;

        var checkboxAttributes = { type: "checkbox" };
        switch (checkboxIndicator) {
        case "X":
          checkboxAttributes["checked"] = "true";
          listItemAttributes["data-checkbox-status"] = "done";
          break;
        case "-":
          listItemAttributes["data-checkbox-status"] = "intermediate";
          break;
        default:
          listItemAttributes["data-checkbox-status"] = "undone";
          break;
        }

        listItemText = this.inlineTag("input", null, checkboxAttributes) + listItemText;
      }

      return this.tag("li", listItemText, listItemAttributes, auxData);
    }
  },

  convertParagraph: function (node, childText, auxData) {
    return this.tag("p", childText, null, auxData);
  },

  convertPreformatted: function (node, childText, auxData) {
    return this.tag("pre", childText, null, auxData);
  },

  convertTable: function (node, childText, auxData) {
    return this.tag("table", this.tag("tbody", childText), null, auxData);
  },

  convertTableRow: function (node, childText, auxData) {
    return this.tag("tr", childText);
  },

  convertTableHeader: function (node, childText, auxData) {
    return this.tag("th", childText);
  },

  convertTableCell: function (node, childText, auxData) {
    return this.tag("td", childText);
  },

  convertHorizontalRule: function (node, childText, auxData) {
    return this.tag("hr", null, null, auxData);
  },

  convertInlineContainer: function (node, childText, auxData) {
    return childText;
  },

  convertBold: function (node, childText, auxData) {
    return this.inlineTag("b", childText);
  },

  convertItalic: function (node, childText, auxData) {
    return this.inlineTag("i", childText);
  },

  convertUnderline: function (node, childText, auxData) {
    return this.inlineTag("span", childText, {
      style: "text-decoration:underline;"
    });
  },

  convertCode: function (node, childText, auxData) {
    return this.inlineTag("code", childText);
  },

  convertDashed: function (node, childText, auxData) {
    return this.inlineTag("del", childText);
  },

  convertLink: function (node, childText, auxData) {
    var srcParameterStripped = this.stripParametersFromURL(node.src);
    if (this.imageExtensionPattern.exec(srcParameterStripped)) {
      var imgText = this.getNodeTextContent(node);
      return this.inlineTag("img", null, {
        src: node.src,
        alt: imgText,
        title: imgText
      }, auxData);
    } else {
      return this.inlineTag("a", childText, { href: node.src });
    }
  },

  convertQuote: function (node, childText, auxData) {
    return this.tag("blockquote", childText, null, auxData);
  },

  convertExample: function (node, childText, auxData) {
    return this.tag("pre", childText, null, auxData);
  },

  convertSrc: function (node, childText, auxData) {
    var codeLanguage = node.directiveArguments.length
          ? node.directiveArguments[0]
          : "unknown";
    childText = this.tag("code", childText, {
      "class": "language-" + codeLanguage
    }, auxData);
    return this.tag("pre", childText, {
      "class": "prettyprint"
    });
  },

  // @override
  convertHTML: function (node, childText, auxData) {
    if (node.directiveName === "html:") {
      return node.directiveRawValue;
    } else if (node.directiveName === "html") {
      return node.children.map(function (textNode) {
        return textNode.value;
      }).join("\n");
    } else {
      return childText;
    }
  },

  // @implement
  convertHeaderBlock: function (headerBlock, level, index) {
    level = level || 0;
    index = index || 0;

    var contents = [];

    var headerNode = headerBlock.header;
    if (headerNode) {
      contents.push(this.convertNode(headerNode));
    }

    var blockContent = this.convertNodes(headerBlock.childNodes);
    contents.push(blockContent);

    var childBlockContent = headerBlock.childBlocks
          .map(function (block, idx) {
            return this.convertHeaderBlock(block, level + 1, idx);
          }, this)
          .join("\n");
    contents.push(childBlockContent);

    var contentsText = contents.join("\n");

    if (headerNode) {
      return this.tag("section", "\n" + contents.join("\n"), {
        "class": "block block-level-" + level
      });
    } else {
      return contentsText;
    }
  },

  // ----------------------------------------------------
  // Supplemental methods
  // ----------------------------------------------------

  replaceMap: {
    // [replacing pattern, predicate]
    "&": ["&#38;", null],
    "<": ["&#60;", null],
    ">": ["&#62;", null],
    '"': ["&#34;", null],
    "'": ["&#39;", null],
    "->": ["&#10132;", function (text, insideCodeElement) {
      return this.exportOptions.translateSymbolArrow && !insideCodeElement;
    }]
  },

  replaceRegexp: null,

  // @implement @override
  escapeSpecialChars: function (text, insideCodeElement) {
    if (!this.replaceRegexp) {
      this.replaceRegexp = new RegExp(Object.keys(this.replaceMap).join("|"), "g");
    }

    var replaceMap = this.replaceMap;
    var self = this;
    return text.replace(this.replaceRegexp, function (matched) {
      if (!replaceMap[matched]) {
        throw Error("escapeSpecialChars: Invalid match");
      }

      var predicate = replaceMap[matched][1];
      if (typeof predicate === "function" &&
          !predicate.call(self, text, insideCodeElement)) {
        // Not fullfill the predicate
        return matched;
      }

      return replaceMap[matched][0];
    });
  },

  // @implement
  postProcess: function (node, currentText, insideCodeElement) {
    if (this.exportOptions.exportFromLineNumber &&
        typeof node.fromLineNumber === "number") {
      // Wrap with line number information
      currentText = this.inlineTag("div", currentText, {
        "data-line-number": node.fromLineNumber
      });
    }
    return currentText;
  },

  // @implement
  makeLink: function (url) {
    return "<a href=\"" + url + "\">" + decodeURIComponent(url) + "</a>";
  },

  // @implement
  makeSubscript: function (match, body, subscript) {
    return "<span class=\"org-subscript-parent\">" +
      body +
      "</span><span class=\"org-subscript-child\">" +
      subscript +
      "</span>";
  },

  // ----------------------------------------------------
  // Specific methods
  // ----------------------------------------------------

  attributesObjectToString: function (attributesObject) {
    var attributesString = "";
    for (var attributeName in attributesObject) {
      if (attributesObject.hasOwnProperty(attributeName)) {
        var attributeValue = attributesObject[attributeName];
        // To avoid id/class name conflicts with other frameworks,
        // users can add arbitrary prefix to org-js generated
        // ids/classes via exportOptions.
        if (attributeName === "class") {
          attributeValue = this.orgClassName(attributeValue);
        } else if (attributeName === "id") {
          attributeValue = this.orgId(attributeValue);
        }
        attributesString += " " + attributeName + "=\"" + attributeValue + "\"";
      }
    }
    return attributesString;
  },

  inlineTag: function (name, innerText, attributesObject, auxAttributesText) {
    attributesObject = attributesObject || {};

    var htmlString = "<" + name;
    // TODO: check duplicated attributes
    if (auxAttributesText)
      htmlString += " " + auxAttributesText;
    htmlString += this.attributesObjectToString(attributesObject);

    if (innerText === null)
      return htmlString + "/>";

    htmlString += ">" + innerText + "</" + name + ">";

    return htmlString;
  },

  tag: function (name, innerText, attributesObject, auxAttributesText) {
    return this.inlineTag(name, innerText, attributesObject, auxAttributesText) + "\n";
  }
};

if (typeof exports !== "undefined")
  exports.ConverterHTML = ConverterHTML;

},{"../node.js":5,"./converter.js":2}],4:[function(require,module,exports){
// ------------------------------------------------------------
// Syntax
// ------------------------------------------------------------

var Syntax = {
  rules: {},

  define: function (name, syntax) {
    this.rules[name] = syntax;
    var methodName = "is" + name.substring(0, 1).toUpperCase() + name.substring(1);
    this[methodName] = function (line) {
      return this.rules[name].exec(line);
    };
  }
};

Syntax.define("header", /^(\*+)\s+(.*)$/); // m[1] => level, m[2] => content
Syntax.define("preformatted", /^(\s*):(?: (.*)$|$)/); // m[1] => indentation, m[2] => content
Syntax.define("unorderedListElement", /^(\s*)(?:-|\+|\s+\*)\s+(.*)$/); // m[1] => indentation, m[2] => content
Syntax.define("orderedListElement", /^(\s*)(\d+)(?:\.|\))\s+(.*)$/); // m[1] => indentation, m[2] => number, m[3] => content
Syntax.define("tableSeparator", /^(\s*)\|((?:\+|-)*?)\|?$/); // m[1] => indentation, m[2] => content
Syntax.define("tableRow", /^(\s*)\|(.*?)\|?$/); // m[1] => indentation, m[2] => content
Syntax.define("blank", /^$/);
Syntax.define("horizontalRule", /^(\s*)-{5,}$/); //
Syntax.define("directive", /^(\s*)#\+(?:(begin|end)_)?(.*)$/i); // m[1] => indentation, m[2] => type, m[3] => content
Syntax.define("comment", /^(\s*)#(.*)$/);
Syntax.define("line", /^(\s*)(.*)$/);

// ------------------------------------------------------------
// Token
// ------------------------------------------------------------

function Token() {
}

Token.prototype = {
  isListElement: function () {
    return this.type === Lexer.tokens.orderedListElement ||
      this.type === Lexer.tokens.unorderedListElement;
  },

  isTableElement: function () {
    return this.type === Lexer.tokens.tableSeparator ||
      this.type === Lexer.tokens.tableRow;
  }
};

// ------------------------------------------------------------
// Lexer
// ------------------------------------------------------------

function Lexer(stream) {
  this.stream = stream;
  this.tokenStack = [];
}

Lexer.prototype = {
  tokenize: function (line) {
    var token = new Token();
    token.fromLineNumber = this.stream.lineNumber;

    if (Syntax.isHeader(line)) {
      token.type        = Lexer.tokens.header;
      token.indentation = 0;
      token.content     = RegExp.$2;
      // specific
      token.level       = RegExp.$1.length;
    } else if (Syntax.isPreformatted(line)) {
      token.type        = Lexer.tokens.preformatted;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$2;
    } else if (Syntax.isUnorderedListElement(line)) {
      token.type        = Lexer.tokens.unorderedListElement;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$2;
    } else if (Syntax.isOrderedListElement(line)) {
      token.type        = Lexer.tokens.orderedListElement;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$3;
      // specific
      token.number      = RegExp.$2;
    } else if (Syntax.isTableSeparator(line)) {
      token.type        = Lexer.tokens.tableSeparator;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$2;
    } else if (Syntax.isTableRow(line)) {
      token.type        = Lexer.tokens.tableRow;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$2;
    } else if (Syntax.isBlank(line)) {
      token.type        = Lexer.tokens.blank;
      token.indentation = 0;
      token.content     = null;
    } else if (Syntax.isHorizontalRule(line)) {
      token.type        = Lexer.tokens.horizontalRule;
      token.indentation = RegExp.$1.length;
      token.content     = null;
    } else if (Syntax.isDirective(line)) {
      token.type        = Lexer.tokens.directive;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$3;
      // decide directive type (begin, end or oneshot)
      var directiveTypeString = RegExp.$2;
      if (/^begin/i.test(directiveTypeString))
        token.beginDirective = true;
      else if (/^end/i.test(directiveTypeString))
        token.endDirective = true;
      else
        token.oneshotDirective = true;
    } else if (Syntax.isComment(line)) {
      token.type        = Lexer.tokens.comment;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$2;
    } else if (Syntax.isLine(line)) {
      token.type        = Lexer.tokens.line;
      token.indentation = RegExp.$1.length;
      token.content     = RegExp.$2;
    } else {
      throw new Error("SyntaxError: Unknown line: " + line);
    }

    return token;
  },

  pushToken: function (token) {
    this.tokenStack.push(token);
  },

  pushDummyTokenByType: function (type) {
    var token = new Token();
    token.type = type;
    this.tokenStack.push(token);
  },

  peekStackedToken: function () {
    return this.tokenStack.length > 0 ?
      this.tokenStack[this.tokenStack.length - 1] : null;
  },

  getStackedToken: function () {
    return this.tokenStack.length > 0 ?
      this.tokenStack.pop() : null;
  },

  peekNextToken: function () {
    return this.peekStackedToken() ||
      this.tokenize(this.stream.peekNextLine());
  },

  getNextToken: function () {
    return this.getStackedToken() ||
      this.tokenize(this.stream.getNextLine());
  },

  hasNext: function () {
    return this.stream.hasNext();
  },

  getLineNumber: function () {
    return this.stream.lineNumber;
  }
};

Lexer.tokens = {};
[
  "header",
  "orderedListElement",
  "unorderedListElement",
  "tableRow",
  "tableSeparator",
  "preformatted",
  "line",
  "horizontalRule",
  "blank",
  "directive",
  "comment"
].forEach(function (tokenName, i) {
  Lexer.tokens[tokenName] = i;
});

// ------------------------------------------------------------
// Exports
// ------------------------------------------------------------

if (typeof exports !== "undefined")
  exports.Lexer = Lexer;

},{}],5:[function(require,module,exports){
function PrototypeNode(type, children) {
  this.type = type;
  this.children = [];

  if (children) {
    for (var i = 0, len = children.length; i < len; ++i) {
      this.appendChild(children[i]);
    }
  }
}
PrototypeNode.prototype = {
  previousSibling: null,
  parent: null,
  get firstChild() {
    return this.children.length < 1 ?
      null : this.children[0];
  },
  get lastChild() {
    return this.children.length < 1 ?
      null : this.children[this.children.length - 1];
  },
  appendChild: function (newChild) {
    var previousSibling = this.children.length < 1 ?
          null : this.lastChild;
    this.children.push(newChild);
    newChild.previousSibling = previousSibling;
    newChild.parent = this;
  },
  toString: function () {
    var string = "<" + this.type + ">";

    if (typeof this.value !== "undefined") {
      string += " " + this.value;
    } else if (this.children) {
      string += "\n" + this.children.map(function (child, idx) {
        return "#" + idx + " " + child.toString();
      }).join("\n").split("\n").map(function (line) {
        return "  " + line;
      }).join("\n");
    }

    return string;
  }
};

var Node = {
  types: {},

  define: function (name, postProcess) {
    this.types[name] = name;

    var methodName = "create" + name.substring(0, 1).toUpperCase() + name.substring(1);
    var postProcessGiven = typeof postProcess === "function";

    this[methodName] = function (children, options) {
      var node = new PrototypeNode(name, children);

      if (postProcessGiven)
        postProcess(node, options || {});

      return node;
    };
  }
};

Node.define("text", function (node, options) {
  node.value = options.value;
});
Node.define("header", function (node, options) {
  node.level = options.level;
});
Node.define("orderedList");
Node.define("unorderedList");
Node.define("definitionList");
Node.define("listElement");
Node.define("paragraph");
Node.define("preformatted");
Node.define("table");
Node.define("tableRow");
Node.define("tableCell");
Node.define("horizontalRule");
Node.define("directive");

// Inline
Node.define("inlineContainer");

Node.define("bold");
Node.define("italic");
Node.define("underline");
Node.define("code");
Node.define("verbatim");
Node.define("dashed");
Node.define("link", function (node, options) {
  node.src = options.src;
});

if (typeof exports !== "undefined")
  exports.Node = Node;

},{}],6:[function(require,module,exports){
var Stream = require("./stream.js").Stream;
var Lexer  = require("./lexer.js").Lexer;
var Node   = require("./node.js").Node;

function Parser() {
  this.inlineParser = new InlineParser();
}

Parser.parseStream = function (stream, options) {
  var parser = new Parser();
  parser.initStatus(stream, options);
  parser.parseNodes();
  return parser.nodes;
};

Parser.prototype = {
  initStatus: function (stream, options) {
    if (typeof stream === "string")
      stream = new Stream(stream);
    this.lexer = new Lexer(stream);
    this.nodes = [];
    this.options = {
      toc: true,
      num: true,
      "^": "{}",
      multilineCell: false
    };
    // Override option values
    if (options && typeof options === "object") {
      for (var key in options) {
        this.options[key] = options[key];
      }
    }
    this.document = {
      options: this.options,
      directiveValues: {},
      convert: function (ConverterClass, exportOptions) {
        var converter = new ConverterClass(this, exportOptions);
        return converter.result;
      }
    };
  },

  parse: function (stream, options) {
    this.initStatus(stream, options);
    this.parseDocument();
    this.document.nodes = this.nodes;
    return this.document;
  },

  createErrorReport: function (message) {
    return new Error(message + " at line " + this.lexer.getLineNumber());
  },

  skipBlank: function () {
    var blankToken = null;
    while (this.lexer.peekNextToken().type === Lexer.tokens.blank)
      blankToken = this.lexer.getNextToken();
    return blankToken;
  },

  setNodeOriginFromToken: function (node, token) {
    node.fromLineNumber = token.fromLineNumber;
    return node;
  },

  appendNode: function (newNode) {
    var previousSibling = this.nodes.length > 0 ? this.nodes[this.nodes.length - 1] : null;
    this.nodes.push(newNode);
    newNode.previousSibling = previousSibling;
  },

  // ------------------------------------------------------------
  // <Document> ::= <Element>*
  // ------------------------------------------------------------

  parseDocument: function () {
    this.parseTitle();
    this.parseNodes();
  },

  parseNodes: function () {
    while (this.lexer.hasNext()) {
      var element = this.parseElement();
      if (element) this.appendNode(element);
    }
  },

  parseTitle: function () {
    this.skipBlank();

    if (this.lexer.hasNext() &&
        this.lexer.peekNextToken().type === Lexer.tokens.line)
      this.document.title = this.createTextNode(this.lexer.getNextToken().content);
    else
      this.document.title = null;

    this.lexer.pushDummyTokenByType(Lexer.tokens.blank);
  },

  // ------------------------------------------------------------
  // <Element> ::= (<Header> | <List>
  //              | <Preformatted> | <Paragraph>
  //              | <Table>)*
  // ------------------------------------------------------------

  parseElement: function () {
    var element = null;

    switch (this.lexer.peekNextToken().type) {
    case Lexer.tokens.header:
      element = this.parseHeader();
      break;
    case Lexer.tokens.preformatted:
      element = this.parsePreformatted();
      break;
    case Lexer.tokens.orderedListElement:
    case Lexer.tokens.unorderedListElement:
      element = this.parseList();
      break;
    case Lexer.tokens.line:
      element = this.parseText();
      break;
    case Lexer.tokens.tableRow:
    case Lexer.tokens.tableSeparator:
      element = this.parseTable();
      break;
    case Lexer.tokens.blank:
      this.skipBlank();
      if (this.lexer.hasNext()) {
        if (this.lexer.peekNextToken().type === Lexer.tokens.line)
          element = this.parseParagraph();
        else
          element = this.parseElement();
      }
      break;
    case Lexer.tokens.horizontalRule:
      this.lexer.getNextToken();
      element = Node.createHorizontalRule();
      break;
    case Lexer.tokens.directive:
      element = this.parseDirective();
      break;
    case Lexer.tokens.comment:
      // Skip
      this.lexer.getNextToken();
      break;
    default:
      throw this.createErrorReport("Unhandled token: " + this.lexer.peekNextToken().type);
    }

    return element;
  },

  parseElementBesidesDirectiveEnd: function () {
    try {
      // Temporary, override the definition of `parseElement`
      this.parseElement = this.parseElementBesidesDirectiveEndBody;
      return this.parseElement();
    } finally {
      this.parseElement = this.originalParseElement;
    }
  },

  parseElementBesidesDirectiveEndBody: function () {
    if (this.lexer.peekNextToken().type === Lexer.tokens.directive &&
        this.lexer.peekNextToken().endDirective) {
      return null;
    }

    return this.originalParseElement();
  },

  // ------------------------------------------------------------
  // <Header>
  //
  // : preformatted
  // : block
  // ------------------------------------------------------------

  parseHeader: function () {
    var headerToken = this.lexer.getNextToken();
    var header = Node.createHeader([
      this.createTextNode(headerToken.content) // TODO: Parse inline markups
    ], { level: headerToken.level });
    this.setNodeOriginFromToken(header, headerToken);

    return header;
  },

  // ------------------------------------------------------------
  // <Preformatted>
  //
  // : preformatted
  // : block
  // ------------------------------------------------------------

  parsePreformatted: function () {
    var preformattedFirstToken = this.lexer.peekNextToken();
    var preformatted = Node.createPreformatted([]);
    this.setNodeOriginFromToken(preformatted, preformattedFirstToken);

    var textContents = [];

    while (this.lexer.hasNext()) {
      var token = this.lexer.peekNextToken();
      if (token.type !== Lexer.tokens.preformatted ||
          token.indentation < preformattedFirstToken.indentation)
        break;
      this.lexer.getNextToken();
      textContents.push(token.content);
    }

    preformatted.appendChild(this.createTextNode(textContents.join("\n"), true /* no emphasis */));

    return preformatted;
  },

  // ------------------------------------------------------------
  // <List>
  //
  //  - foo
  //    1. bar
  //    2. baz
  // ------------------------------------------------------------

  // XXX: not consider codes (e.g., =Foo::Bar=)
  definitionPattern: /^(.*?) :: *(.*)$/,

  parseList: function () {
    var rootToken = this.lexer.peekNextToken();
    var list;
    var isDefinitionList = false;

    if (this.definitionPattern.test(rootToken.content)) {
      list = Node.createDefinitionList([]);
      isDefinitionList = true;
    } else {
      list = rootToken.type === Lexer.tokens.unorderedListElement ?
        Node.createUnorderedList([]) : Node.createOrderedList([]);
    }
    this.setNodeOriginFromToken(list, rootToken);

    while (this.lexer.hasNext()) {
      var nextToken = this.lexer.peekNextToken();
      if (!nextToken.isListElement() || nextToken.indentation !== rootToken.indentation)
        break;
      list.appendChild(this.parseListElement(rootToken.indentation, isDefinitionList));
    }

    return list;
  },

  unknownDefinitionTerm: "???",

  parseListElement: function (rootIndentation, isDefinitionList) {
    var listElementToken = this.lexer.getNextToken();
    var listElement = Node.createListElement([]);
    this.setNodeOriginFromToken(listElement, listElementToken);

    listElement.isDefinitionList = isDefinitionList;

    if (isDefinitionList) {
      var match = this.definitionPattern.exec(listElementToken.content);
      listElement.term = [
        this.createTextNode(match && match[1] ? match[1] : this.unknownDefinitionTerm)
      ];
      listElement.appendChild(this.createTextNode(match ? match[2] : listElementToken.content));
    } else {
      listElement.appendChild(this.createTextNode(listElementToken.content));
    }

    while (this.lexer.hasNext()) {
      var blankToken = this.skipBlank();
      if (!this.lexer.hasNext())
        break;

      var notBlankNextToken = this.lexer.peekNextToken();
      if (blankToken && !notBlankNextToken.isListElement())
        this.lexer.pushToken(blankToken); // Recover blank token only when next line is not listElement.
      if (notBlankNextToken.indentation <= rootIndentation)
        break;                  // end of the list

      var element = this.parseElement(); // recursive
      if (element)
        listElement.appendChild(element);
    }

    return listElement;
  },

  // ------------------------------------------------------------
  // <Table> ::= <TableRow>+
  // ------------------------------------------------------------

  parseTable: function () {
    var nextToken = this.lexer.peekNextToken();
    var table = Node.createTable([]);
    this.setNodeOriginFromToken(table, nextToken);
    var sawSeparator = false;

    var allowMultilineCell = nextToken.type === Lexer.tokens.tableSeparator && this.options.multilineCell;

    while (this.lexer.hasNext() &&
           (nextToken = this.lexer.peekNextToken()).isTableElement()) {
      if (nextToken.type === Lexer.tokens.tableRow) {
        var tableRow = this.parseTableRow(allowMultilineCell);
        table.appendChild(tableRow);
      } else {
        // Lexer.tokens.tableSeparator
        sawSeparator = true;
        this.lexer.getNextToken();
      }
    }

    if (sawSeparator && table.children.length) {
      table.children[0].children.forEach(function (cell) {
        cell.isHeader = true;
      });
    }

    return table;
  },

  // ------------------------------------------------------------
  // <TableRow> ::= <TableCell>+
  // ------------------------------------------------------------

  parseTableRow: function (allowMultilineCell) {
    var tableRowTokens = [];

    while (this.lexer.peekNextToken().type === Lexer.tokens.tableRow) {
      tableRowTokens.push(this.lexer.getNextToken());
      if (!allowMultilineCell) {
        break;
      }
    }

    if (!tableRowTokens.length) {
      throw this.createErrorReport("Expected table row");
    }

    var firstTableRowToken = tableRowTokens.shift();
    var tableCellTexts = firstTableRowToken.content.split("|");

    tableRowTokens.forEach(function (rowToken) {
      rowToken.content.split("|").forEach(function (cellText, cellIdx) {
        tableCellTexts[cellIdx] = (tableCellTexts[cellIdx] || "") + "\n" + cellText;
      });
    });

    // TODO: Prepare two pathes: (1)
    var tableCells = tableCellTexts.map(
      // TODO: consider '|' escape?
      function (text) {
        return Node.createTableCell(Parser.parseStream(text));
      }, this);

    return this.setNodeOriginFromToken(Node.createTableRow(tableCells), firstTableRowToken);
  },

  // ------------------------------------------------------------
  // <Directive> ::= "#+.*"
  // ------------------------------------------------------------

  parseDirective: function () {
    var directiveToken = this.lexer.getNextToken();
    var directiveNode = this.createDirectiveNodeFromToken(directiveToken);

    if (directiveToken.endDirective)
      throw this.createErrorReport("Unmatched 'end' directive for " + directiveNode.directiveName);

    if (directiveToken.oneshotDirective) {
      this.interpretDirective(directiveNode);
      return directiveNode;
    }

    if (!directiveToken.beginDirective)
      throw this.createErrorReport("Invalid directive " + directiveNode.directiveName);

    // Parse begin ~ end
    directiveNode.children = [];
    if (this.isVerbatimDirective(directiveNode))
      return this.parseDirectiveBlockVerbatim(directiveNode);
    else
      return this.parseDirectiveBlock(directiveNode);
  },

  createDirectiveNodeFromToken: function (directiveToken) {
    var matched = /^[ ]*([^ ]*)[ ]*(.*)[ ]*$/.exec(directiveToken.content);

    var directiveNode = Node.createDirective(null);
    this.setNodeOriginFromToken(directiveNode, directiveToken);
    directiveNode.directiveName = matched[1].toLowerCase();
    directiveNode.directiveArguments = this.parseDirectiveArguments(matched[2]);
    directiveNode.directiveOptions = this.parseDirectiveOptions(matched[2]);
    directiveNode.directiveRawValue = matched[2];

    return directiveNode;
  },

  isVerbatimDirective: function (directiveNode) {
    var directiveName = directiveNode.directiveName;
    return directiveName === "src" || directiveName === "example" || directiveName === "html";
  },

  parseDirectiveBlock: function (directiveNode, verbatim) {
    this.lexer.pushDummyTokenByType(Lexer.tokens.blank);

    while (this.lexer.hasNext()) {
      var nextToken = this.lexer.peekNextToken();
      if (nextToken.type === Lexer.tokens.directive &&
          nextToken.endDirective &&
          this.createDirectiveNodeFromToken(nextToken).directiveName === directiveNode.directiveName) {
        // Close directive
        this.lexer.getNextToken();
        return directiveNode;
      }
      var element = this.parseElementBesidesDirectiveEnd();
      if (element)
        directiveNode.appendChild(element);
    }

    throw this.createErrorReport("Unclosed directive " + directiveNode.directiveName);
  },

  parseDirectiveBlockVerbatim: function (directiveNode) {
    var textContent = [];

    while (this.lexer.hasNext()) {
      var nextToken = this.lexer.peekNextToken();
      if (nextToken.type === Lexer.tokens.directive &&
          nextToken.endDirective &&
          this.createDirectiveNodeFromToken(nextToken).directiveName === directiveNode.directiveName) {
        this.lexer.getNextToken();
        directiveNode.appendChild(this.createTextNode(textContent.join("\n"), true));
        return directiveNode;
      }
      textContent.push(this.lexer.stream.getNextLine());
    }

    throw this.createErrorReport("Unclosed directive " + directiveNode.directiveName);
  },

  parseDirectiveArguments: function (parameters) {
    return parameters.split(/[ ]+/).filter(function (param) {
      return param.length && param[0] !== "-";
    });
  },

  parseDirectiveOptions: function (parameters) {
    return parameters.split(/[ ]+/).filter(function (param) {
      return param.length && param[0] === "-";
    });
  },

  interpretDirective: function (directiveNode) {
    // http://orgmode.org/manual/Export-options.html
    switch (directiveNode.directiveName) {
    case "options:":
      this.interpretOptionDirective(directiveNode);
      break;
    case "title:":
      this.document.title = directiveNode.directiveRawValue;
      break;
    case "author:":
      this.document.author = directiveNode.directiveRawValue;
      break;
    case "email:":
      this.document.email = directiveNode.directiveRawValue;
      break;
    default:
      this.document.directiveValues[directiveNode.directiveName] = directiveNode.directiveRawValue;
      break;
    }
  },

  interpretOptionDirective: function (optionDirectiveNode) {
    optionDirectiveNode.directiveArguments.forEach(function (pairString) {
      var pair = pairString.split(":");
      this.options[pair[0]] = this.convertLispyValue(pair[1]);
    }, this);
  },

  convertLispyValue: function (lispyValue) {
    switch (lispyValue) {
    case "t":
      return true;
    case "nil":
      return false;
    default:
      if (/^[0-9]+$/.test(lispyValue))
        return parseInt(lispyValue);
      return lispyValue;
    }
  },

  // ------------------------------------------------------------
  // <Paragraph> ::= <Blank> <Line>*
  // ------------------------------------------------------------

  parseParagraph: function () {
    var paragraphFisrtToken = this.lexer.peekNextToken();
    var paragraph = Node.createParagraph([]);
    this.setNodeOriginFromToken(paragraph, paragraphFisrtToken);

    var textContents = [];

    while (this.lexer.hasNext()) {
      var nextToken = this.lexer.peekNextToken();
      if (nextToken.type !== Lexer.tokens.line
          || nextToken.indentation < paragraphFisrtToken.indentation)
        break;
      this.lexer.getNextToken();
      textContents.push(nextToken.content);
    }

    paragraph.appendChild(this.createTextNode(textContents.join("\n")));

    return paragraph;
  },

  parseText: function (noEmphasis) {
    var lineToken = this.lexer.getNextToken();
    return this.createTextNode(lineToken.content, noEmphasis);
  },

  // ------------------------------------------------------------
  // <Text> (DOM Like)
  // ------------------------------------------------------------

  createTextNode: function (text, noEmphasis) {
    return noEmphasis ? Node.createText(null, { value: text })
      : this.inlineParser.parseEmphasis(text);
  }
};
Parser.prototype.originalParseElement = Parser.prototype.parseElement;

// ------------------------------------------------------------
// Parser for Inline Elements
//
// @refs org-emphasis-regexp-components
// ------------------------------------------------------------

function InlineParser() {
  this.preEmphasis     = " \t\\('\"";
  this.postEmphasis    = "- \t.,:!?;'\"\\)";
  this.borderForbidden = " \t\r\n,\"'";
  this.bodyRegexp      = "[\\s\\S]*?";
  this.markers         = "*/_=~+";

  this.emphasisPattern = this.buildEmphasisPattern();
  this.linkPattern = /\[\[([^\]]*)\](?:\[([^\]]*)\])?\]/g; // \1 => link, \2 => text
}

InlineParser.prototype = {
  parseEmphasis: function (text) {
    var emphasisPattern = this.emphasisPattern;
    emphasisPattern.lastIndex = 0;

    var result = [],
        match,
        previousLast = 0,
        savedLastIndex;

    while ((match = emphasisPattern.exec(text))) {
      var whole  = match[0];
      var pre    = match[1];
      var marker = match[2];
      var body   = match[3];
      var post   = match[4];

      {
        // parse links
        var matchBegin = emphasisPattern.lastIndex - whole.length;
        var beforeContent = text.substring(previousLast, matchBegin + pre.length);
        savedLastIndex = emphasisPattern.lastIndex;
        result.push(this.parseLink(beforeContent));
        emphasisPattern.lastIndex = savedLastIndex;
      }

      var bodyNode = [Node.createText(null, { value: body })];
      var bodyContainer = this.emphasizeElementByMarker(bodyNode, marker);
      result.push(bodyContainer);

      previousLast = emphasisPattern.lastIndex - post.length;
    }

    if (emphasisPattern.lastIndex === 0 ||
        emphasisPattern.lastIndex !== text.length - 1)
      result.push(this.parseLink(text.substring(previousLast)));

    if (result.length === 1) {
      // Avoid duplicated inline container wrapping
      return result[0];
    } else {
      return Node.createInlineContainer(result);
    }
  },

  depth: 0,
  parseLink: function (text) {
    var linkPattern = this.linkPattern;
    linkPattern.lastIndex = 0;

    var match,
        result = [],
        previousLast = 0,
        savedLastIndex;

    while ((match = linkPattern.exec(text))) {
      var whole = match[0];
      var src   = match[1];
      var title = match[2];

      // parse before content
      var matchBegin = linkPattern.lastIndex - whole.length;
      var beforeContent = text.substring(previousLast, matchBegin);
      result.push(Node.createText(null, { value: beforeContent }));

      // parse link
      var link = Node.createLink([]);
      link.src = src;
      if (title) {
        savedLastIndex = linkPattern.lastIndex;
        link.appendChild(this.parseEmphasis(title));
        linkPattern.lastIndex = savedLastIndex;
      } else {
        link.appendChild(Node.createText(null, { value: src }));
      }
      result.push(link);

      previousLast = linkPattern.lastIndex;
    }

    if (linkPattern.lastIndex === 0 ||
        linkPattern.lastIndex !== text.length - 1)
      result.push(Node.createText(null, { value: text.substring(previousLast) }));

    return Node.createInlineContainer(result);
  },

  emphasizeElementByMarker: function (element, marker) {
    switch (marker) {
    case "*":
      return Node.createBold(element);
    case "/":
      return Node.createItalic(element);
    case "_":
      return Node.createUnderline(element);
    case "=":
    case "~":
      return Node.createCode(element);
    case "+":
      return Node.createDashed(element);
    }
  },

  buildEmphasisPattern: function () {
    return new RegExp(
      "([" + this.preEmphasis + "]|^|\r?\n)" +               // \1 => pre
        "([" + this.markers + "])" +                         // \2 => marker
        "([^" + this.borderForbidden + "]|" +                // \3 => body
        "[^" + this.borderForbidden + "]" +
        this.bodyRegexp +
        "[^" + this.borderForbidden + "])" +
        "\\2" +
        "([" + this.postEmphasis +"]|$|\r?\n)",              // \4 => post
        // flags
        "g"
    );
  }
};

if (typeof exports !== "undefined") {
  exports.Parser = Parser;
  exports.InlineParser = InlineParser;
}

},{"./lexer.js":4,"./node.js":5,"./stream.js":7}],7:[function(require,module,exports){
function Stream(sequence) {
  this.sequences = sequence.split(/\r?\n/);
  this.totalLines = this.sequences.length;
  this.lineNumber = 0;
}

Stream.prototype.peekNextLine = function () {
  return this.hasNext() ? this.sequences[this.lineNumber] : null;
};

Stream.prototype.getNextLine = function () {
  return this.hasNext() ? this.sequences[this.lineNumber++] : null;
};

Stream.prototype.hasNext = function () {
  return this.lineNumber < this.totalLines;
};

if (typeof exports !== "undefined") {
  exports.Stream = Stream;
}

},{}],8:[function(require,module,exports){
let org = require('org')
window.org = org

},{"org":1}]},{},[8]);
