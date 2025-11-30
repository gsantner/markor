/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity.openeditor;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import net.gsantner.markor.activity.DocumentActivity;

public class OpenShareIntoActivity extends OpenEditorActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent openShare = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "")
                .setClass(this, DocumentActivity.class)
                .setPackage(getPackageName());

        _cu.animateToActivity(this, openShare, true, 1);
    }
}
