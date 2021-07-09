package com.example.cumttraffic.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cumttraffic.R;
import com.example.cumttraffic.application.MyApplication;

public class AboutActivity extends BaseActivity {

    private TextView textVersion, textName;
    private ImageView aboutback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullScreenConfig();
        setContentView(R.layout.activity_about);
        init();

        textVersion.setText("当前版本：" + getAppVersionName(com.example.cumttraffic.activity.AboutActivity.this));

        textName.setText(getAppName(MyApplication.getContext()));

        aboutback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    private void init() {
        textVersion = findViewById(R.id.text_about_version);
        textName = findViewById(R.id.text_about_name);
        aboutback = findViewById(R.id.aboutback);
    }

    public static String getAppVersionCode(Context context) {
        int versioncode = 0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            // versionName = pi.versionName;
            versioncode = pi.versionCode;
        } catch (Exception e) {
            //Log.e("VersionInfo", "Exception", e);
        }
        return versioncode + "";
    }

    public static String getAppVersionName(Context context) {
        String versionName = null;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
        } catch (Exception e) {
            //Log.e("VersionInfo", "Exception", e);
        }
        return versionName;
    }

    public static String getAppName(Context context) {
        if (context == null) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            return String.valueOf(packageManager.getApplicationLabel(context.getApplicationInfo()));
        } catch (Throwable e) {
        }
        return null;
    }


}
