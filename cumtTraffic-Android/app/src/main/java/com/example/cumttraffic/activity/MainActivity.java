package com.example.cumttraffic.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.cumttraffic.R;
import com.example.cumttraffic.constant.Api;
import com.example.cumttraffic.constant.NetConstant;
import com.example.cumttraffic.fragments.MapFragment;
import com.example.cumttraffic.fragments.MeFragment;
import com.example.cumttraffic.model.Bus;
import com.example.cumttraffic.mqtt.MqttManager;
import com.example.cumttraffic.utils.PermissionUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends BaseActivity  {

    private BottomNavigationView bottomNavigationView;
    private Fragment fragMap, fragMe;

    private Fragment[] fragments;

    //用于记录上个选择的Fragment
    public static int lastFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullScreenConfig();
        setContentView(R.layout.activity_main);
        initView();
        initFragment();
        startMqttServer();
    }

    private void initView(){
        bottomNavigationView = findViewById(R.id.bottom_nav);
    }

    private void initFragment() {
        fragMap = new MapFragment();
        fragMe = new MeFragment();
        fragments = new Fragment[]{fragMap, fragMe};
        switch (lastFragment) {
            case 0:
                getSupportFragmentManager().beginTransaction().replace(R.id.linear_frag_container, fragMap).show(fragMap).commit();
                break;
            case 1:
                getSupportFragmentManager().beginTransaction().replace(R.id.linear_frag_container, fragMe).show(fragMe).commit();
                break;
        }
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.bnavigation_map:
                        if (lastFragment != 0) {
                            switchFragment(lastFragment, 0);
                            lastFragment = 0;
                        }
                        return true;
                    case R.id.bnavigation_me:
                        if (lastFragment != 1) {
                            switchFragment(lastFragment, 1);
                            lastFragment = 1;
                        }
                        return true;
                }
                return true;
            }
        });
    }

    private void switchFragment(int lastIndex, int index) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(fragments[lastIndex]);
        if (fragments[index].isAdded() == false) {
            transaction.add(R.id.linear_frag_container, fragments[index]);
        }
        transaction.show(fragments[index]).commitAllowingStateLoss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] paramArrayOfInt) {
        Log.e("sty", "onRequestPermissionsResult");
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS_CODE) {
            if (!PermissionUtils.verifyPermissions(paramArrayOfInt)) {
                PermissionUtils.showMissingPermissionDialog(this);
            }
        }
    }

    /*
     * 初始化MQTT服务
     * */
    private void startMqttServer(){
        //初始化自定义WiseMqtt模块的配置,并开启长连接
        MqttManager.getInstance()
                .init(getApplication())
                .setServerIp(Api.IP_MQTT)                            //Ip
                .setServerPort(Api.PORT_MQTT)                        //port
                .connect();
    }

}