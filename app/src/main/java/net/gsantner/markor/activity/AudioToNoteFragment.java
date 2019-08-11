package net.gsantner.markor.activity;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import net.gsantner.markor.R;
import java.io.IOException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link AudioToNoteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AudioToNoteFragment extends DialogFragment {
    private static final String ARG_PATH = "path";
    private String path;

    private boolean recording = false;
    private MediaRecorder recorder;

    public AudioToNoteFragment() {
        // Required empty public constructor
    }

    public static AudioToNoteFragment newInstance(String path) {
        AudioToNoteFragment fragment = new AudioToNoteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_to_note, container, false);
        Button recordingButton = view.findViewById(R.id.start_recording);
        Log.w("DEV", "onCreateView: path: " + path);
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
        return view;
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(path + "recording");
        Log.w("DEV", "stopRecording: path: " + path +"  name: " + recording);
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
    }
}


// TODO: Test on real device. MediaRecorder does not run on simulated devices
// TODO: Test behaviour when creating multiple files
