#include "sys.h"
#include "usart.h"	  
#include "stdio.h"
#include "string.h"
#include "stm32f10x_dma.h"
 #define RXBUFFERSIZE  (200)
unsigned char  RxBufferDMA[RXBUFFERSIZE]; 
unsigned char  RxBufferDMA2[RXBUFFERSIZE]; 
unsigned char uart2_getok;
unsigned char RxCounter2,RxBuffer2[100];     
//////////////////////////////////////////////////////////////////
//加入以下代码,支持printf函数,而不需要选择use MicroLIB	  
#if 1
#pragma import(__use_no_semihosting)             
//标准库需要的支持函数                 
struct __FILE 
{ 
	int handle; 

}; 

FILE __stdout;       
//定义_sys_exit()以避免使用半主机模式    
void _sys_exit(int x) 
{ 
	x = x; 
} 
//重定义fputc函数 
int fputc(int ch, FILE *f)
{      
	while((USART2->SR&0X40)==0);//循环发送,直到发送完毕   
    USART2->DR = (u8) ch;      
	return ch;
}
#endif 



//串口1中断服务程序
//注意,读取USARTx->SR能避免莫名其妙的错误   	
unsigned char RxCounter,RxBuffer[200];     //接收缓冲,最大USART_REC_LEN个字节.
//接收状态
//bit15，	接收完成标志
//bit14，	接收到0x0d
//bit13~0，	接收到的有效字节数目
u16 USART_RX_STA=0;       //接收状态标记	  

//初始化IO 串口1 
//bound:波特率
void uart_init(u32 bound){
    //GPIO端口设置
    GPIO_InitTypeDef GPIO_InitStructure;
	USART_InitTypeDef USART_InitStructure;
	NVIC_InitTypeDef NVIC_InitStructure;
	 
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1|RCC_APB2Periph_GPIOA, ENABLE);	//使能USART1，GPIOA时钟
 	USART_DeInit(USART1);  //复位串口1
	 //USART1_TX   PA.9
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_9; //PA.9
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	//复用推挽输出
    GPIO_Init(GPIOA, &GPIO_InitStructure); //初始化PA9
   
    //USART1_RX	  PA.10
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IPU;//浮空输入
    GPIO_Init(GPIOA, &GPIO_InitStructure);  //初始化PA10

   //Usart1 NVIC 配置

    NVIC_InitStructure.NVIC_IRQChannel = USART1_IRQn;
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority=3 ;//抢占优先级3
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 3;		//子优先级3
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			//IRQ通道使能
	NVIC_Init(&NVIC_InitStructure);	//根据指定的参数初始化VIC寄存器
  
   //USART 初始化设置

	USART_InitStructure.USART_BaudRate = bound;//一般设置为9600;
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;//字长为8位数据格式
	USART_InitStructure.USART_StopBits = USART_StopBits_1;//一个停止位
	USART_InitStructure.USART_Parity = USART_Parity_No;//无奇偶校验位
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
	USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//收发模式

    USART_Init(USART1, &USART_InitStructure); //初始化串口
    USART_ITConfig(USART1, USART_IT_RXNE, ENABLE);//开启中断
    USART_Cmd(USART1, ENABLE);                    //使能串口 

}
//初始化IO 串2
//bound:波特率
void uart2_init(u32 bound)
{
    //GPIO端口设置
    GPIO_InitTypeDef GPIO_InitStructure;
	USART_InitTypeDef USART_InitStructure;
	NVIC_InitTypeDef NVIC_InitStructure;
	 
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);	//使能，GPIOA时钟
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2,ENABLE);//USART2
 	USART_DeInit(USART2);  //复位串口2
	 //USART2_TX   PA.2
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_2; //PA.2
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	//复用推挽输出
    GPIO_Init(GPIOA, &GPIO_InitStructure); //初始化PA2
   
    //USART2_RX	  PA.3
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_3;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;//浮空输入
    GPIO_Init(GPIOA, &GPIO_InitStructure);  //初始化PA3

   //Usart1 NVIC 配置

    NVIC_InitStructure.NVIC_IRQChannel = USART2_IRQn;
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority=3 ;//抢占优先级0
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 3;		//子优先级3
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			//IRQ通道使能
	NVIC_Init(&NVIC_InitStructure);	//根据指定的参数初始化VIC寄存器
  
   //USART 初始化设置

	USART_InitStructure.USART_BaudRate = bound;//115200
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;//字长为8位数据格式
	USART_InitStructure.USART_StopBits = USART_StopBits_1;//一个停止位
	USART_InitStructure.USART_Parity = USART_Parity_No;//无奇偶校验位
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
	USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//收发模式
    USART_Init(USART2, &USART_InitStructure); //初始化串口
		
   USART_ITConfig(USART2, USART_IT_RXNE, ENABLE);//开启中断
    USART_Cmd(USART2, ENABLE);                    //使能串口 

}

//初始化IO 串3
//bound:波特率
void uart3_init(u32 bound)
{
    //GPIO端口设置
    GPIO_InitTypeDef GPIO_InitStructure;
	USART_InitTypeDef USART_InitStructure;
	NVIC_InitTypeDef NVIC_InitStructure;
	 
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB, ENABLE);	//使能，GPIOB时钟
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART3,ENABLE);//USART3
 	USART_DeInit(USART3);  //复位串口3
	 //USART3_TX   PB10
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10; //PB10
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;	//复用推挽输出
    GPIO_Init(GPIOB, &GPIO_InitStructure); //初始化PB10
   
    //USART3_RX	  PB11
    GPIO_InitStructure.GPIO_Pin = GPIO_Pin_11;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;//浮空输入
    GPIO_Init(GPIOB, &GPIO_InitStructure);  //初始化B11

   //Usart3 NVIC 配置

    NVIC_InitStructure.NVIC_IRQChannel = USART3_IRQn;
	NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority=0 ;//抢占优先级0
	NVIC_InitStructure.NVIC_IRQChannelSubPriority = 1;		//子优先级3
	NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;			//IRQ通道使能
	NVIC_Init(&NVIC_InitStructure);	//根据指定的参数初始化VIC寄存器
  
   //USART 初始化设置

	USART_InitStructure.USART_BaudRate = bound;//115200
	USART_InitStructure.USART_WordLength = USART_WordLength_8b;//字长为8位数据格式
	USART_InitStructure.USART_StopBits = USART_StopBits_1;//一个停止位
	USART_InitStructure.USART_Parity = USART_Parity_No;//无奇偶校验位
	USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;//无硬件数据流控制
	USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;	//收发模式
    USART_Init(USART3, &USART_InitStructure); //初始化串口
		
    USART_ITConfig(USART3, USART_IT_RXNE, ENABLE);//开启中断
    USART_Cmd(USART3, ENABLE);                    //使能串口 

}
void USART3_DMAInit(void)                
{
  DMA_InitTypeDef  DMA_InitStructure;
  
  
  USART_ITConfig(USART3, USART_IT_RXNE, DISABLE);//??????
  /*------------------------------- DMA---------------------------------------*/   
  /* Enable the DMA periph */
  RCC_AHBPeriphClockCmd(RCC_AHBPeriph_DMA1, ENABLE);
  
  /* Common DMA configuration */
  DMA_InitStructure.DMA_BufferSize = RXBUFFERSIZE;
  DMA_InitStructure.DMA_PeripheralDataSize = DMA_PeripheralDataSize_Byte;
  DMA_InitStructure.DMA_MemoryDataSize = DMA_MemoryDataSize_Byte;
  DMA_InitStructure.DMA_PeripheralInc = DMA_PeripheralInc_Disable;
  DMA_InitStructure.DMA_MemoryInc = DMA_MemoryInc_Enable;
  DMA_InitStructure.DMA_Mode = DMA_Mode_Normal;
  DMA_InitStructure.DMA_M2M = DMA_M2M_Disable;

  /* DMA1 Channel3 configuration */
  DMA_InitStructure.DMA_MemoryBaseAddr = (uint32_t)RxBufferDMA;
  DMA_InitStructure.DMA_DIR = DMA_DIR_PeripheralSRC;
  DMA_InitStructure.DMA_Priority = DMA_Priority_Medium;
  DMA_InitStructure.DMA_PeripheralBaseAddr = (uint32_t)(&USART3->DR);//0x40004424
  DMA_Init(DMA1_Channel3, &DMA_InitStructure);


   USART_DMACmd(USART3, USART_DMAReq_Rx, ENABLE);
  /* Enable the DMA1 channels */
  DMA_Cmd(DMA1_Channel3, ENABLE);  
}
void USART2_DMAInit(void)                
{
  DMA_InitTypeDef  DMA_InitStructure;
  
  
  USART_ITConfig(USART2, USART_IT_RXNE, DISABLE);//??????
  /*------------------------------- DMA---------------------------------------*/   
  /* Enable the DMA periph */
  RCC_AHBPeriphClockCmd(RCC_AHBPeriph_DMA1, ENABLE);
  
  /* Common DMA configuration */
  DMA_InitStructure.DMA_BufferSize = RXBUFFERSIZE;
  DMA_InitStructure.DMA_PeripheralDataSize = DMA_PeripheralDataSize_Byte;
  DMA_InitStructure.DMA_MemoryDataSize = DMA_MemoryDataSize_Byte;
  DMA_InitStructure.DMA_PeripheralInc = DMA_PeripheralInc_Disable;
  DMA_InitStructure.DMA_MemoryInc = DMA_MemoryInc_Enable;
  DMA_InitStructure.DMA_Mode = DMA_Mode_Normal;
  DMA_InitStructure.DMA_M2M = DMA_M2M_Disable;

  /* DMA1 Channel6 configuration */
  DMA_InitStructure.DMA_MemoryBaseAddr = (uint32_t)RxBufferDMA2;
  DMA_InitStructure.DMA_DIR = DMA_DIR_PeripheralSRC;
  DMA_InitStructure.DMA_Priority = DMA_Priority_Medium;
  DMA_InitStructure.DMA_PeripheralBaseAddr = (uint32_t)(&USART2->DR);//0x40004424
  DMA_Init(DMA1_Channel6, &DMA_InitStructure);


   USART_DMACmd(USART2, USART_DMAReq_Rx, ENABLE);
  /* Enable the DMA1 channels */
  DMA_Cmd(DMA1_Channel6, ENABLE);  
}
void usart1_sendstr(u8 *data)
{
	while(*data)
	{
		while((USART1->SR&0X40)==0){};//循环发送,直到发送完毕   
    USART1->DR = (u8) *data; 
    data++;		
  }
}
void usart1_sendhex(u8 *data,u8 len)
{
 u8 i;
	for(i=0;i<len;i++)
	{
		while((USART1->SR&0X40)==0){};//循环发送,直到发送完毕   
    USART1->DR = (u8) *data; 
    data++;		
  }
}
void USART1_IRQHandler(void)                	//串口1中断服务程序
{
		u8 Res;
		if(USART_GetITStatus(USART1, USART_IT_RXNE) != RESET)  //接收中断，可以扩展来控制
		{
			 Res =USART_ReceiveData(USART1);//接收模块的数据
		} 
} 
void USART2_IRQHandler(void)                	//串口2中断服务程序
	{

if(USART_GetITStatus(USART2, USART_IT_RXNE) != RESET)  //接收中断，可以扩展来控制
		{
			//  	TIM_Cmd(TIM3, ENABLE);  
	      RxBuffer[RxCounter++] =USART_ReceiveData(USART2);//接收模块的数据
    } 

} 


void USART3_IRQHandler(void)                	//串口3中断服务程序
	{
if(USART_GetITStatus(USART3, USART_IT_RXNE) != RESET)  //接收模块返回的数据
		{
			RxBuffer[RxCounter++] =USART_ReceiveData(USART3);//接收模块的数据
    
   } 
if(RxCounter>=200)
	RxCounter=0;
} 	


int USART3_RecieveData(unsigned char *buf)
{   
    int len;
    
    len = RXBUFFERSIZE - DMA1_Channel3->CNDTR;
    if(len > 0)
    {
      DMA_Cmd(DMA1_Channel3, DISABLE);
      memcpy(buf, RxBufferDMA, len);
      DMA1_Channel3->CMAR = (uint32_t)RxBufferDMA;
      DMA1_Channel3->CNDTR = RXBUFFERSIZE;
      DMA_Cmd(DMA1_Channel3, ENABLE);
    }
    return len;
}

int USART2_RecieveData(unsigned char *buf)
{   
    int len;
    
    len = 200 - DMA1_Channel6->CNDTR;
    if(len > 0)
    {
      DMA_Cmd(DMA1_Channel6, DISABLE);
      memcpy(buf, RxBufferDMA2, len);
      DMA1_Channel6->CMAR = (uint32_t)RxBufferDMA2;
      DMA1_Channel6->CNDTR = 200;
      DMA_Cmd(DMA1_Channel6, ENABLE);
    }
    return len;
}



