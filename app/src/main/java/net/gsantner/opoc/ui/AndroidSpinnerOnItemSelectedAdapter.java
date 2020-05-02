/*#######################################################
 *
 *   Maintained by Gregor Santner, 2020-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.ui;

import android.view.View;
import android.widget.AdapterView;

import net.gsantner.opoc.util.Callback;

public class AndroidSpinnerOnItemSelectedAdapter implements AdapterView.OnItemSelectedListener {

    private final Callback.a1<Integer> _callback;

    public AndroidSpinnerOnItemSelectedAdapter(final Callback.a1<Integer> callback) {
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
