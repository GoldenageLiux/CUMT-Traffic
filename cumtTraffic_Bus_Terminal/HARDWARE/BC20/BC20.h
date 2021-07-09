#ifndef __BC20_H
#define __BC20_H
#include <stm32f10x.h>

#include "usart.h"
#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include "timer.h"

typedef struct    
{
	unsigned char status;//
	unsigned char IMEI[20];//IMEI
	unsigned char ipstatus;
	unsigned char tcpstaus;
	int sendcount;
	unsigned char csq;
	unsigned char csqstr[10];
	unsigned char socektnum;   
	unsigned char tcpcount;//TCP连接基数
	unsigned int timeout;
	unsigned char pubtopic[50];
	unsigned char subtopic[50];
	unsigned char subflag;//订阅消息标志位
  unsigned char subcount;//订阅
}BC20_init;
void printf_CSQ(void);
void printf_CPIN(void);
void printf_CREG(void);
void printf_CGREG(void);
void printf_CloseTCP(void);
void printf_IPstatus(void);
void printf_qideact(void);
void printf_qifgnct(void);
void printf_apn(void);
void printf_qiact(void);
void printf_qiregapp(void);
void printf_qimode(void);
void printf_qiopen(void);
void Get_CPIN(void);
void Get_CREG(void);
void Get_CGREG(void);
void Get_CloseTCP(void);
void Get_IPstatus(void);
void Get_qideact(void);
void Get_qifgnct(void);
void Get_apn(void);
void Get_qiact(void);
void Get_qistat(void);
void Get_qifgcnt(void);
void Get_qiregapp(void);
void Get_Connected(void);
void Get_qimode(void);
void printf_sendstr(void);
void printf_sendata(char *strdata);
void change_data(char *data);
void Get_CCLK(void);
void change_data(char *data);
void printf_CLSTCP(void);
void Printf_VTG(void);
void printf_CGATT(void);
void printf_GetCGATT(void);
void printf_CreSocket(void);
void printf_ConTCP(void);
void printf_Sendata(u8*len,u8 *data);
void printf_MQTTCon(u8 *id);
void printf_MQTTPub(u8 *topic,u8 *data);
void printf_MQTTSub(u8 *topic);
#endif

