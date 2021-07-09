#include "callback.h"
#include "led.h"
#include "oled.h"
#include "delay.h"
/*********************����������������������������������������������
��������Ϊ���ĵĵط�����Ҫ��DMA���ݻ�ȡ����COM_Rx_Callback�������д��ݡ�
COM_Get_Callback������Ϊ��COM_Rx_Callback��ȡ�������ݽ��н���������ȡ����Ӧ������ֵ
����Ӧ��λ�á�����ֵ����ATCMD_Table����������洢������ֵ������Ľ��������ɻص��������д���
�����Ѿ���main�����������˳�ʼ�����ã�����״̬�����в��ϵ������ݵĽ��մ������û����ԣ�ֻҪ
��COM_Get_Callback��switch������������Լ���Ҫ�ĺ������ɡ�
ī�ӺſƼ�����
*********************************************/
/***********ī�Ӻſ����幺���ַ��
https://item.taobao.com/item.htm?spm=a1z10.3-c.w4023-3005174199.2.62e64f45YMjqsT&id=544366989645
***************/

extern LongLati_data LongLatidata ;  //GPSģ��ľ�γ������ֵ
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
/*************��������γ������*******************/	

/*******************
�û���Ҫ��switch������������Լ���Ҫ��������������Ϣ
ATCMD_Table�ǰ���$����@�ָ����ݼ�϶�ģ���һ��$λ��Ϊ0���ڶ���$Ϊ1����������@Ϊ2���Դ����Ƹ���ָ������ŵ�λ�þͶ��嵽�����case x���ɡ�
********************/
int COM_Get_Callback(unsigned char cmd, unsigned char *buf, unsigned int len)
{
		char *strx;
		if(len > 200)
				len = 200;
		memcpy(timebuf, buf, len);  //�����Ǵ�ӡ�ϴ�����������ֵ�����û��鿴
		
		switch(cmd)
		{
			case 0:    //��ȡ���ֻ���
						BC20init.status=1;//status�������Լ����ж���ģ�0-7������ǵ�¼�������ģ�0XFF�Ǵ���ȴ���������¼����OK,
														//0X0A�����ǿ��Է���TCP���ݣ�0X0B�������ݷ������,0X0C���������ݲɼ���ɣ�
														//����׼���ٴη������ݣ�����Ŀ��Ը��ݴ���ÿһ����λ�ý��ж�Ӧ�鿴����
														//0X0F������SEND ����֮�󣬽���QISACK��ѯ
					break;
			case 1://���ؿ�ע��
						BC20init.status=1;
					break;
			case 2://���ο�ע��
						BC20init.status=3;//ע���ɹ�,���Խ�������Ĳ�����
					break;
			case 3://�ź�ǿ��ֵ
						strx=strstr((const char*)buf,(const char*)":");//��ȡ����λ
						BC20init.csq=(strx[2]-0x30)*10+(strx[3]-0x30); //
						if(BC20init.csq<99||(strx[1]-0x30)>0)
						{
							BC20init.status=4;
							BC20init.csqstr[0]=strx[2];
							BC20init.csqstr[1]=strx[3];
							BC20init.csqstr[2]=0;
							usart1_sendstr((u8*)"�ź�ǿ��ֵ:");
							usart1_sendstr(BC20init.csqstr);	
							usart1_sendstr((u8*)"\r\n");
						}
					break;
			case 4://�ɹ�����SOCKET 
						BC20init.status=6;    //����SOCKET�ɹ�
					break;
			case 5://��¼�������豸����,�ɹ�
						BC20init.status=7;//��¼�ɹ�
						usart1_sendstr((u8*)"MQTT��������¼�ɹ���\r\n");
					break;
			case 6://���ĳɹ�
						BC20init.subflag=1;//���ĳɹ�
						usart1_sendstr((u8*)"MQTT���ⶩ�ĳɹ���\r\n");
					break;
			case 7://�����ɹ�
						BC20init.status=0x07;
						usart1_sendstr((u8*)"MQTT�������ݳɹ���\r\n");
					break;
			case 8://���ĵ�����
						usart1_sendstr((u8*)"���ĵ�����: ");
						memcpy(recsubdata,buf,len);
						RecvSub((char*)recsubdata);
					break;
			case 9://���յ������ݽ��н���
						usart1_sendstr((u8*)"���ĵ�����: ");
					break;
			case 10://�ɼ���GPS����
						BC20init.status=0x08;
						memcpy(timebuf, buf, len);
						Getdata_Change(timebuf);
					break;
			default:
					break;

		}			
		
		return 1;
}

//LED�Ƴ���
void LedChange(void){
		LED1=0;
		LED2=0;
}

//����SUB��������
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
 

