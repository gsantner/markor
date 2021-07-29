package net.gsantner.opoc.android.dummy;

/*
 * Copyright (c) 2021 Gregor Santner <https://gsantner.net>
 * License: Creative Commons Zero (CC0 1.0) / Public Domain
 *  http://creativecommons.org/publicdomain/zero/1.0/
 *
 * You can do whatever you want with this. If we meet some day, and you think it is worth it,
 * you can buy me a drink in return. Provided as is without any kind of warranty. Do not blame
 * or ask for support if something goes wrong.  - Gregor Santner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

import android.text.Editable;
import android.text.TextWatcher;

import net.gsantner.opoc.util.Callback;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class TextWatcherDummy implements TextWatcher {
    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    @Override
    public void afterTextChanged(final Editable s) {
    }

    public static TextWatcher before(final Callback.a4<CharSequence, Integer, Integer, Integer> impl) {
        return new TextWatcherDummy() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                impl.callback(s, start, count, after);
            }
        };
    }

    public static TextWatcher on(final Callback.a4<CharSequence, Integer, Integer, Integer> impl) {
        return new TextWatcherDummy() {
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                impl.callback(s, start, before, count);
            }
        };
    }

    public static TextWatcher after(final Callback.a1<Editable> impl) {
        return new TextWatcherDummy() {
            public void afterTextChanged(final Editable s) {
                impl.callback(s);
            }
        };
    }
}
