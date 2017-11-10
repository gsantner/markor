package net.gsantner.markor.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public class ActivityUtils extends net.gsantner.opoc.util.ActivityUtils {

    public ActivityUtils(Activity activity) {
        super(activity);
    }

    public void showRateOnGplayDialog() {
        String pkgId = "details?id=" + _activity.getPackageName();
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://" + pkgId));
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                (Build.VERSION.SDK_INT >= 21 ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            _activity.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            _activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/" + pkgId)));
        }
    }
}
