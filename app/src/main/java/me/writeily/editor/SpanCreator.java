package me.writeily.editor;

import android.text.ParcelableSpan;

import java.util.regex.Matcher;

public interface SpanCreator {
    ParcelableSpan create(Matcher m);
}
