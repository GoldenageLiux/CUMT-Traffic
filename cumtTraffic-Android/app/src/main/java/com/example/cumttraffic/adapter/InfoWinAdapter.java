package com.example.cumttraffic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.example.cumttraffic.R;
import com.example.cumttraffic.application.MyApplication;


/**
 * Created by Liuxin on 2021/5/9.
 * 地图上自定义的infowindow的适配器
 */
public class InfoWinAdapter implements AMap.InfoWindowAdapter{
    private Context mContext = MyApplication.getContext();
    private TextView nameTV;
    private String agentName;
    private TextView addrTV;
    private String snippet;

    @Override
    public View getInfoWindow(Marker marker) {
        initData(marker);
        View view = initView();
        return view;
    }
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    private void initData(Marker marker) {
        snippet = marker.getSnippet();
        agentName = marker.getTitle();
    }

    private View initView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_infowindow, null);
        nameTV = (TextView) view.findViewById(R.id.id);
        addrTV = (TextView) view.findViewById(R.id.addr);

        nameTV.setText(agentName);
        addrTV.setText(String.format(mContext.getString(R.string.agent_addr),snippet));

        return view;
    }

}
