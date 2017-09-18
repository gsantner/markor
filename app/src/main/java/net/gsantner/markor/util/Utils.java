package net.gsantner.markor.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Utils {
    private static int screenWidth = 0;
    private static int screenHeight = 0;

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int getScreenHeight(Context c) {
        if (screenHeight == 0) {
            WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenHeight = size.y;
        }

        return screenHeight;
    }

    public static int getScreenWidth(Context c) {
        if (screenWidth == 0) {
            WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;
        }

        return screenWidth;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean renameFileInSameFolder(File srcFile, String destFilename, String cacheDir) {
        File destFile = new File(srcFile.getParent(), destFilename);
        Random random = new Random();
        File cacheFile;

        // Move file temporary, otherwise "hello.txt"->"heLLo.txt" will not work (file exist)
        do {
            cacheFile = new File(cacheDir, random.nextInt() + "rename.tmp");
        } while (cacheFile.exists());
        try {
            FileUtils.moveFile(srcFile, cacheFile);
            FileUtils.moveFile(cacheFile, destFile);
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
        }
    }
}