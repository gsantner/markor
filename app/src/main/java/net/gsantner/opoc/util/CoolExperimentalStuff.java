package net.gsantner.opoc.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mertakdut.BookSection;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;
import com.github.mertakdut.exception.ReadingException;

import java.util.concurrent.atomic.AtomicReference;

// License Public domain/CC0 for now
// Just some experimental code
public class CoolExperimentalStuff {

    public static String convertEpubToText(final String translatedStringForPage) {
        final String filepath = "/sdcard/epub.epub";
        final StringBuilder sb = new StringBuilder();
        final Reader reader = new Reader();
        reader.setMaxContentPerSection(1000);
        reader.setIsIncludingTextContent(true);
        try {
            reader.setFullContent(filepath);
        } catch (Exception e) {
            return "";
        }
        int pageIndex = 0;
        for (boolean hasMoreContent = true; hasMoreContent; ) {
            BookSection bookSection = null;
            try {
                bookSection = reader.readSection(pageIndex);
            } catch (ReadingException e) {
                hasMoreContent = false;
            } catch (OutOfPagesException e) {
                hasMoreContent = false;
            }
            pageIndex++;
            if (bookSection != null) {
                String pageLabel = bookSection.getLabel();
                pageLabel = "\n\n## " + ((!TextUtils.isEmpty(pageLabel) && !pageLabel.equals("null")) ? pageLabel : (translatedStringForPage + (pageIndex + 1))) + "\n\n";
                String pageText = bookSection.getSectionTextContent();
                if (pageText.length() > 300) {
                    sb.append(pageLabel);
                }
                sb.append(pageText);
            }
        }
        return sb.toString().replace("&nbsp;", " ").replaceAll("[ ]{4,}", "    ").replace("    ", "  \n\n");
    }


    public static void showSpeedReadDialog(final Activity activity, @StringRes final int titleResId, String text) {
        ////////////////////////////////////
        // Init
        final AtomicReference<AlertDialog> dialog = new AtomicReference<>();
        final AtomicReference<Long> wpm = new AtomicReference<>((long) (1000 * 60 / 300));
        final AtomicReference<String> displayString = new AtomicReference<>();

        ////////////////////////////////////
        // Create UI
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        final View sep1 = new View(activity);
        sep1.setLayoutParams(new LinearLayout.LayoutParams(100, 1));

        final TextView textViewTextDisplay = new TextView(activity);
        textViewTextDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        textViewTextDisplay.setGravity(Gravity.CENTER);
        textViewTextDisplay.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        textViewTextDisplay.setMinHeight((int) new ContextUtils(activity).convertDpToPx(1000));
        Runnable updateViewOnUi = () -> textViewTextDisplay.setText(displayString.get());

        Thread s = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    String[] strs = text.replaceAll("[^\\p{L}\\p{Nd}]+", "\n").replaceAll("\\s+", "\n").split("\n");
                    for (int i = 0; i < strs.length; i++) {
                        displayString.set(strs[i]);
                        textViewTextDisplay.post(updateViewOnUi);
                        Thread.sleep(wpm.get());
                    }

                } catch (Exception e) {
                }
            }
        };
        s.start();


        ////////////////////////////////////
        // Callback for OK & Cancel dialog button
        DialogInterface.OnClickListener dialogOkAndCancelListener = (dialogInterface, dialogButtonCase) -> {
            dialogInterface.dismiss();
        };

        ////////////////////////////////////
        // Add to layout
        layout.addView(textViewTextDisplay);

        ////////////////////////////////////
        // Create & show dialog
        dialogBuilder
                .setNegativeButton(android.R.string.cancel, dialogOkAndCancelListener)
                .setView(layout);
        dialog.set(dialogBuilder.create());
        Window w;
        dialog.get().show();
        if ((w = dialog.get().getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }
}
