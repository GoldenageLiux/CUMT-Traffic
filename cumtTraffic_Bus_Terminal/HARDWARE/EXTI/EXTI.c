#include "exti.h"
#include "stm32f10x_exti.h"
EXTIPORT EXTIstatus;
void EXTI_Config(void)
{
  EXTI_InitTypeDef EXTI_InitStructure;
  GPIO_InitTypeDef   GPIO_InitStructure;
  NVIC_InitTypeDef   NVIC_InitStructure;
  /* Enable GPIOE clock */
  RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOE, ENABLE);
  RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO,ENABLE);	//使能复用功能时钟
	
  /* Configure PE0 pin as input floating */
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IPU;
  GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0;
  GPIO_Init(GPIOE, &GPIO_InitStructure);
  GPIO_EXTILineConfig(GPIO_PortSourceGPIOE,GPIO_PinSource0);
	
  /* Configure EXTI0 line */
  EXTI_InitStructure.EXTI_Line = EXTI_Line0;
  EXTI_InitStructure.EXTI_Mode = EXTI_Mode_Interrupt;
  EXTI_InitStructure.EXTI_Trigger = EXTI_Trigger_Falling;  
  EXTI_InitStructure.EXTI_LineCmd = ENABLE;
  EXTI_Init(&EXTI_InitStructure);

  /* Enable and set EXTI0 Interrupt to the lowest priority */
  NVIC_InitStructure.NVIC_IRQChannel = EXTI0_IRQn;
  NVIC_InitStructure.NVIC_IRQChannelPreemptionPriority = 0x0F;
  NVIC_InitStructure.NVIC_IRQChannelSubPriority = 0x0F;
  NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
  NVIC_Init(&NVIC_InitStructure);
}

//中断服务函数
void EXTI0_IRQHandler(void)
{
  if(EXTI_GetITStatus(EXTI_Line0) != RESET)//检测是否触发对应的中断
  {
    EXTIstatus.flag=1;//下降沿触发
    /* Clear the EXTI line 0 pending bit */
    EXTI_ClearITPendingBit(EXTI_Line0);
  }
}


