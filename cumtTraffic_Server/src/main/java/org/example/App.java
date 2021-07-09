package org.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello world!
 *
 */
@SpringBootApplication(scanBasePackages = {"org.example"})
@RestController
@MapperScan("org.example.dao")
public class App {

    public static ShapMqtt shapMqtt = new ShapMqtt("tcp://1.116.180.130:1883");

    @RequestMapping("/")
    public String home(){
        return  "Hello world!";
    }

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        //订阅车载硬件端主题，接收GPS信息
        shapMqtt.subscribe("mqttBusPub",new NcSubscribe());
        //订阅安卓用户主题，接收呼叫请求
        shapMqtt.subscribe("mqttUserPub",new NcSubscribe());

        SpringApplication.run(App.class);
    }
}
