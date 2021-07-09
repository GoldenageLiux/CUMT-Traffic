package com.example.cumttraffic.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.cumttraffic.R;
import com.example.cumttraffic.activity.BaseActivity;
import com.example.cumttraffic.activity.MainActivity;
import com.example.cumttraffic.adapter.InfoWinAdapter;
import com.example.cumttraffic.application.MyApplication;
import com.example.cumttraffic.constant.Api;
import com.example.cumttraffic.constant.NetConstant;
import com.example.cumttraffic.interfaces.OnMqttAndroidConnectListener;
import com.example.cumttraffic.model.Bus;
import com.example.cumttraffic.mqtt.MqttManager;
import com.example.cumttraffic.utils.LocOverlay;
import com.example.cumttraffic.utils.ModelPreference;
import com.example.cumttraffic.utils.PermissionUtils;
import com.example.cumttraffic.utils.TraceAsset;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MapFragment extends Fragment implements AMap.OnMapClickListener, AMap.OnMapLoadedListener, AMap.OnMarkerClickListener, LocationSource, AMapLocationListener {

    private MapView mMapView;
    private AMap aMap;
    private AMapLocationClient mLocationClient;
    private AMapLocationClientOption mLocationOption;
    private LatLng mylocation;
    private LocOverlay mylocationoverlay;
    private InfoWinAdapter adapter;
    private Marker currentMarker;
    private boolean infoWindowShown = false;
    private Bus callBus;
    private CardView mqtt_send;

    private BaseActivity baseActivity;

    //所有车站的经纬度坐标
    private List<LatLng> mOriginList = new ArrayList<>();
    //所有巴士对象的集合
    private List<Bus> busList = new ArrayList<>();
    //所有巴士ID的集合
    private List<String> busIds = new ArrayList<>();
    /*
    所有车站的Marker集合
    因为车站是静态对象，只需要绘制一次，所以将其坐标与Marker分离开
    */
    private List<Marker> busStation = new ArrayList<>();
    //打印日志信息的TAG
    private final String TAG = "MapActivity";
    //定时器
    private Timer timerQuery;
    //我呼叫的站台
    private Integer myCallStation = -1;
    //监听前来巴士与自己距离的子线程
    private Thread listenCalThread;
    //是否当前Marker的infoWindow被捕捉到
    private boolean hasRecognize = false;


    /**
     * 需要进行检测的权限数组
     */
    private String[] needPermissions = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //获取地图控件引用
        mMapView = getActivity().findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        baseActivity = new BaseActivity();
        initlocation();
        initMap();
        mqttExchange();
        timerQuery = new Timer();
        //每1000毫秒执行一次
        timerQuery.schedule(asyncUpdateBus, 0, 2000);
    }

    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
        //执行mLocationClient.onDestroy()，暂停定位监听
        if(null != mLocationClient){
            mLocationClient.stopLocation();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        //执行mLocationClient.onDestroy()，销毁定位监听
        if(null != mLocationClient){
            mLocationClient.onDestroy();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void activate(LocationSource.OnLocationChangedListener onLocationChangedListener) {
        if (PermissionUtils.checkPermissions(getActivity(), needPermissions)) {
            mLocationClient.startLocation();
        } else {
            PermissionUtils.requestPermissions(getActivity(), needPermissions);
        }
        if (mylocation != null)
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mylocation, 17));
    }

    @Override
    public void deactivate() {
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    //初始化地图
    private void initMap() {
        mqtt_send = getActivity().findViewById(R.id.mqttsend);
        if (aMap == null) {
            aMap = mMapView.getMap();
            aMap.setOnMapClickListener(this);
        }
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);
        aMap.setOnMapLoadedListener(this);//设置设置aMap加载成功事件监听器
        mylocationoverlay = new LocOverlay(aMap);//自定义定位overlay

        //自定义InfoWindow
        aMap.setOnMarkerClickListener(this);
        adapter = new InfoWinAdapter();
        aMap.setInfoWindowAdapter(adapter);

    }

    //初始化定位
    private void initlocation() {
        mLocationClient = new AMapLocationClient(MyApplication.getContext());
        mLocationOption = new AMapLocationClientOption();
        // 设置定位监听
        mLocationClient.setLocationListener(this);
        // 设置为高精度定位模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置为连续定位
        mLocationOption.setOnceLocation(false);
        mLocationOption.setMockEnable(true);
        // 设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    /**
     * 定位回调方法
     *
     * @param aMapLocation 定位结果类。
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                mylocation = new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude());
                mylocationoverlay.locationChanged(aMapLocation);//设置定位图标、精度圈以及移动效果
            } else {
                String errText = "定位失败:" + aMapLocation.getErrorCode() + ": "
                        + aMapLocation.getErrorInfo();
                Toast.makeText(getActivity(), errText, Toast.LENGTH_SHORT).show();
                if (aMapLocation.getErrorCode() == 12 && !hasRecognize) {
                    hasRecognize = true;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("提示：打开GPS开关定位会更加准确")
                            .setMessage("是否进入设置页选择打开GPS定位服务")
                            .setPositiveButton("进入", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                        getActivity().startActivity(myIntent);
                                    } catch (ActivityNotFoundException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getActivity(), "无法开启定位设置页面", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            }
           // Log.e("坐标信息","lat:"+String.valueOf(aMapLocation.getLatitude())+" lng:"+String.valueOf(aMapLocation.getLongitude()));
        }
    }

    @Override
    public void onMapLoaded() {
        //读取本地轨迹数据，添加轨迹标记点
        mOriginList = TraceAsset.parseLocationsData(getActivity().getAssets(),
                "Bus_Line.csv");

        //移动地图可视区域到起始点
        if (mOriginList != null && mOriginList.size()>0) {
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mOriginList.get(0),16));
        }

        //地图上添加标记点
        for(int i = 0; i < mOriginList.size(); i++){
            BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE);
            Marker marker = aMap.addMarker(new MarkerOptions().position(mOriginList.get(i)).icon(icon).title("校园巴士"+i+"号站点").snippet("中国矿业大学南湖校区"));
            marker.setObject(i);
            busStation.add(marker);
        }
    }

    //marker的点击事件
    @Override
    public boolean onMarkerClick(Marker marker) {
        currentMarker = marker;
        infoWindowShown = false;
        return false; //返回 “false”，除定义的操作之外，默认操作也将会被执行
    }

    //地图的点击事件
    @Override
    public void onMapClick(LatLng latLng) {
        //点击地图上没marker 的地方，隐藏infowindow
        if (currentMarker.isInfoWindowShown() && !infoWindowShown) {
            infoWindowShown = true;
            return;
        }
        if (currentMarker.isInfoWindowShown() && infoWindowShown){
            currentMarker.hideInfoWindow();//这个是隐藏infowindow窗口的方法
        }
    }

    //mqtt通信，接收Mqtt回调信息，并通过点击按钮实现Mqtt发布消息
    private void mqttExchange() {
        MqttManager.getInstance().regeisterServerMsg(new OnMqttAndroidConnectListener() {
            @Override
            public void onDataReceive(String message) throws IOException {
                Log.e(TAG,message);
                String[] str = message.split(",");
                String BusId = str[0];
                Integer ResponseStation = Integer.valueOf(str[1]);
                if(ResponseStation == myCallStation){
                    for (Bus bus : busList) {
                        if (BusId.equals(bus.getBusId())) {
                            Toast.makeText(getActivity(), "车辆正在往"+myCallStation+"号站点赶来", Toast.LENGTH_LONG).show();
                            //成功接收呼叫请求的车辆变色
                            callBus = bus;
                            BitmapDescriptor des = BitmapDescriptorFactory.fromResource(R.drawable.bus_up_change);
                            Marker busMarker = bus.getLocMarker();
                            busMarker.setIcon(des);
                            bus.setLocMarker(busMarker);
                            //有车辆前来，开始监听他的距离
                            initListen();
                        }
                    }
                }
            }
        });

        mqtt_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int station = getNearstStation();
                String userTel = ModelPreference.getString(getActivity(),"telphone","");
                MqttManager.getInstance().sendMsg(Api.PUB_TOPIC, "userCall,"+userTel+","+station);
                myCallStation = station;
                Toast.makeText(getActivity(), "你正在"+station+"号站点附近呼叫车辆", Toast.LENGTH_LONG).show();
            }
        });
    }

    /*
     okhttp异步POST请求 要求API level 21+
     实现每隔1s请求一次所有车辆位置
   */
    private TimerTask asyncUpdateBus = new TimerTask() {
        @Override
        public void run() {
            // 1、初始化okhttpClient对象
            OkHttpClient okHttpClient = new OkHttpClient();
            // 2、构建请求体requestBody
            RequestBody requestBody = new FormBody.Builder()
                    .build();
            // 3、发送请求
            Request request = new Request.Builder()
                    .url(NetConstant.getAllBusURL())
                    .post(requestBody)
                    .build();
            // 4、使用okhttpClient对象获取请求的回调方法，enqueue()方法代表异步执行
            okHttpClient.newCall(request).enqueue(new Callback() {

                // 5、重写两个回调方法
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d(TAG, "请求URL失败： " + e.getMessage());
                    baseActivity.showToastInThread(getActivity(),"请求URL失败" );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // 先判断一下服务器是否异常
                    String responseStr = response.toString();
                    if (responseStr.contains("200")) {
                        String responseBodyStr = response.body().string();
                        JsonObject responseBodyJSONObject = (JsonObject) new JsonParser().parse(responseBodyStr);
                        if (getStatus(responseBodyJSONObject).equals("success")) {
                            parseBusLatLng(getActivity(), responseBodyJSONObject);
                        } else {
                            getResponseErrMsg(getActivity(), responseBodyJSONObject);
                        }
                    } else {
                        Log.d(TAG, "服务器异常");
                        baseActivity.showToastInThread(getActivity(), responseStr);
                    }
                }
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private String getStatus(JsonObject responseBodyJSONObject) {
        String status = responseBodyJSONObject.get("status").getAsString();
        return status;
    }

    private void getResponseErrMsg(Context context, JsonObject responseBodyJSONObject) {
        String errorCode = responseBodyJSONObject.get("data").getAsString();
        String errorMsg = responseBodyJSONObject.get("status").getAsString();
        Log.d(TAG, "errorCode: " + errorCode + " errorMsg: " + errorMsg);
    }

    //计算得到离自己当前位置最近的站点
    private int getNearstStation(){
        int stationCode = -1;
        float minDistance = Integer.MAX_VALUE;
        for(int i = 0; i<busStation.size()-1;i++){
            float distance = AMapUtils.calculateLineDistance(busStation.get(i).getPosition(),mylocation);
            if(distance < minDistance){
                minDistance = distance;
                stationCode = i;
            }
        }
        return stationCode;
    }

    //计算前来车辆与自己呼叫车站的距离
    private void calcDistance(){
        LatLng latLng = callBus.getLocMarker().getPosition();
        float distance = AMapUtils.calculateLineDistance(latLng,busStation.get(myCallStation).getPosition());
        //距离小于5米时，车辆恢复原来的颜色
        if(distance<10){
            stopListen();
            BitmapDescriptor des = BitmapDescriptorFactory.fromResource(R.drawable.bus_up_blue);
            Marker busMarker = callBus.getLocMarker();
            busMarker.setIcon(des);
            callBus.setLocMarker(busMarker);
        }
    }

    //解析所有车辆的坐标信息
    private void parseBusLatLng(Context context, JsonObject responseBodyJSONObject) {
        JsonArray allBusMes = responseBodyJSONObject.get("data").getAsJsonArray();

        for(int i = 0; i < allBusMes.size(); i++){
            String bus_id = allBusMes.get(i).getAsJsonObject().get("bus_id").getAsString();
            Double latitude = Double.valueOf(allBusMes.get(i).getAsJsonObject().get("latitude").toString());
            Double longitude = Double.valueOf(allBusMes.get(i).getAsJsonObject().get("longitude").toString());

            LatLng latLng = new LatLng(latitude,longitude);

            //进行坐标转换
            /* WGS84坐标系	地球坐标系，国际通用坐标系
             ** GCJ02坐标系	火星坐标系，WGS84坐标系加密后的坐标系；Google国内地图、高德、QQ地图 使用
             ** BD09坐标系	百度坐标系，GCJ02坐标系加密后的坐标系
             */
            /*CoordinateConverter converter = new CoordinateConverter(getActivity());
            converter.from(CoordinateConverter.CoordType.GPS);
            converter.coord(latLng);
            latLng = converter.convert();*/
            if(latLng!=null){
                //如果校车列表中已经存在此校车，则直接进行位置移动
                if (busIds.contains(bus_id)) {
                    for (Bus bus : busList) {
                        if (bus_id.equals(bus.getBusId())) {
                            bus.changeLoc(latLng);
                            changeBusThread(getActivity(),bus);
                        }
                    }
                }
                //否则新创建该校车
                else {
                    busIds.add(bus_id);
                    Bus newBus = new Bus(aMap, bus_id, latLng, latLng);
                    newBus.addMarker();
                    busList.add(newBus);
                }
            }
        }
    }

    // 实现在子线程中移动Bus
    private void changeBusThread(Context context, Bus bus) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bus.locationChanged();
            }
        });
    }

    //开启监听距离线程
    private void initListen() {
        listenCalThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //当线程未结束时，一直监听距离
                while (!listenCalThread.isInterrupted()) {
                    calcDistance();
                }
            }
        });
        listenCalThread.start();
    }

    //中止线程
    private void stopListen()
    {
        listenCalThread.interrupt();
    }


}
