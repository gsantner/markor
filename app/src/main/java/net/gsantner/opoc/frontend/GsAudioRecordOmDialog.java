/*#######################################################
 *
 * SPDX-FileCopyrightText: 2020-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2020-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import omrecorder.AudioRecordConfig;
import omrecorder.OmRecorder;
import omrecorder.PullTransport;
import omrecorder.PullableSource;
import omrecorder.Recorder;


//
// Callback: Called when successfully recorded
// will contain path to file in cache directory. Must be copied to custom location in callback handler
//
// Add to build.gradle: implementation 'com.kailashdabhi:om-recorder:1.1.5'
// Add to manifest: <uses-permission android:name="android.permission.RECORD_AUDIO" />
//
@SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
public class GsAudioRecordOmDialog {
    public static void showAudioRecordDialog(final Activity activity, @StringRes final int titleResId, final GsCallback.a1<String> recordFinishedCallbackWithPathToTemporaryFile) {
        ////////////////////////////////////
        // Request permission in case not granted. Do not show dialog UI in this case
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return;
        }

        ////////////////////////////////////
        // Init
        final String EMOJI_MICROPHONE = "\uD83D\uDD34";
        final String EMOJI_STOP = "⭕";//"\uD83D\uDED1";
        final String EMOJI_RESTART = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? "\uD83D\uDD04" : EMOJI_MICROPHONE;
        final String EMOJI_SPEAKER = "\uD83D\uDD0A"; //"\uD83C\uDFA7";

        final AtomicBoolean isRecording = new AtomicBoolean();
        final AtomicBoolean isRecordSavedOnce = new AtomicBoolean();
        final AtomicReference<Recorder> recorder = new AtomicReference<>();
        final AtomicReference<MediaPlayer> mediaPlayer = new AtomicReference<>();
        final AtomicReference<AlertDialog> dialog = new AtomicReference<>();
        final AtomicReference<Long> startTime = new AtomicReference<>();
        final File TMP_FILE_RECORDING = generateFilename(activity.getCacheDir());
        if (TMP_FILE_RECORDING.exists()) {
            TMP_FILE_RECORDING.delete();
        }

        // Record management callbacks
        final GsCallback.a2<Boolean, Boolean> recorderManager = (cbArgRestart, cbArgStop) -> {
            if (cbArgRestart) {
                final PullableSource SRC_MICROPHONE = new PullableSource.Default(new AudioRecordConfig.Default(MediaRecorder.AudioSource.MIC, AudioFormat.ENCODING_PCM_16BIT, AudioFormat.CHANNEL_IN_STEREO, 44100));
                recorder.set(OmRecorder.wav(new PullTransport.Default(SRC_MICROPHONE), TMP_FILE_RECORDING));
                recorder.get().startRecording();
                startTime.set(System.currentTimeMillis());
            } else if (cbArgStop) {
                try {
                    recorder.get().stopRecording();
                    isRecordSavedOnce.set(true);

                    int[] diff = GsFileUtils.getTimeDiffHMS(System.currentTimeMillis(), startTime.get());
                    dialog.get().setMessage(String.format(Locale.getDefault(), "%02d:%02d:%02d / %s [.wav]", diff[0], diff[1], diff[2], GsFileUtils.getReadableFileSize(TMP_FILE_RECORDING.length(), true)));
                } catch (Exception ignored) {
                }
            }
        };

        ////////////////////////////////////
        // Create UI
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        final LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        final TextView playbackButton = new TextView(activity);
        final TextView recordButton = new TextView(activity);
        final View sep1 = new View(activity);
        sep1.setLayoutParams(new LinearLayout.LayoutParams(100, 1));

        // Record button
        recordButton.setTextColor(Color.BLACK);
        recordButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        recordButton.setGravity(Gravity.CENTER_HORIZONTAL);
        recordButton.setText(EMOJI_MICROPHONE);
        recordButton.setOnClickListener(v -> {
            if (isRecording.get()) {
                recorderManager.callback(false, true);
            } else {
                recorderManager.callback(true, false);
            }

            // Update state
            isRecording.set(!isRecording.get());
            recordButton.setText(isRecording.get() ? EMOJI_STOP : EMOJI_RESTART);
            playbackButton.setEnabled(!isRecording.get());
        });

        final GsCallback.a0 playbackStoppedCallback = () -> {
            recordButton.setEnabled(true);
            if (mediaPlayer.get() != null) {
                mediaPlayer.getAndSet(null).release();
            }
            playbackButton.setText(EMOJI_SPEAKER);
        };

        // Play button
        playbackButton.setTextColor(Color.BLACK);
        playbackButton.setEnabled(false);
        playbackButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        playbackButton.setGravity(Gravity.CENTER_HORIZONTAL);
        playbackButton.setText(EMOJI_SPEAKER);
        playbackButton.setOnClickListener(v -> {
            final boolean startPlaybackNow = mediaPlayer.get() == null;
            recordButton.setEnabled(false);
            playbackButton.setText(startPlaybackNow ? EMOJI_STOP : EMOJI_SPEAKER);
            if (startPlaybackNow) {
                try {
                    MediaPlayer player = new MediaPlayer();
                    mediaPlayer.set(player);
                    player.setDataSource(TMP_FILE_RECORDING.getAbsolutePath());
                    player.prepare();
                    player.start();
                    player.setOnCompletionListener(mp -> playbackStoppedCallback.callback());
                    player.setLooping(false);
                } catch (IOException ignored) {
                }
            } else {
                mediaPlayer.get().stop();
                playbackStoppedCallback.callback();
            }
        });

        ////////////////////////////////////
        // Callback for OK & Cancel dialog button
        final DialogInterface.OnClickListener dialogOkAndCancelListener = (dialogInterface, dialogButtonCase) -> {
            final boolean isSavePressed = (dialogButtonCase == DialogInterface.BUTTON_POSITIVE);
            if (isRecording.get() || isRecordSavedOnce.get()) {
                try {
                    recorder.get().stopRecording();
                } catch (Exception ignored) {
                }
                if (!isSavePressed) {
                    if (TMP_FILE_RECORDING.exists()) {
                        TMP_FILE_RECORDING.delete();
                    }
                } else if (recordFinishedCallbackWithPathToTemporaryFile != null) {
                    recordFinishedCallbackWithPathToTemporaryFile.callback(TMP_FILE_RECORDING.getAbsolutePath());
                }
            }
            dialogInterface.dismiss();
        };

        ////////////////////////////////////
        // Tooltip
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackButton.setTooltipText("Play recording / Stop playback");
            recordButton.setTooltipText("Record Audio (Voice Note)");
        }

        ////////////////////////////////////
        // Add to layout
        layout.addView(playbackButton);
        layout.addView(sep1);
        layout.addView(recordButton);

        ////////////////////////////////////
        // Create & show dialog
        dialogBuilder
                .setTitle(titleResId)
                .setPositiveButton(android.R.string.ok, dialogOkAndCancelListener)
                .setNegativeButton(android.R.string.cancel, dialogOkAndCancelListener)
                .setMessage("00:00:00 / 0kB [.wav]")
                .setView(layout);
        dialog.set(dialogBuilder.create());
        Window w;
        dialog.get().show();
        if ((w = dialog.get().getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams wlp = w.getAttributes();
            wlp.gravity = Gravity.BOTTOM;
            w.setAttributes(wlp);
        }
    }

    public static File generateFilename(final File recordDirectory) {
        final String datestr = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(new Date());
        return new File(recordDirectory, datestr + "-record.wav");
    }
}
