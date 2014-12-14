package me.writeily.writeilypro.dialog;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import me.writeily.writeilypro.R;
import me.writeily.writeilypro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class ShareDialog extends DialogFragment {

    private Button shareTextButton;
    private Button shareHtmlButton;

    public ShareDialog() {
    }

    public void sendBroadcast(int type) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.SHARE_BROADCAST_TAG);
        broadcast.putExtra(Constants.SHARE_TYPE_TAG, type);
        getActivity().sendBroadcast(broadcast);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.share_dialog_layout, container);

        shareTextButton = (Button) rootView.findViewById(R.id.share_text);
        shareHtmlButton = (Button) rootView.findViewById(R.id.share_html);

        // Set the dialog title
        if (getDialog() != null) {
            getDialog().setTitle(getResources().getString(R.string.share_as));
        }

        shareTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(Constants.SHARE_TXT_TYPE);
                dismiss();
            }
        });

        shareHtmlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBroadcast(Constants.SHARE_HTML_TYPE);
                dismiss();
            }
        });

        return rootView;
    }
}
