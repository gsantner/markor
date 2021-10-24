/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity.openeditor;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import net.gsantner.markor.activity.DocumentRelayActivity;
import net.gsantner.markor.model.Document;

public class OpenShareIntoActivity extends OpenEditorActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent openShare = new Intent(this, DocumentRelayActivity.class)
                .setAction(Intent.ACTION_SEND)
                .putExtra(Document.EXTRA_PATH_IS_FOLDER, true)
                .putExtra(Intent.EXTRA_TEXT, "");
        openActivityAndClose(openShare, null);
    }
}
