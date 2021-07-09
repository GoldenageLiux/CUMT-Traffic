#include "led.h"
#include "delay.h"
#include "sys.h"
#include "usart.h"	 
#include "math.h"			
#include "stdio.h"
#include "stm32f10x_flash.h"
#include "stdlib.h"
#include "string.h"
#include "wdg.h"
#include "timer.h"
#include "BC20.h"
#include "sccom.h"
#include "callback.h"
#include "oled.h"
#include "mqtt_fun.h"
#include "dht11.h"
#include "exti.h"
/***************************
״̬�����룬�������whileѭ��
switch�����״̬��������ʵ��������ơ�
�趨10msɨ��һ��ѭ�������������10�������û�����ʹ�ã��û����Ը��ݲ�ͬ���������趨�Լ���Ҫ�Ĺ��ܡ�
ÿ�����ܿ��Ի������ţ�Ҳ�����໥�������ݽ�����
���ڴ���ͨѶ���˴����õ���DMA��ʽ����ȡBC26���ص�����ֵ��ͨ��ī�ӺſƼ�ʵ�ֵ�ר����������ÿһ֡���ݽ���ʶ��
�Ӷ���֤ÿһ֡���ݵķ����ж϶�����֡������֤�����ݿɿ��Ե��ж����ϱ����ϲ��û��������ݵ��á�
*****************************/
/******
ATCMD_Table����������洢ģ��᷵�ص�һЩ״ֵ̬���Լ�������ȡ�ͨ��ȥ�жϷ��ص�������Ӷ��ж�ģ���״̬�Լ�Я���ص���Ч������Ϣ��
ͨ�����ǵ�ScCom.lib����ĺ�������Щ�����н��д���������Ч����Ϣ�ϴ����û��㣬�û��Ͳ���Ҫȥ���������ʱ���أ�ֻҪ�ȴ��ص���������
�����ϴ����ɡ��ڶ�Ӧ�Ľӿڼ����Լ���Ҫ�Ĵ��������ɣ����������ʹ��Ч�ʡ�
��������д��������ʾ:
$��һλ������ŵ����ݽ�β������OK\r\nλ��β�ģ��򵥵㿴����������AT+CPIN? ���ص�һ����+CPIN: READY OK\r\n��������OK\r\n�������
������һЩָ����ǲ���OK\r\n�������ġ��򵥵㽲���������Ƿ�AT+QISEND֮�������ص���>,һ��Ҫ�����>֮���������Ӧ�����ݡ�������ֻ��һ��
���ֽڵ�һ���ַ��������κεĽ�������������������ʹ��@��ͷ��Ϊ�������ﷵ��ָ�����OK\r\n�������������������
���ǽ���Ӧ�����ݲ��䵽��������鵱�У�@��$���Ի��ҷ��ã�����Ҫ@��$�ֿ����ã��û����Ը����Լ�����Ҫ����Ӻ����ָ���ʽ���Ӷ�ȥ�жϷ���ֵ��
******/
const char ATCMD_Table[] = "$46011$46004$+CGATT: 1$+CESQ@+QMTOPEN: 0,0@+QMTCONN: 0,0,0@+QMTSUB: 0,1,0,1@+QMTPUB: 0,0,0$+QMTRECV:$order$GNRMC";
//$������\r\n,@��û��\r\n���ص�

MQTT_DATA mqtt_data;
BC20_init BC20init;
LongLati_data LongLatidata;
S_Comhd m_com;
char P1msReq;
char *strx=0,*Readystrx,*Errstrx ; 	//����ֵָ���ж�
extern char  RxBuffer[200],RxCounter;
unsigned char sendok=1,tcprec;
unsigned char LongLatistr[50];
extern DHT11 DHT11_DATA;
extern EXTIPORT EXTIstatus;
void Per1msTask(void);
void Get_IMEI(void);
void OLED_SHOW(void);
void OLED_SHOWGPS(void);
void OLED_SHOWDHT11(void);
void temphumi_show(void);

extern char station[50];

const char* PUBTOPIC = "mqttBusPub";
const char* SUBTOPIC = "mqttServerToBus";

u8 temp,humi;

void Clear_Buffer(void)
{
		u8 i;
		RxCounter=0;
		for(i=0;i<100;i++)
				RxBuffer[i]=0;//����
		IWDG_Feed();//ι��
}

//��ȡIMEIֵ,IMEI��ΪΨһID��¼������
void Get_IMEI(void)
{
		unsigned char i;
		Clear_Buffer();
		printf("AT+CGSN=1\r\n");//��ȡIMEI
		delay_ms(300);	
		strx=strstr((const char*)RxBuffer,(const char*)"+CGSN:");//����+CGSN:
		while(strx==NULL)
		{
				Clear_Buffer();
				printf("AT+CGSN=1\r\n");//��ȡIMEI
				delay_ms(300);	
				strx=strstr((const char*)RxBuffer,(const char*)"+CGSN:");//����+CGSN:
		}	
		usart1_sendstr((u8*)RxBuffer);//ͨ������1���Դ�ӡ��IMEI
		
		for(i=0;i<15;i++)
		{
				BC20init.IMEI[i]=strx[i+7];//�洢IMEI		
		}
		
		//����������������
		memcpy(BC20init.pubtopic,PUBTOPIC,strlen(PUBTOPIC));
		//����������������
		memcpy(BC20init.subtopic,SUBTOPIC,strlen(SUBTOPIC));

}

//��BC20ģ����п�������
void OPEN_BC20(void)
{
    printf("AT\r\n"); 
    delay_ms(300);
    strx=strstr((const char*)RxBuffer,(const char*)"OK");//����OK
    printf("AT\r\n"); 
    delay_ms(300);
    strx=strstr((const char*)RxBuffer,(const char*)"OK");//����OK
		IWDG_Feed();//ι��
		/*
		RESET=1;
		delay_ms(300);
		delay_ms(300);
		RESET=0;
    */
		if(strx==NULL)
		{
        PWRKEY=1;//����
        delay_ms(300);
        delay_ms(300);
        delay_ms(300);
        delay_ms(300);	
        PWRKEY=0;//������������
        IWDG_Feed();//ι��
		}
		
    printf("AT\r\n"); 
    delay_ms(300);
    IWDG_Feed();//ι��
    strx=strstr((const char*)RxBuffer,(const char*)"OK");//����OK
		
		while(strx==NULL)
    {
        Clear_Buffer();	
        printf("AT\r\n"); 
        delay_ms(300);
        LED1=!LED1;
        strx=strstr((const char*)RxBuffer,(const char*)"OK");//����OK
    }
		
		LED1=1;
		LED2=1;
		
		printf("ATe0&w\r\n"); 
		delay_ms(300);
		printf("AT+CFUN=1\r\n"); 
		delay_ms(300);
		//printf("AT+QGNSSAGPS=1\r\n"); //����AGPS����
		//delay_ms(300);
		printf("AT+QGNSSC=1\r\n");//��GNSS�ϵ�
		delay_ms(300);
		printf("AT+QMTCLOSE=0\r\n");//�ر�����
		delay_ms(300);
		IWDG_Feed();//ι��
}

void Per1msTask(void)
{
		static u8 P10msReq = 0;
		if (P10msReq < 25) 
				P10msReq++;//250msһ���¼�
		else 
				P10msReq = 0;  
		switch(P10msReq)
		{
				case 0:
						switch(BC20init.status)//���豸���г�ʼ��
						{
								case 0:  printf_CPIN();usart1_sendstr((u8*)"��ѯ����״̬��\r\n");break;//��ȡ�ֻ���
								case 1:  printf_CGATT();usart1_sendstr((u8*)"ע������\r\n");break;//ȥע������
								case 2:  printf_GetCGATT();usart1_sendstr((u8*)"��ȡע�����\r\n");break; //��ȡע��״̬     
								case 3:  printf_CSQ();break; //��ȡע��״̬  
								case 4:  printf_CLSTCP();break; //�ر���һ������  							
						}
						break;
						
				case 1:
								SCOM_RecieveAT(&m_com);//�����ݲ���ɨ�����
						break;
				
				case 2:
						switch(BC20init.status)
						{
								case 5: printf_ConTCP();usart1_sendstr((u8*)"����MQTT������ TCPģʽ\r\n");break;//���ӷ�����
						}                        
						break;
						
				case 3:
						if (BC20init.status==6)//���ӷ������ɹ���׼��������
						{
								BC20init.tcpcount++;
								if(BC20init.tcpcount>=5)//�������ݰ�׼����������
								{
								
										printf_MQTTCon(BC20init.IMEI);//ʵ��MQTT��¼������
										usart1_sendstr((u8*)"��¼MQTT������,����MQTT��ʽ\r\n");
										BC20init.tcpcount=0;
										BC20init.status=0x0f;//����
								}
						}
						break;
						
				case 4://�������ݵ�MQTT������
							if(BC20init.status==0x08)
							{
									BC20init.tcpcount++;
									if(	BC20init.tcpcount>=3)//�������ݰ�׼����������
									{				
											Mqtt_SendGPSdata(mqtt_data.t_payload,BC20init.IMEI,LongLatidata.TrueLatitude,LongLatidata.Truelongitude);//��װ����
											printf_MQTTPub(BC20init.pubtopic,mqtt_data.t_payload);//����������Ϣ
											usart1_sendstr((u8*)"������Ϣ��MQTT������\r\n");
											BC20init.tcpcount=0;
											BC20init.status=0x0f;//����
									}
							}
							break;
							
				case 5:
							if(BC20init.status==0xFF)//�ȴ����ӷ�����
									LED1=!LED1;
							break;	
							
				case 6://�Ի�ȡ����GPS���ݽ��н���
						if(BC20init.status==7)//��¼�������ɹ���׼���ɼ�GPS����
						{
								printf("AT+QGNSSRD=\"NMEA/RMC\"\r\n");
						}
						break;
						
				case 7:
							DHT11_Read_Data(&DHT11_DATA.temp,&DHT11_DATA.humi);
						break;
				
				case 8:
						if(BC20init.status==8&&BC20init.subflag==0)//֧�ֶ����뷢��
						{
								BC20init.subcount++;
								if(BC20init.subcount>=10)
								{
										printf_MQTTSub(BC20init.subtopic);//��������
										usart1_sendstr((u8*)"������Ϣ��MQTT������\r\n");
										BC20init.subcount=0;
								}
						}
						break;
						
				case 9:
						if(BC20init.status==0x0f)//
						{
								BC20init.timeout++;
								if(BC20init.timeout>=200)//��������
								{
									while(1);
								}
						}
						else
								BC20init.timeout=0;
						IWDG_Feed();
						break;	
						
				default:
						break;
		}
}

void OLED_SHOW(void)
{
	  OLED_ShowChinese(9+18,0,0);//ī�ӺſƼ�
		OLED_ShowChinese(9+36,0,1);
		OLED_ShowChinese(9+54,0,2);
		OLED_ShowChinese(9+72,0,3);
		OLED_ShowChinese(9+90,0,4);
		OLED_ShowString(30,3,(u8*)"Power on",16);
		
}

void OLED_SHOWGPS(void)
{
	  OLED_ShowChinese(0,0,5);//��γ��
		OLED_ShowChinese(0+18,0,7);
	  OLED_ShowString(0+36,0,(u8*)":000.000000",16);
		OLED_ShowChinese(0,3,6);
		OLED_ShowChinese(0+18,3,7);
		OLED_ShowString(0+36,3,(u8*)":00.000000",16);
}

void OLED_SHOWDHT11(void)
{
	  OLED_ShowChinese(0,0,8);//��ʪ��
		OLED_ShowChinese(0+18,0,7);
	  OLED_ShowString(0+36,0,(u8*)":00C",16);
		OLED_ShowChinese(0,3,9);
		OLED_ShowChinese(0+18,3,7);
		OLED_ShowString(0+36,3,(u8*)":00%",16);
}

void temphumi_show(void){
		u8 showstr[5];
		u8 sendata[]="727394ACB8221234";
		DHT11_Read_Data(&temp,&humi); //��ȡ��ʪ������ֵ
		showstr[0]=':';
		
		sendata[12]=temp/10+0x30;
		showstr[1]=sendata[12];
		sendata[13]=temp%10+0x30;
		showstr[2]=sendata[13];
		showstr[3]=0;
		OLED_ShowString(0+36,0,showstr,16);//OLED��ʾ�¶�ֵ
	
		sendata[14]=humi/10+0x30;
		showstr[1]=sendata[14];
		sendata[15]=humi%10+0x30;//ת���ַ���ʽ
		showstr[2]=sendata[15];
		OLED_ShowString(0+36,3,showstr,16);//OLED��ʾʪ��ֵ
}

void mcu_init(void)
{   
		delay_init();	    	 //��ʱ������ʼ��	  
		delay_ms(1000);
		LED_Init();		  		//��ʼ����LED���ӵ�Ӳ���ӿ�
		BC20CTR_Init();			//BC20�˿ڳ�ʼ��
		
		uart_init(115200);//����1��ʼ����������PC���д�ӡģ�鷵������
		uart2_init(115200);//��ʼ����BC20���Ӵ���	
		
		while(DHT11_Init()){};//���DHT11�Ƿ����
			
		OLED_Init();			 //��ʼ��OLED  
		OLED_Clear(); 
	  OLED_SHOW();      //����ҳ��
			
    IWDG_Init(7,625);    //���Ź�8Sһ��
    OPEN_BC20();				//��ģ�鿪��
			
		EXTI_Config();
		
		Get_IMEI();					//��ȡ���豸IMEI
    SCOM_Init(&m_com, 0, COM_Rx_Callback, COM_Get_Callback);//�ص�������ʼ�������˺�����ֵ��ȡDMA����ֵ�����Է���ֵ���д���
    SCOM_SetTable(&m_com, ATCMD_Table, "OK\r\n");//����������ֵ�����ڲ�����ֵ
		USART2_DMAInit();	//����2����DMA�ķ�ʽ
    TIM3_Int_Init(199,7199);//20ms�жϣ�Ϊ״̬���ṩ��׼ʱ��
			
		OLED_Clear();     //�������ҳ��
	  //OLED_SHOWGPS();   //��ʾ��γ����Ϣ
		OLED_SHOWDHT11();   //��ʾ��ʪ����Ϣ
		temphumi_show();
}

int main(void)
{	
		mcu_init();//��Ƭ����ʼ��
		while(1)
		{	
				if(EXTIstatus.flag)//�������±�����,��ʾ���յ�����
				{
						EXTIstatus.flag=0; 
						LED1=1;
						LED2=1;
						OLED_Clear();
						OLED_SHOWDHT11();   //��ʾ��ʪ����Ϣ
						temphumi_show();
						memset(mqtt_data.t_payload, 0, sizeof(mqtt_data.t_payload));
						Mqtt_SendRecvdata(mqtt_data.t_payload,BC20init.IMEI,station);//��װ����
						printf_MQTTPub(BC20init.pubtopic, mqtt_data.t_payload);
				}
				IWDG_Feed();//ι��
				
				if (P1msReq)//10msɨ��һ��
				{
						P1msReq = 0;
						Per1msTask();
						IWDG_Feed();//ι��
				}
		}
}


