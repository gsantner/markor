/*#######################################################
 *
 * SPDX-FileCopyrightText: 2020-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2020-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.wrapper;

import android.view.View;
import android.widget.AdapterView;

public class GsAndroidSpinnerOnItemSelectedAdapter implements AdapterView.OnItemSelectedListener {

    private final GsCallback.a1<Integer> _callback;

    public GsAndroidSpinnerOnItemSelectedAdapter(final GsCallback.a1<Integer> callback) {
        _callback = callback;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        _callback.callback(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        _callback.callback(-1);
    }
}
