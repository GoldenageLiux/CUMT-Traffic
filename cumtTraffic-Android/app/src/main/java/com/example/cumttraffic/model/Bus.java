package com.example.cumttraffic.model;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.cumttraffic.R;

public class Bus {

    private String busId;
    private LatLng oldLatLng;
    private LatLng newLatLng;
    private Marker locMarker;
    private AMap aMap;

    public Bus(AMap aMap, String busId, LatLng oldLatLng, LatLng newLatLng) {
        this.aMap = aMap;
        this.busId = busId;
        this.oldLatLng = oldLatLng;
        this.newLatLng = newLatLng;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public LatLng getOldLatLng() {
        return oldLatLng;
    }

    public void setOldLatLng(LatLng oldLatLng) {
        this.oldLatLng = oldLatLng;
    }

    public LatLng getNewLatLng() {
        return newLatLng;
    }

    public void setNewLatLng(LatLng newLatLng) {
        this.newLatLng = newLatLng;
    }

    public Marker getLocMarker() {
        return locMarker;
    }

    public void setLocMarker(Marker locMarker) {
        this.locMarker = locMarker;
    }

    public void locationChanged () {
        if (locMarker == null) {
            addMarker();
        }
        moveLocationMarker();
    }

    /**
     * 平滑移动动画
     */
    private void moveLocationMarker() {
        final LatLng startnewLatLng  = oldLatLng;
        final LatLng endnewLatLng  = newLatLng;
        float rotate = getRotate(startnewLatLng, endnewLatLng);
        locMarker.setRotateAngle(360 - rotate + aMap.getCameraPosition().bearing);
        ValueAnimator anim = ValueAnimator.ofObject(new Bus.newLatLngEvaluator(), startnewLatLng, endnewLatLng);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                LatLng target = (LatLng) valueAnimator.getAnimatedValue();
                if (locMarker!= null)
                    locMarker.setPosition(target);
            }
        });
        anim.setDuration(1000);
        anim.start();
    }
    /**
     * 添加车辆marker
     */
    public void addMarker() {
        BitmapDescriptor des = BitmapDescriptorFactory.fromResource(R.drawable.bus_up_blue);
        locMarker = aMap.addMarker(new MarkerOptions().position(newLatLng).icon(des).title("校园巴士"+ busId +"号").snippet("中国矿业大学南湖校区"));
    }

    public class newLatLngEvaluator implements TypeEvaluator {
        @Override
        public Object evaluate(float fraction, Object startValue, Object endValue) {
            LatLng startnewLatLng = (LatLng) startValue;
            LatLng endnewLatLng = (LatLng) endValue;
            double x = startnewLatLng.latitude + fraction * (endnewLatLng.latitude - startnewLatLng.latitude);
            double y = startnewLatLng.longitude + fraction * (endnewLatLng.longitude - startnewLatLng.longitude);
            LatLng newLatLng = new LatLng(x, y);
            return newLatLng;
        }
    }

    /**
     * 根据经纬度计算需要偏转的角度
     *
     * @param curPos
     * @param nextPos
     * @return
     */
    private float getRotate(LatLng curPos, LatLng nextPos) {
        if(curPos==null||nextPos==null){
            return 0;
        }
        double x1 = curPos.latitude;
        double x2 = nextPos.latitude;
        double y1 = curPos.longitude;
        double y2 = nextPos.longitude;

        float rotate = (float) (Math.atan2(y2 - y1, x2 - x1) / Math.PI * 180);
        return rotate;
    }

    //改变车辆位置
    public void changeLoc(LatLng latLng) {
        oldLatLng = newLatLng;
        newLatLng = latLng;
    }
}
