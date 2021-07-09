#include "mqtt_fun.h"
#include "string.h"
#include "stdlib.h"
#include "usart.h"

//发送GPS数据到MQTT服务器
void Mqtt_SendGPSdata(u8 *t_payload,unsigned char *IMEI,float lat,float lon)
{  
    char json[]="sendData,%s,%d.%06d,%d.%06d";
    char t_json[200];
    unsigned short json_len;
	  sprintf(t_json, json, IMEI, (unsigned int)lat, (unsigned int)((lat-(unsigned int )lat)*1000000), (unsigned int )lon, (unsigned int)((lon-(unsigned int )lon)*1000000));
    json_len = strlen(t_json)/sizeof(char);
  	memcpy(t_payload, t_json, json_len);
}

//发送回复请求信息到MQTT服务器
void Mqtt_SendRecvdata(u8 *t_payload,unsigned char *IMEI,char* station)
{  
    char json[]="sendRecv,%s,%s";
    char t_json[200];
    unsigned short json_len;
	  sprintf(t_json, json, IMEI,station);
    json_len = strlen(t_json)/sizeof(char);
  	memcpy(t_payload, t_json, json_len);
}

