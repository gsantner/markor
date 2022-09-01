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

import android.os.Bundle;

import androidx.annotation.Nullable;

import net.gsantner.markor.model.Document;

public class OpenEditorQuickNoteActivity extends OpenEditorActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openEditorForFile(_appSettings.getQuickNoteFile(), Document.EXTRA_FILE_LINE_NUMBER_LAST);
    }
}
