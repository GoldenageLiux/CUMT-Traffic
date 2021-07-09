import paho.mqtt.client as mqtt
import time
import sys

def on_connect(client, userdata, flags, rc):
    print("Connected with result code " + str(rc))
def on_subscribe(client,userdata,mid,granted_qos):
    print("消息发送成功")
client = mqtt.Client(protocol=3)
client.username_pw_set("admin", "public")
client.on_connect = on_connect
client.on_subscribe = on_subscribe
client.connect(host="1.116.180.130", port = 1883, keepalive=60)  # 订阅频道
time.sleep(1)
i = 0

bus_line3 = open('Bus_03_Line.csv','r')
list3 = bus_line3.readlines()

for i in range(0,len(list3)):
    try:
        # 发布MQTT信息
        list3[i] = list3[i].rstrip('\n')
        sensor_data = "sendData," + list3[i]
        topic = "mqttBusPub"
        client.publish(topic, payload=sensor_data, qos=0)
        print(topic + " " + sensor_data)
        time.sleep(2)
    except KeyboardInterrupt:
        print("EXIT")
        client.disconnect()
        sys.exit(0)
