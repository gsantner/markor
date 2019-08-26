package net.gsantner.markor.activity;

import android.Manifest;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.util.LogWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import net.gsantner.markor.R;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;

import java.io.File;
import java.io.IOException;

/**
 * Requires permission to record audio permission (200);
 *
 * Simple DialogFragment. Includes functionality to record audio notes from within text files.
 * Audio notes will be saved in the same folder as the text file.
 *
 * Use the {@link AudioToNoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AudioToNoteFragment extends DialogFragment {
    private static final String ARG_PATH = "textFilePath";

    private static HighlightingEditor highlightEditor;

    private static final String FILE_EXTENSION = ".3gp";

    private String textFilePath;
    private String currentAudioFilePath;

    private boolean recording = false;
    private MediaRecorder recorder;

    private boolean hasRecording = false;

    private Button saveButton;

    public AudioToNoteFragment() {
        // Required empty public constructor
    }

    public static AudioToNoteFragment newInstance(String path, HighlightingEditor highlightingEditor) {
        AudioToNoteFragment fragment = new AudioToNoteFragment();
        highlightEditor = highlightingEditor;
        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.textFilePath = getArguments().getString(ARG_PATH); // textFilePath needs to be set !
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_to_note, container, false);
        Button recordingButton = view.findViewById(R.id.start_recording);
        saveButton = view.findViewById(R.id.save_recording);
        Button cancelButton = view.findViewById(R.id.cancel_recording);

        Log.w("DEV", "onCreateView: textFilePath: " + textFilePath);

        doSelfPermissions();

        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recording) {
                    recording = false;
                    stopRecording();
                    recordingButton.setText("Start");
                } else {
                    recording = true;
                    startRecording();
                    recordingButton.setText("Stop");
                }
            }
        });

        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasRecording) {
                    highlightEditor.insertOrReplaceTextOnCursor(
                            currentAudioFilePath.substring(
                                    currentAudioFilePath.lastIndexOf("/")));
                    dismiss();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we are still recording, terminate it
                if(recorder != null) {
                    stopRecording();
                }

                // we assume the recorded audio is not wanted -> have to remove it
                if (hasRecording && currentAudioFilePath != null) {
                    File audioFile = new File(currentAudioFilePath);
                    audioFile.delete();
                }
                dismiss();
            }
        });

        return view;
    }

    private void startRecording() {
        if ((this.textFilePath == null) || this.textFilePath.equals("")) {
            Log.e("DEV", "startRecording: Cannot record audio without set textFilePath !");
            return;
        }

        this.currentAudioFilePath = generateFilePath();

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(currentAudioFilePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("DEV", "stopRecording: IOException occurred while preparing recorder", e);
        }
        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;

        hasRecording = true;
        saveButton.setEnabled(true);
    }

    //TODO: Incorporate this in the app wide permission handling
    private boolean doSelfPermissions() {
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 200);
        return true;
    }

    private String generateFilePath() {
        String audioFileGeneric = textFilePath.substring(0, textFilePath.lastIndexOf(".")) + "_audio";

        int i = 1;
        File audioFile = new File(audioFileGeneric + FILE_EXTENSION);
        while (audioFile.exists()) {
            audioFile = new File(audioFileGeneric + "_" + i + FILE_EXTENSION);
            i++;
        }

        Log.w("DEV", "generateFilePath: absolute file path: " + audioFile.getAbsolutePath());
        return audioFile.getAbsolutePath();
    }
}