package me.writeily;

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

import java.io.File;

import me.writeily.model.Constants;

class CurrentFolderChangedReceiver extends BroadcastReceiver {

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
        TextView breadcrumbs = (TextView) context.findViewById(R.id.breadcrumbs);
        if (currentDir.equals(rootDir)) {
            breadcrumbs.setVisibility(View.GONE);
        } else {
            breadcrumbs.setText(backButtonText(currentDir, rootDir));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                showCircularReveal(breadcrumbs);
            } else {
                breadcrumbs.setVisibility(View.VISIBLE);
            }
        }
    }

    private String backButtonText(File currentDir, File rootDir) {
        if (currentDir.getParentFile().equals(rootDir)) {
            return "Writeily > " + currentDir.getName();
        } else {
            return "... > " + currentDir.getParentFile().getName() + " > " + currentDir.getName();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void showCircularReveal(View view) {
        final View myView = view;
        myView.setVisibility(View.INVISIBLE);
        final int height = myView.getHeight() / 2;
        myView.setTranslationY(-height);
        myView.animate()
                .translationY(0)
                .setDuration(300)
                .setStartDelay(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        myView.setVisibility(View.VISIBLE);
                        final int cx = myView.getWidth() / 2;
                        final int cy = myView.getHeight() / 2;
                        final int finalRadius = Math.max(myView.getWidth(), myView.getHeight());
                        Animator anim = ViewAnimationUtils.createCircularReveal(myView, cx, cy, 0, finalRadius);
                        anim.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {

                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        anim.start();
                    }
                })
                .start();
    }
}
