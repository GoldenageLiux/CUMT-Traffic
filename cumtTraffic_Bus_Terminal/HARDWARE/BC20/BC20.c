#include "BC20.h"
#include "usart.h"
#include "delay.h"		
#include "callback.h"
#include "oled.h"
extern BC20_init BC20init;
char *getstrx;
extern  unsigned char RxCounter,RxBuffer[200];  
extern LongLati_data LongLatidata ;  //GPSģ��ľ�γ������ֵ
extern BC20_init BC20init;
/**********************************************************
����ļ�������Ҫ�����û���ģ���������ָ���ӡ�������Լ�����Ҫ��
����Լ���Ҫ��һЩ����ֵ��ֻ��Ҫ��������ɣ�����Ҫ�����ݲ�ѯ��
********************************************************/
/***********ī�Ӻſ����幺���ַ��
https://item.taobao.com/item.htm?spm=a1z10.3-c.w4023-3005174199.2.62e64f45YMjqsT&id=544366989645
***************/
void printf_CSQ(void)
{
		printf("AT+CESQ\r\n"); //��ѯ�ź�ǿ��
}

void printf_CLSTCP(void)
{
    printf("AT+QMTDISC=0\r\n");//�ر�����
    BC20init.status=5;
}

void printf_CPIN(void)
{
    printf("AT+CIMI\r\n"); //��ȡ����״̬
}

void printf_CGATT(void)
{
    printf("AT+CGATT=1\r\n");//�������磬PDP
    BC20init.status=2;//
}

void printf_GetCGATT(void)
{
    printf("AT+CGATT?\r\n");//��ȡ����״̬
}

void printf_CreSocket(void)
{
		printf("AT+QSOC=1,1,1\r\n");//����socket
}

void printf_ConTCP(void)
{
    BC20init.socektnum++;
    if(BC20init.socektnum>=20)
    {
        BC20init.socektnum=0;
        printf("AT+QMTOPEN=0,\"1.116.180.130\",1883\r\n");//��������TCP,����IP�Լ��������˿ں��� ,����ֱ���³���ʽ
    }
}

void printf_Sendata(u8*len,u8 *data)
{
		printf("AT+QISENDEX=0,%s,%s\r\n",len,data);//����ʮ���������ݷ�ʽ
}

void printf_MQTTCon(u8 *id)//����MQTT������
{
		printf("AT+QMTCONN=0,id\r\n");//��������
}

void printf_MQTTPub(u8 *topic,u8 *data)//����MQTT������Ϣ
{
		printf("AT+QMTPUB=0,0,0,0,%s,\"%s\"\r\n",topic,data);//������Ϣ
}

void printf_MQTTSub(u8 *topic)//����MQTT������Ϣ
{
    printf("AT+QMTSUB=0,1,%s,1\r\n",topic);//��������
}

/*************��ԭʼ���ݽ�������γ������*******************/	
void Getdata_Change(u8 *buffer)
{
		unsigned char i;
		char *strx;
		strx=strstr((const char*)buffer,(const char*)"A,");//����A��������γ�����ݱ���ȷ��ȡ��
		if(strx)
		{
			for(i=0;i<9;i++)
					LongLatidata.Latitude[i]=strx[i+2];
		  strx=strstr((const char*)buffer,(const char*)"N,");//����N�������ȡ��������
			if(strx)
			{
					for(i=0;i<10;i++)
						LongLatidata.Longitude[i]=strx[i+2];
				
					for(i=0;i<3;i++)
						LongLatidata.Longitudess[i]=LongLatidata.Longitude[i];
					for(i=3;i<10;i++)
						LongLatidata.Longitudedd[i-3]=LongLatidata.Longitude[i];
				
					LongLatidata.Truelongitude=(LongLatidata.Longitudess[0]-0x30)*100+(LongLatidata.Longitudess[1]-0x30)*10+(LongLatidata.Longitudess[2]-0x30)\
					+((LongLatidata.Longitudedd[0]-0x30)*10+(LongLatidata.Longitudedd[1]-0x30)+(float)(LongLatidata.Longitudedd[3]-0x30)/10+\
					(float)(LongLatidata.Longitudedd[4]-0x30)/100+(float)(LongLatidata.Longitudedd[5]-0x30)/1000+(float)(LongLatidata.Longitudedd[6]-0x30)/10000)/60.0;//��ȡ����������
					
					for(i=0;i<2;i++)
						LongLatidata.Latitudess[i]=LongLatidata.Latitude[i];
					for(i=2;i<9;i++)
						LongLatidata.Latitudedd[i-2]=LongLatidata.Latitude[i];	
					
					LongLatidata.TrueLatitude=(LongLatidata.Latitudess[0]-0x30)*10+(LongLatidata.Latitudess[1]-0x30)\
					+((LongLatidata.Latitudedd[0]-0x30)*10+(LongLatidata.Latitudedd[1]-0x30)+(float)(LongLatidata.Latitudedd[3]-0x30)/10+\
					(float)(LongLatidata.Latitudedd[4]-0x30)/100+(float)(LongLatidata.Latitudedd[5]-0x30)/1000+(float)(LongLatidata.Latitudedd[6]-0x30)/10000)/60.0;//��ȡ����������
			
				}
			
			}
}

