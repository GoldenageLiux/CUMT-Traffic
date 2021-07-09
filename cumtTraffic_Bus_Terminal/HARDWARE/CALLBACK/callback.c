#include "callback.h"
#include "led.h"
#include "oled.h"
#include "delay.h"
/*********************×××××××××××××××××××××××
这里是作为核心的地方，主要有DMA数据获取给到COM_Rx_Callback函数进行传递。
COM_Get_Callback函数作为对COM_Rx_Callback获取到的数据进行解析，并获取到对应的命令值
所对应的位置。命令值就是ATCMD_Table这个里面所存储的数据值。这里的交互都是由回调函数自行处理。
我们已经在main函数里面做了初始化引用，并在状态机当中不断的做数据的接收处理。对用户而言，只要
在COM_Get_Callback的switch函数里面加入自己需要的函数即可。
墨子号科技出版
*********************************************/
/***********墨子号开发板购买地址：
https://item.taobao.com/item.htm?spm=a1z10.3-c.w4023-3005174199.2.62e64f45YMjqsT&id=544366989645
***************/

extern LongLati_data LongLatidata ;  //GPS模块的经纬度数据值
extern BC20_init BC20init;

unsigned char timebuf[200];
unsigned char recsubdata[100];
char* data;
void LedChange(void);
void RecvSub(char *buf);

char station[50];
char stationID[50] = "To station:";
char userCount[50] = "userCount:";

char *point; 
char *bufferData; 
char *delims={ ":,\"" };

int COM_Rx_Callback(unsigned char cmd, unsigned char *buf, unsigned int len)
{
		return USART2_RecieveData(buf);
}
/*************解析出经纬度数据*******************/	

/*******************
用户需要在switch函数里面加入自己需要处理的相关数据信息
ATCMD_Table是按照$或者@分割数据间隙的，第一个$位置为0，第二个$为1，第三个是@为2，以此类推各个指令所存放的位置就定义到下面的case x即可。
********************/
int COM_Get_Callback(unsigned char cmd, unsigned char *buf, unsigned int len)
{
		char *strx;
		if(len > 200)
				len = 200;
		memcpy(timebuf, buf, len);  //这里是打印上传上来的数据值，供用户查看
		
		switch(cmd)
		{
			case 0:    //获取到手机卡
						BC20init.status=1;//status是我们自己进行定义的，0-7代表的是登录服务器的，0XFF是代表等待服务器登录返回OK,
														//0X0A代表是可以发送TCP数据，0X0B代表数据发送完成,0X0C代表是数据采集完成，
														//可以准备再次发送数据，具体的可以根据代码每一步的位置进行对应查看即可
														//0X0F代表是SEND 数据之后，进行QISACK查询
					break;
			case 1://本地卡注网
						BC20init.status=1;
					break;
			case 2://漫游卡注网
						BC20init.status=3;//注网成功,可以进行下面的操作了
					break;
			case 3://信号强度值
						strx=strstr((const char*)buf,(const char*)":");//获取符号位
						BC20init.csq=(strx[2]-0x30)*10+(strx[3]-0x30); //
						if(BC20init.csq<99||(strx[1]-0x30)>0)
						{
							BC20init.status=4;
							BC20init.csqstr[0]=strx[2];
							BC20init.csqstr[1]=strx[3];
							BC20init.csqstr[2]=0;
							usart1_sendstr((u8*)"信号强度值:");
							usart1_sendstr(BC20init.csqstr);	
							usart1_sendstr((u8*)"\r\n");
						}
					break;
			case 4://成功建立SOCKET 
						BC20init.status=6;    //创建SOCKET成功
					break;
			case 5://登录服务器设备返回,成功
						BC20init.status=7;//登录成功
						usart1_sendstr((u8*)"MQTT服务器登录成功！\r\n");
					break;
			case 6://订阅成功
						BC20init.subflag=1;//订阅成功
						usart1_sendstr((u8*)"MQTT主题订阅成功！\r\n");
					break;
			case 7://发布成功
						BC20init.status=0x07;
						usart1_sendstr((u8*)"MQTT发布数据成功！\r\n");
					break;
			case 8://订阅到数据
						usart1_sendstr((u8*)"订阅到数据: ");
						memcpy(recsubdata,buf,len);
						RecvSub((char*)recsubdata);
					break;
			case 9://对收到的数据进行解析
						usart1_sendstr((u8*)"订阅到数据: ");
					break;
			case 10://采集到GPS数据
						BC20init.status=0x08;
						memcpy(timebuf, buf, len);
						Getdata_Change(timebuf);
					break;
			default:
					break;

		}			
		
		return 1;
}

//LED灯常亮
void LedChange(void){
		LED1=0;
		LED2=0;
}

//解析SUB到的数据
void RecvSub(char *bufferData){
		
		int i = 0;
		usart1_sendstr((u8*)bufferData);
		char *strx = strstr((const char*)bufferData,(const char*)"station");
	
		point=strtok(strx,delims); 
		i++;
		while(point!=NULL){ 
				if(i == 2 )
				{
						strcat(stationID,point);
						memset(station,0,sizeof(station));
						strcat(station,stationID);
				}
				if(i == 4)
						strcat(userCount,point);
				point=strtok(NULL,delims); 
				i++;
		} 
	
		OLED_Clear();
		OLED_ShowString(18,0,(u8*)userCount,16);
		OLED_ShowString(18,3,(u8*)stationID,16);
		OLED_ShowString(18,6,(u8*)"IS CALLING...",16);
		strcpy(userCount,"userCount:");
		strcpy(stationID,"To station:");
		LedChange();
}
 

