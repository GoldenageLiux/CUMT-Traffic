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
状态机代码，代码采用while循环
switch的语句状态机方法来实现整个设计。
设定10ms扫描一次循环，这里分配了10个任务供用户进行使用，用户可以根据不同的任务来设定自己需要的功能。
每个功能可以互不干扰，也可以相互进行数据交互。
对于串口通讯，此处采用的是DMA方式来获取BC26返回的数据值，通过墨子号科技实现的专利代码来对每一帧数据进行识别，
从而保证每一帧数据的返回判断都不丢帧，并保证对数据可靠性的判读与上报给上层用户进行数据调用。
*****************************/
/******
ATCMD_Table数组就是来存储模块会返回的一些状态值，以及命令符等。通过去判断返回的命令符从而判断模块的状态以及携带回的有效数据信息。
通过我们的ScCom.lib里面的函数对这些命令行进行处理，并将有效的信息上传给用户层，用户就不需要去关心命令何时返回，只要等待回调函数进行
数据上传即可。在对应的接口加入自己需要的处理函数即可，将大大提升使用效率。
对于命令写法如下所示:
$第一位后面跟着的数据结尾都是以OK\r\n位结尾的，简单点看，比如输入AT+CPIN? 返回的一定是+CPIN: READY OK\r\n。后面会带OK\r\n结束词语。
对于有一些指令返回是不带OK\r\n结束符的。简单点讲，比如我们发AT+QISEND之后，他返回的是>,一般要求出现>之后再输入对应的数据。但是他只是一个
单字节的一个字符，不带任何的结束符，所以这里我们使用@开头作为我们这里返回指令不含有OK\r\n结束符的语句来操作。
我们将对应的数据补充到下面的数组当中，@和$可以混乱放置，不需要@和$分开放置，用户可以根据自己的需要，添加合理的指令格式，从而去判断返回值。
******/
const char ATCMD_Table[] = "$46011$46004$+CGATT: 1$+CESQ@+QMTOPEN: 0,0@+QMTCONN: 0,0,0@+QMTSUB: 0,1,0,1@+QMTPUB: 0,0,0$+QMTRECV:$order$GNRMC";
//$代表有\r\n,@是没有\r\n返回的

MQTT_DATA mqtt_data;
BC20_init BC20init;
LongLati_data LongLatidata;
S_Comhd m_com;
char P1msReq;
char *strx=0,*Readystrx,*Errstrx ; 	//返回值指针判断
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
				RxBuffer[i]=0;//缓存
		IWDG_Feed();//喂狗
}

//获取IMEI值,IMEI作为唯一ID登录服务器
void Get_IMEI(void)
{
		unsigned char i;
		Clear_Buffer();
		printf("AT+CGSN=1\r\n");//获取IMEI
		delay_ms(300);	
		strx=strstr((const char*)RxBuffer,(const char*)"+CGSN:");//返回+CGSN:
		while(strx==NULL)
		{
				Clear_Buffer();
				printf("AT+CGSN=1\r\n");//获取IMEI
				delay_ms(300);	
				strx=strstr((const char*)RxBuffer,(const char*)"+CGSN:");//返回+CGSN:
		}	
		usart1_sendstr((u8*)RxBuffer);//通过串口1可以打印出IMEI
		
		for(i=0;i<15;i++)
		{
				BC20init.IMEI[i]=strx[i+7];//存储IMEI		
		}
		
		//发布数据主题名称
		memcpy(BC20init.pubtopic,PUBTOPIC,strlen(PUBTOPIC));
		//订阅数据主题名称
		memcpy(BC20init.subtopic,SUBTOPIC,strlen(SUBTOPIC));

}

//对BC20模块进行开机操作
void OPEN_BC20(void)
{
    printf("AT\r\n"); 
    delay_ms(300);
    strx=strstr((const char*)RxBuffer,(const char*)"OK");//返回OK
    printf("AT\r\n"); 
    delay_ms(300);
    strx=strstr((const char*)RxBuffer,(const char*)"OK");//返回OK
		IWDG_Feed();//喂狗
		/*
		RESET=1;
		delay_ms(300);
		delay_ms(300);
		RESET=0;
    */
		if(strx==NULL)
		{
        PWRKEY=1;//拉低
        delay_ms(300);
        delay_ms(300);
        delay_ms(300);
        delay_ms(300);	
        PWRKEY=0;//拉高正常开机
        IWDG_Feed();//喂狗
		}
		
    printf("AT\r\n"); 
    delay_ms(300);
    IWDG_Feed();//喂狗
    strx=strstr((const char*)RxBuffer,(const char*)"OK");//返回OK
		
		while(strx==NULL)
    {
        Clear_Buffer();	
        printf("AT\r\n"); 
        delay_ms(300);
        LED1=!LED1;
        strx=strstr((const char*)RxBuffer,(const char*)"OK");//返回OK
    }
		
		LED1=1;
		LED2=1;
		
		printf("ATe0&w\r\n"); 
		delay_ms(300);
		printf("AT+CFUN=1\r\n"); 
		delay_ms(300);
		//printf("AT+QGNSSAGPS=1\r\n"); //启用AGPS功能
		//delay_ms(300);
		printf("AT+QGNSSC=1\r\n");//对GNSS上电
		delay_ms(300);
		printf("AT+QMTCLOSE=0\r\n");//关闭连接
		delay_ms(300);
		IWDG_Feed();//喂狗
}

void Per1msTask(void)
{
		static u8 P10msReq = 0;
		if (P10msReq < 25) 
				P10msReq++;//250ms一次事件
		else 
				P10msReq = 0;  
		switch(P10msReq)
		{
				case 0:
						switch(BC20init.status)//对设备进行初始化
						{
								case 0:  printf_CPIN();usart1_sendstr((u8*)"查询卡的状态！\r\n");break;//获取手机卡
								case 1:  printf_CGATT();usart1_sendstr((u8*)"注册网络\r\n");break;//去注册网络
								case 2:  printf_GetCGATT();usart1_sendstr((u8*)"获取注网情况\r\n");break; //获取注网状态     
								case 3:  printf_CSQ();break; //获取注网状态  
								case 4:  printf_CLSTCP();break; //关闭上一次连接  							
						}
						break;
						
				case 1:
								SCOM_RecieveAT(&m_com);//做数据不断扫描机制
						break;
				
				case 2:
						switch(BC20init.status)
						{
								case 5: printf_ConTCP();usart1_sendstr((u8*)"连接MQTT服务器 TCP模式\r\n");break;//连接服务器
						}                        
						break;
						
				case 3:
						if (BC20init.status==6)//连接服务器成功，准备发数据
						{
								BC20init.tcpcount++;
								if(BC20init.tcpcount>=5)//构建数据包准备发送数据
								{
								
										printf_MQTTCon(BC20init.IMEI);//实现MQTT登录服务器
										usart1_sendstr((u8*)"登录MQTT服务器,利用MQTT方式\r\n");
										BC20init.tcpcount=0;
										BC20init.status=0x0f;//空闲
								}
						}
						break;
						
				case 4://发送数据到MQTT服务器
							if(BC20init.status==0x08)
							{
									BC20init.tcpcount++;
									if(	BC20init.tcpcount>=3)//构建数据包准备发送数据
									{				
											Mqtt_SendGPSdata(mqtt_data.t_payload,BC20init.IMEI,LongLatidata.TrueLatitude,LongLatidata.Truelongitude);//组装数据
											printf_MQTTPub(BC20init.pubtopic,mqtt_data.t_payload);//发布主题消息
											usart1_sendstr((u8*)"发布消息到MQTT服务器\r\n");
											BC20init.tcpcount=0;
											BC20init.status=0x0f;//空闲
									}
							}
							break;
							
				case 5:
							if(BC20init.status==0xFF)//等待连接服务器
									LED1=!LED1;
							break;	
							
				case 6://对获取到的GPS数据进行解析
						if(BC20init.status==7)//登录服务器成功，准备采集GPS数据
						{
								printf("AT+QGNSSRD=\"NMEA/RMC\"\r\n");
						}
						break;
						
				case 7:
							DHT11_Read_Data(&DHT11_DATA.temp,&DHT11_DATA.humi);
						break;
				
				case 8:
						if(BC20init.status==8&&BC20init.subflag==0)//支持订阅与发布
						{
								BC20init.subcount++;
								if(BC20init.subcount>=10)
								{
										printf_MQTTSub(BC20init.subtopic);//订阅主题
										usart1_sendstr((u8*)"订阅消息到MQTT服务器\r\n");
										BC20init.subcount=0;
								}
						}
						break;
						
				case 9:
						if(BC20init.status==0x0f)//
						{
								BC20init.timeout++;
								if(BC20init.timeout>=200)//启动重连
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
	  OLED_ShowChinese(9+18,0,0);//墨子号科技
		OLED_ShowChinese(9+36,0,1);
		OLED_ShowChinese(9+54,0,2);
		OLED_ShowChinese(9+72,0,3);
		OLED_ShowChinese(9+90,0,4);
		OLED_ShowString(30,3,(u8*)"Power on",16);
		
}

void OLED_SHOWGPS(void)
{
	  OLED_ShowChinese(0,0,5);//经纬度
		OLED_ShowChinese(0+18,0,7);
	  OLED_ShowString(0+36,0,(u8*)":000.000000",16);
		OLED_ShowChinese(0,3,6);
		OLED_ShowChinese(0+18,3,7);
		OLED_ShowString(0+36,3,(u8*)":00.000000",16);
}

void OLED_SHOWDHT11(void)
{
	  OLED_ShowChinese(0,0,8);//温湿度
		OLED_ShowChinese(0+18,0,7);
	  OLED_ShowString(0+36,0,(u8*)":00C",16);
		OLED_ShowChinese(0,3,9);
		OLED_ShowChinese(0+18,3,7);
		OLED_ShowString(0+36,3,(u8*)":00%",16);
}

void temphumi_show(void){
		u8 showstr[5];
		u8 sendata[]="727394ACB8221234";
		DHT11_Read_Data(&temp,&humi); //获取温湿度数据值
		showstr[0]=':';
		
		sendata[12]=temp/10+0x30;
		showstr[1]=sendata[12];
		sendata[13]=temp%10+0x30;
		showstr[2]=sendata[13];
		showstr[3]=0;
		OLED_ShowString(0+36,0,showstr,16);//OLED显示温度值
	
		sendata[14]=humi/10+0x30;
		showstr[1]=sendata[14];
		sendata[15]=humi%10+0x30;//转成字符形式
		showstr[2]=sendata[15];
		OLED_ShowString(0+36,3,showstr,16);//OLED显示湿度值
}

void mcu_init(void)
{   
		delay_init();	    	 //延时函数初始化	  
		delay_ms(1000);
		LED_Init();		  		//初始化与LED连接的硬件接口
		BC20CTR_Init();			//BC20端口初始化
		
		uart_init(115200);//串口1初始化，可连接PC进行打印模块返回数据
		uart2_init(115200);//初始化和BC20连接串口	
		
		while(DHT11_Init()){};//检测DHT11是否存在
			
		OLED_Init();			 //初始化OLED  
		OLED_Clear(); 
	  OLED_SHOW();      //开机页面
			
    IWDG_Init(7,625);    //看门狗8S一次
    OPEN_BC20();				//对模块开机
			
		EXTI_Config();
		
		Get_IMEI();					//获取到设备IMEI
    SCOM_Init(&m_com, 0, COM_Rx_Callback, COM_Get_Callback);//回调函数初始化，给此函数赋值获取DMA返回值，并对返回值进行处理。
    SCOM_SetTable(&m_com, ATCMD_Table, "OK\r\n");//将定义数据值传给内部数据值
		USART2_DMAInit();	//串口2采用DMA的方式
    TIM3_Int_Init(199,7199);//20ms中断，为状态机提供基准时钟
			
		OLED_Clear();     //清除开机页面
	  //OLED_SHOWGPS();   //显示经纬度信息
		OLED_SHOWDHT11();   //显示温湿度信息
		temphumi_show();
}

int main(void)
{	
		mcu_init();//单片机初始化
		while(1)
		{	
				if(EXTIstatus.flag)//按键按下被触发,表示接收到呼叫
				{
						EXTIstatus.flag=0; 
						LED1=1;
						LED2=1;
						OLED_Clear();
						OLED_SHOWDHT11();   //显示温湿度信息
						temphumi_show();
						memset(mqtt_data.t_payload, 0, sizeof(mqtt_data.t_payload));
						Mqtt_SendRecvdata(mqtt_data.t_payload,BC20init.IMEI,station);//组装数据
						printf_MQTTPub(BC20init.pubtopic, mqtt_data.t_payload);
				}
				IWDG_Feed();//喂狗
				
				if (P1msReq)//10ms扫描一次
				{
						P1msReq = 0;
						Per1msTask();
						IWDG_Feed();//喂狗
				}
		}
}


