/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
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
