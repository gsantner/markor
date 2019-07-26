/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Context;
import android.webkit.MimeTypeMap;

import net.gsantner.markor.App;

public class ContextUtils extends net.gsantner.opoc.util.ContextUtils {
    public ContextUtils(Context context) {
        super(context);
    }

    public static ContextUtils get() {
        return new ContextUtils(App.get());
    }

    public static String getMimeType(String url) {
        String ext = MimeTypeMap.getFileExtensionFromUrl(url);
        return ext != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) : null;
    }
}
