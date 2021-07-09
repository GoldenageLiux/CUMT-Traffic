package com.example.cumttraffic.interfaces;

import java.io.IOException;

/**
 * @Desc Mqtt模块收到消息的回调
 */

public abstract class OnMqttAndroidConnectListener {
    public void connect() {

    }

    public void disConnect() {

    }

    public abstract void onDataReceive(String message) throws IOException;

    public void onConnectFail(String exception) {

    }
}
