package com.example.cumttraffic.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.example.cumttraffic.R;

public class HintDialog extends DialogFragment {
    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String REQUEST_CODE = "request_code";

    public static HintDialog newInstance(int title, int message, int requestCode) {
        HintDialog frag = new HintDialog();
        Bundle args = new Bundle();
        args.putInt(TITLE, title);
        args.putInt(MESSAGE, message);
        args.putInt(REQUEST_CODE, requestCode);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int title = getArguments().getInt(TITLE);
        int message = getArguments().getInt(MESSAGE);
        final int requestCode = getArguments().getInt(REQUEST_CODE);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((DialogFragmentCallback) getActivity()).doPositiveClick(requestCode);
                            }
                        }
                )
                .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((DialogFragmentCallback) getActivity()).doNegativeClick(requestCode);
                            }
                        }
                )
                .create();
    }

    public void show(FragmentManager fragmentManager, String simpleName) {
    }

    public interface DialogFragmentCallback {

        void doPositiveClick(int requestCode);

        void doNegativeClick(int requestCode);
    }

}
