#ifndef __LED_H
#define __LED_H	 
#include "sys.h"

void LED_Init(void);//≥ı ºªØ
void BC20CTR_Init(void);
#define LED1     PCout(6)
#define READLED1 PCin(6)
#define LED2  	 PCout(7)
#define READLED2 PCin(7)
#define PWRKEY  PCout(3)//PEout(0)
#define RESET   PAout(8)

#endif
