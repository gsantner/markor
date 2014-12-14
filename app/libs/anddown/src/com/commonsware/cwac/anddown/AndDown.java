package com.commonsware.cwac.anddown;

public class AndDown {
  static {
    System.loadLibrary("anddown");
  }

  public native String markdownToHtml(String raw);
}
