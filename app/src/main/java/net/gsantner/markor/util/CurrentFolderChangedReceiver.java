package net.gsantner.markor.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.model.Constants;

import java.io.File;

public class CurrentFolderChangedReceiver extends BroadcastReceiver {

    private MainActivity context;

    public CurrentFolderChangedReceiver(MainActivity activity) {
        this.context = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Constants.CURRENT_FOLDER_CHANGED)) {
            File directory = (File) intent.getSerializableExtra(Constants.CURRENT_FOLDER);
            File rootDir = (File) intent.getSerializableExtra(Constants.ROOT_DIR);
            toggleBreadcrumbsVisibility(directory, rootDir);
        }
    }

    private void toggleBreadcrumbsVisibility(File currentDir, File rootDir) {
        TextView breadcrumbs = context.findViewById(R.id.main__activity__breadcrumbs);
        if (currentDir.equals(rootDir)) {
            breadcrumbs.setVisibility(View.GONE);
        } else {
            breadcrumbs.setText(backButtonText(currentDir, rootDir));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && breadcrumbs.getVisibility() != View.VISIBLE) {
                showRevealAnimation(breadcrumbs);
            } else {
                breadcrumbs.setVisibility(View.VISIBLE);
            }
        }
    }

    private String backButtonText(File currentDir, File rootDir) {
        if (currentDir.getParentFile().equals(rootDir)) {
            return " > " + currentDir.getName();
        } else {
            return "... > " + currentDir.getParentFile().getName() + " > " + currentDir.getName();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void showRevealAnimation(View view) {
        final View myView = view;
        myView.setVisibility(View.INVISIBLE);
        final int height = myView.getHeight() / 2;
        myView.setTranslationY(-height);
        myView.animate()
                .translationY(0)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        myView.setVisibility(View.VISIBLE);
                        Animator anim = ViewAnimationUtils.createCircularReveal(myView,
                                myView.getWidth() / 16,
                                myView.getHeight() / 2,
                                0, Math.max(myView.getWidth(), myView.getHeight())
                        );
                        anim.start();
                    }
                })
                .start();
    }
}
