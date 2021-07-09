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
 unsigned int Value;//���ֵ
 unsigned int lightValue1;//��ȡ��ADCֵ
 unsigned int lightValue2;//����Դ��ѹADCֵ
 unsigned int powervalue;//����Դ��ѹADCֵ
 float VREFINT;//�ڲ���׼��ѹֵ
 float battdata;//��ص�ѹ
}Charge;



typedef struct    
{
 unsigned char status;//��ʼ��״̬
 unsigned char getstatus;//��ʼ��״̬
 unsigned char IMEI[16];//IMEI��ֵ
 unsigned int sendcount;
 unsigned char datastatus;
 unsigned char tcpcount;
 unsigned char enablesend;//���Է�������
 unsigned char csqcount;//���Է�������
 unsigned char sendnum;//���ʹ���
 unsigned char sleepdata;
}GSM_init;







typedef struct    
{

 unsigned char Realtime[25];//��ǰʱ��
 unsigned char Qcell[25];//��ǰ��γ��
 unsigned char GPS[50];//��ǰ��γ��
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

 unsigned int Timeout;//��ǰʱ��
 unsigned int SetTimeout;//��ǰ��γ��

}TIME_OUT;




#endif