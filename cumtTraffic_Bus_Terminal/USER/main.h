#ifndef __MAIN_H
#define __MAIN_H
#include <stm32f10x.h>
#include "usart.h"
#include "stdio.h"
#include "stdlib.h"
#include "string.h"

#define u8 unsigned char



typedef struct    
{
 unsigned int Value;//充電值
 unsigned int lightValue1;//读取的ADC值
 unsigned int lightValue2;//主电源电压ADC值
 unsigned int powervalue;//主电源电压ADC值
 float VREFINT;//内部基准电压值
 float battdata;//电池电压
}Charge;



typedef struct    
{
 unsigned char status;//初始化状态
 unsigned char getstatus;//初始化状态
 unsigned char IMEI[16];//IMEI的值
 unsigned int sendcount;
 unsigned char datastatus;
 unsigned char tcpcount;
 unsigned char enablesend;//可以发送数据
 unsigned char csqcount;//可以发送数据
 unsigned char sendnum;//发送次数
 unsigned char sleepdata;
}GSM_init;







typedef struct    
{

 unsigned char Realtime[25];//当前时间
 unsigned char Qcell[25];//当前经纬度
 unsigned char GPS[50];//当前经纬度
 unsigned char senddata[200];
 unsigned char INdata[2];
 unsigned char count[6];
 unsigned char speed[10];
 unsigned char row[10]; 
 unsigned char tphi[20];
  unsigned char adcdata[20];
}GPS_DATA;

typedef struct    
{

 unsigned int Timeout;//当前时间
 unsigned int SetTimeout;//当前经纬度

}TIME_OUT;




#endif