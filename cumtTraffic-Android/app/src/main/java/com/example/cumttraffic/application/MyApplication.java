package com.example.cumttraffic.application;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.example.cumttraffic.constant.NetConstant;

public class MyApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        MultiDex.install(this);
    }

    public static Context getContext() {
        return context;
    }
}
