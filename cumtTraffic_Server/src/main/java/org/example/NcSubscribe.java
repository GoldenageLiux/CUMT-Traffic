package org.example;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.example.dao.BusDOMapper;
import org.example.dataobject.BusDO;
import org.example.error.BusinessException;
import org.example.interfaces.IShapListener;
import org.example.service.BusService;
import org.example.service.model.BusModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

import static org.example.App.shapMqtt;

/**
 * Created by liuxin on 2021/5/15
 */
@Component
public class NcSubscribe implements IShapListener {

    @Autowired
    private BusService busService;

    @Autowired
    private BusDOMapper busDOMapper;

    public static NcSubscribe ncSubscribe;

    /*
    *创建一个初始化方法
    *贴上@PostConstruct 标签
    *用于注入bean
    */
    @PostConstruct
    public void init(){
        ncSubscribe = this;
        ncSubscribe.busDOMapper = this.busDOMapper;
        ncSubscribe.busService = this.busService;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

        System.out.println("topic:"+topic);
        System.out.println("id:"+message.getId());
        System.out.println("qos:"+message.getQos());
        String payload = new String(message.getPayload());
        System.out.println("context:"+payload);
        parsePayload(topic, payload);
    }

    public void parsePayload(String topic, String payload) throws BusinessException {

        String[] str = payload.split(",");

        if(topic.equals("mqttBusPub")){
            if(str[0].equals("sendData")){
                //如果是巴士发来的坐标信息<sendData,buId,latitude,longitude>
                String busId = str[1];
                Double latitude = Double.valueOf(str[2]);
                Double longitude = Double.valueOf(str[3]);

                BusDO busDO = ncSubscribe.busDOMapper.selectByBusId(busId);
                BusModel busModel = new BusModel();
                busModel.setBusId(busId);
                busModel.setLatitude(latitude);
                busModel.setLongitude(longitude);

                //如果车辆不存在，则在数据库中新添加这个车辆
                if(busDO == null){
                    ncSubscribe.busService.addNewBus(busModel);
                }
                //如果当前车辆存在，则更新其坐标
                else{
                    busModel.setId(busDO.getId());
                    ncSubscribe.busService.updateBus(busModel);
                }
            }else if(str[0].equals("sendRecv")){
                //如果是巴士响应用户请求信息<sendRecv,busId,station>
                String busId = str[1];
                String toStation = str[2];
                Integer station = 0;
                if(toStation.length()>11){
                    station = Integer.valueOf(toStation.substring(11,toStation.length()));
                    UserCall.userCall.get(station).clear();
                }
                //回复该站点所有用户，巴士正在赶来
                shapMqtt.publish("mqttServerToUser",1,(busId+","+station).getBytes(),false);
            }

        }else if(topic.equals("mqttUserPub")){
            if(str[0].equals("userCall")){
                //用户发来的呼叫信息<userCall,userId,station>
                String userId = str[1];
                Integer station = Integer.valueOf(str[2]);

                if(!UserCall.userCall.containsKey(station)){
                    /*
                    如果此站是第一次有人呼叫，
                    则新定义一个set，并将此用户添加进set
                    然后将此set添加进map中
                    */
                    Set user = new HashSet();
                    user.add(userId);
                    UserCall.userCall.put(station,user);
                } else{
                    /*
                    利用set集合中元素不会重复的特性
                    如果是同一用户在同样站进行重复请求，则不会多次响应
                    否则将该站点该用户的请求加入
                    */
                    UserCall.userCall.get(station).add(userId);
                }

                if(UserCall.userCall.get(station).size()>0){
                    //当前站点请求数大于0时，向巴士发出呼叫请求
                    shapMqtt.publish("mqttServerToBus",1,("station:"+station+",userCount:"+UserCall.userCall.get(station).size()).getBytes(),false);
                }
            }
        }

    }
}
