#ifndef __callback_H
#define __callback_H	

#include "usart.h"
#include "BC20.h"

int COM_Get_Callback(unsigned char cmd, unsigned char *buf, unsigned int len);
int COM_Rx_Callback(unsigned char cmd, unsigned char *buf, unsigned int len);
void Getdata_Change(u8 *buffer);

typedef struct 
{
char Latitude[9];
char Longitude[10];
char Latitudess[2];//��������
char Longitudess[3];
char Latitudedd[7];//С���㲿��
char Longitudedd[7];
float TrueLatitude;//ת��������
float Truelongitude;//ת��������
}LongLati_data;
#endif


