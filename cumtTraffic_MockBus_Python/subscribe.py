import time

import paho.mqtt.client as mqtt
# The callback for when the client receives a CONNACK response from the server.
def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("连接成功")
        print("Connected with result code " + str(rc))

def on_message(client, userdata, msg):
    print(msg.topic + " " + str(msg.payload))
client = mqtt.Client(protocol=3)
client.username_pw_set("admin", "password")
client.on_connect = on_connect
client.on_message = on_message
client.connect(host="1.116.180.130", port = 1883, keepalive=60)  # 订阅频道
time.sleep(1)
# client.subscribe("public")
client.subscribe([("mqttdemo2", 0), ("test", 2)])
client.loop_forever()
