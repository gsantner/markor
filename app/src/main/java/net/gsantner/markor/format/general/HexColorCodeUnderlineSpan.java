/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.graphics.Color;
import android.text.ParcelableSpan;

import net.gsantner.markor.ui.hleditor.SpanCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorCodeUnderlineSpan implements SpanCreator.ParcelableSpanCreator {
    public static final Pattern PATTERN = Pattern.compile("(?:\\s|[\";,:'*]|^)(#[A-Fa-f0-9]{6,8})+(?:\\s|[\";,:'*]|$)");

    public ParcelableSpan create(Matcher matcher, int iM) {
        return new ColorUnderlineSpan(Color.parseColor(matcher.group(1)), 3f);

    }
}
