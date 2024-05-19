/*#######################################################
 *
 * SPDX-FileCopyrightText: 2020-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2020-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mertakdut.BookSection;
import com.github.mertakdut.Reader;
import com.github.mertakdut.exception.OutOfPagesException;
import com.github.mertakdut.exception.ReadingException;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

// Just some experimental code
public class GsCoolExperimentalStuff {

    public static String convertEpubToText(final File filepath, final String translatedStringForPage) {
        //final String filepath = new File("/sdcard/epub.epub");
        final StringBuilder sb = new StringBuilder();
        final Reader reader = new Reader();
        reader.setMaxContentPerSection(1000);
        reader.setIsIncludingTextContent(true);
        try {
            reader.setFullContent(filepath.getAbsolutePath());
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


    public static void showSpeedReadDialog(final Activity activity, final String text) {
        ////////////////////////////////////
        // Init
        final AtomicReference<AlertDialog> dialog = new AtomicReference<>();
        final AtomicReference<Long> wpm = new AtomicReference<>((long) (1000 * 60 / 300));
        final AtomicReference<String> displayString = new AtomicReference<>();
        final AtomicReference<Integer> textPos = new AtomicReference<>(0);

        ////////////////////////////////////
        // Create UI
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        final View sep1 = new View(activity);
        sep1.setLayoutParams(new LinearLayout.LayoutParams(100, 1));
        final TextView currentWordTextShower = new TextView(activity);

        final Button buttonMoveWords = new Button(activity);
        buttonMoveWords.setText("+100 words (click)\n-100 long click swipe down");
        buttonMoveWords.setOnClickListener(v -> textPos.set(Math.max(0, Math.min(text.length() - 2, textPos.get() + 100))));
        buttonMoveWords.setOnLongClickListener(v -> {
            textPos.set(Math.max(0, textPos.get() - 100));
            return false;
        });

        final TextView textViewTextDisplay = new TextView(activity);
        textViewTextDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 72);
        textViewTextDisplay.setGravity(Gravity.CENTER);
        textViewTextDisplay.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        textViewTextDisplay.setMinHeight((int) GsContextUtils.instance.convertDpToPx(activity, 1000));
        Runnable updateViewOnUi = () -> {
            textViewTextDisplay.setText(displayString.get());
            currentWordTextShower.setText(String.format(Locale.ENGLISH, "Word: %d", textPos.get() + 1));
        };

        Thread s = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    String[] strs = text.replaceAll("[^\\p{L}\\p{Nd}]+", "\n").replaceAll("\\s+", "\n").split("\n");
                    for (textPos.set(0); textPos.get() < strs.length; textPos.set(textPos.get() + 1)) {
                        displayString.set(strs[textPos.get()]);
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
        layout.addView(buttonMoveWords);
        layout.addView(currentWordTextShower);
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
