package com.example.cumttraffic.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.cumttraffic.R;
import com.example.cumttraffic.activity.AboutActivity;
import com.example.cumttraffic.activity.LoginActivity;
import com.example.cumttraffic.application.MyApplication;

import static android.content.Context.MODE_PRIVATE;

public class MeFragment extends Fragment implements View.OnClickListener {

    private RelativeLayout layoutAbout;
    private CardView layoutQuit;
    private TextView userTel;
    private SharedPreferences sp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_me, container, false);
        return view;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
        layoutQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("提示")
                            .setMessage("确定退出登陆吗？")
                            .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent().setClass(getActivity(),LoginActivity.class));

                                    SharedPreferences.Editor editor = MyApplication.getContext().
                                            getSharedPreferences("login_info", Context.MODE_PRIVATE).edit();
                                    editor.putBoolean("isLogged",false);
                                    editor.apply();

                                    userTel.setText("");
                                    getActivity().finish();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();

            }
        });
        layoutAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(getActivity(), AboutActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void init(){
        sp = getActivity().getSharedPreferences("login_info", MODE_PRIVATE);
        String LoggedID = sp.getString("telphone","");
        userTel = getActivity().findViewById(R.id.user_tel);
        userTel.setText(LoggedID);

        layoutAbout = getActivity().findViewById(R.id.layout_me_about);
        layoutAbout.setOnClickListener(this);

        layoutQuit = getActivity().findViewById(R.id.layout_me_quit);
        layoutQuit.setOnClickListener(this);

    }

}
