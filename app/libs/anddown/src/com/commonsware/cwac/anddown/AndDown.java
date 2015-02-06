package com.commonsware.cwac.anddown;

public class AndDown {
  static {
    System.loadLibrary("anddown");
  }

  // extension flags for markdown rendering (see document.h for detail)
  // no extensions
  public static final int FLAG_EXTENSIONS_NONE = 0;
  // basic extensions set: tables(bit 0), autolink(bit 3), strike through(bit 4), underline(bit 5)
  public static final int FLAG_EXTENSIONS_BASIC = 0x39;
  // medium extensions set: fenced code(bit 1), highlight(bit 6), quote(bit 7), superscript(bit 8)
  public static final int FLAG_EXTENSIONS_MEDIUM = FLAG_EXTENSIONS_BASIC | 0x1c2;
  // advance extensions set: footnotes(bit 2), math(bit 9)
  public static final int FLAG_EXTENSIONS_ADVANCE = FLAG_EXTENSIONS_MEDIUM | 0x204;

  static final int[] EXTENSIONS_LEVEL = {
    FLAG_EXTENSIONS_NONE,
    FLAG_EXTENSIONS_BASIC,
    FLAG_EXTENSIONS_MEDIUM,
    FLAG_EXTENSIONS_ADVANCE,
  };

  // Disable emphasis_between_words(bit 11)
  public static final int FLAG_NO_INTRA_EMPHASIS = 1 << 11;
  // Require a space after '#' in headers(bit 12)
  public static final int FLAG_SPACE_HEADERS = 1 << 12;
  // Instead of guessing by context, parse $inline math$ and $$always block math$$(bit 13)
  public static final int FLAG_MATH_EXPLICIT = 1 << 13;
  // Don't parse indented code blocks(bit 14)
  public static final int FLAG_DISABLE_INDENTED_CODE = 1 << 14;

  // default flag for markdown rendering
  public static final int FLAG_DEFAULT = FLAG_EXTENSIONS_NONE;

  public native String markdownToHtml(String raw, int flag);

  public String markdownToHtml(String raw) {
    return markdownToHtml(raw, FLAG_DEFAULT);
  }

  /**
   * convent markdown to html
   *
   * @raw: markdown in plain text
   * @flag: extension flags for markdown rendering
   * @level: 0 - none, 1 - basic, 2 - medium, 3 - advance, other value are ignored
   */
  public String markdownToHtml(String raw, int flag, int level) {
    if (level >= 0 && level < EXTENSIONS_LEVEL.length) {
      flag |= EXTENSIONS_LEVEL[level];
    }
    return markdownToHtml(raw, flag);
  }
}
