/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
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
