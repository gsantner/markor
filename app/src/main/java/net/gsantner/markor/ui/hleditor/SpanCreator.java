/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import android.text.ParcelableSpan;
import android.text.style.ParagraphStyle;

import java.util.regex.Matcher;

public class SpanCreator {
    public interface ParagraphStyleCreator {
        ParagraphStyle create(Matcher matcher, int iM);
    }

    public interface ParcelableSpanCreator {
        ParcelableSpan create(Matcher matcher, int iM);
    }
}
