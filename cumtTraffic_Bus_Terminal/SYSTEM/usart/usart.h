#ifndef __USART_H
#define __USART_H
#include "stdio.h"	
#include "sys.h" 
	
void uart_init(u32 bound);
void uart2_init(u32 bound);
void uart3_init(u32 bound);
int USART3_RecieveData(unsigned char *buf);
void USART3_DMAInit(void);
void USART2_DMAInit(void);
int USART2_RecieveData(unsigned char *buf);
void usart1_sendstr(u8 *data);
void usart1_sendhex(u8 *data,u8 len);
#define USART2_MAX_RECV_LEN		600					//最大接收缓存字节数
#define USART2_MAX_SEND_LEN		600					//最大发送缓存字节数

#endif


