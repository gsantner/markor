/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;

public class GsAndroidSupportMeWrapper extends GsContextUtils {
    private LocalSettingsImpl _localSettingsImpl;

    public GsAndroidSupportMeWrapper(Activity activity) {
        super();
        _localSettingsImpl = new LocalSettingsImpl(activity.getApplicationContext());
    }

    public void openPayPalDonationPage(final Context context) {
        String id = getAppIdUsedAtManifest(context);
        String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TVV24QBGMN23C&source=self.gsantner.net%2F" + id;
        openWebpageInExternalBrowser(context, url);
    }


    public void openGeneralDonatePage(final Context context) {
        openWebpageInExternalBrowser(context, context.getString(R.string.app_donate_url));
    }

    public void mainOnResume(Context context) {
        if (_localSettingsImpl.all14dRequest()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.donate_)
                    .setCancelable(false)
                    .setNegativeButton(R.string.close, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton("PayPal", (dialogInterface, i) -> {
                        openPayPalDonationPage(context);
                        dialogInterface.dismiss();
                    })
                    .setNeutralButton(R.string.donate_, (dialogInterface, i) -> {
                        openGeneralDonatePage(context);
                        dialogInterface.dismiss();
                    })
                    .setMessage(R.string.do_you_like_this_project_want_donate_to_keep_alive);
            dialog.show();
        }
    }

    private class LocalSettingsImpl extends GsSharedPreferencesPropertyBackend {
        private final SharedPreferences _prefCache;

        public LocalSettingsImpl(final Context context) {
            super.init(context, "AndroidSupportMeWrapper.LocalSettingsImpl");
            _prefCache = context.getSharedPreferences("cache", Context.MODE_PRIVATE);
        }

        public boolean all14dRequest() {
            return afterDaysTrue("all14dRequest", 31, 3);
        }
    }
}
