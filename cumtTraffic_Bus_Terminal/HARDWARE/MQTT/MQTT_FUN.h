#ifndef __MQTT_FUN_H
#define __MQTT_FUN_H
#include "delay.h"
#include "usart.h"

//MQTT的相关数据存储
typedef struct    
{
		u8 mqtt_msg[400];
		u8 mqtt_strmsg[500];
		u8 len;
		u8 lenstr[10];
		u8 t_payload[300];
}MQTT_DATA;

//发送GPS数据到MQTT服务器
void Mqtt_SendGPSdata(u8 *t_payload, unsigned char *IMEI, float lat, float lon);

//发送回复请求信息到MQTT服务器
void Mqtt_SendRecvdata(u8 *t_payload,unsigned char *IMEI,char* station);

#endif
