package net.gsantner.markor.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.ActivityUtils;

public class AndroidSupportMeWrapper extends ActivityUtils {
    private LocalSettingsImpl _localSettingsImpl;

    public AndroidSupportMeWrapper(Activity activity) {
        super(activity);
        _localSettingsImpl = new LocalSettingsImpl(_context.getApplicationContext());
    }

    public void openPayPalDonationPage() {
        String id = getPackageIdManifest();
        String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TVV24QBGMN23C&source=self.gsantner.net%2F" + id;
        openWebpageInExternalBrowser(url);
    }


    public void openGeneralDonatePage() {
        openWebpageInExternalBrowser(_context.getString(R.string.app_donate_url));
    }

    public void mainOnResume() {
        if (_localSettingsImpl.all14dRequest()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(_context);
            dialog.setTitle(R.string.donate_)
                    .setCancelable(false)
                    .setNegativeButton(R.string.close, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton("PayPal", (dialogInterface, i) -> {
                        openPayPalDonationPage();
                        dialogInterface.dismiss();
                    })
                    .setNeutralButton(R.string.donate_, (dialogInterface, i) -> {
                        openGeneralDonatePage();
                        dialogInterface.dismiss();
                    })
                    .setMessage(R.string.do_you_like_this_project_want_donate_to_keep_alive);
            dialog.show();
        }
    }

    private class LocalSettingsImpl extends SharedPreferencesPropertyBackend {
        private final SharedPreferences _prefCache;

        public LocalSettingsImpl(Context _context) {
            super(_context, "AndroidSupportMeWrapper.LocalSettingsImpl");
            _prefCache = _context.getSharedPreferences("cache", Context.MODE_PRIVATE);
        }

        public boolean all14dRequest() {
            return afterDaysTrue("all14dRequest", 14, 3);
        }

    }
}
