package com.example.cumttraffic.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @ClassName BaseActivity
 */
public class BaseActivity extends AppCompatActivity {

    public static Toast toast;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    // 全屏显示
    protected void fullScreenConfig() {
        // 去除标题栏
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.hide();
        }
    }

    // 实现在子线程中显示Toast
    public void showToastInThread(Context context, String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    protected void hideToast() {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
    }
}
