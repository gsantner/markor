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
import android.text.style.MetricAffectingSpan;
import android.text.style.ParagraphStyle;

import java.util.regex.Matcher;

public interface SpanCreator {
    Object create(Matcher matcher, int iM);

    interface ParagraphStyleCreator {
        ParagraphStyle create(Matcher matcher, int iM);
    }

    interface ParcelableSpanCreator {
        ParcelableSpan create(Matcher matcher, int iM);
    }

    interface MetricAffectingSpanCreator {
        MetricAffectingSpan create(Matcher matcher, int iM);
    }

}
