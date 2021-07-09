#ifndef __SCCOM_H
#define __SCCOM_H





#define SCOM_MAX_PACK_LEN		(256)
#define SCOM_TAB_FLAG1			'@'
#define SCOM_TAB_FLAG2			'$'

typedef int (*f_ComCb)(unsigned char, unsigned char*, unsigned int);


typedef struct
{
	char			*ComTab;
	char			*EndStr;
	int				ESLen;
	f_ComCb			TxData;
	f_ComCb			RxData;
	f_ComCb			PackGet;
	unsigned char 	TxBuf[SCOM_MAX_PACK_LEN];
	unsigned char 	RxBuf[SCOM_MAX_PACK_LEN * 2];
	unsigned int 	iDataPos;
} S_Comhd;






#define	SCOM_PACK_HEAD		(0xF3)
#define	SCOM_PACK_TAIL		(0x83)
#define	SCOM_EXTRA_LEN		(7)//head + len + cmd + crc16 + tail

int SCOM_Init(S_Comhd* com, f_ComCb txCb, f_ComCb rxCb, f_ComCb getCb);
void SCOM_SetTable(S_Comhd* com, const char *tab, const char *estr);
void SCOM_Transmit(S_Comhd* com, unsigned char cmd, unsigned char *buf, unsigned short len);
int SCOM_Recieve(S_Comhd* com);
int SCOM_RecieveAT(S_Comhd* com);




#endif

