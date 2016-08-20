/* RadioCountToLedsC.nc,v 1.5 2009/06/07 15:45:30 stoleru Exp $
/*									tab:4
 * "Copyright (c) 2000-2005 The Regents of the University  of California.  
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Copyright (c) 2002-2003 Intel Corporation
 * All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE     
 * file. If you do not find these files, copies can be found by writing to
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300, Berkeley, CA, 
 * 94704.  Attention:  Intel License Inquiry.
 */
 
//#include "Timer.h"
#include "RadioCountToLeds.h"
//#define LPL_INTERVAL 2000
//#include "printf.h"

/**
 * Implementation of the RadioCountToLeds application. RadioCountToLeds 
 * maintains a 4Hz counter, broadcasting its value in an AM packet 
 * every time it gets updated. A RadioCountToLeds node that hears a counter 
 * displays the bottom three bits on its LEDs. This application is a useful 
 * test to show that basic AM communication and timers work.
 *
 * @author Philip Levis
 * @date   June 6 2005
 */
module RadioCountToLedsC {
  uses {
    /* Leds for debugging and boot is the main interface */
    interface Leds;
    interface Boot;
    
    /* For receiving and transmitting packets */
    interface Receive;
    interface AMSend;

    /* timers for different activities 
       Timer1   - Source sends normal packets
       Timer2   - For coded packet transmission
       Timer3   - NACK timer
       Timer4   - Renack
       Timer5&6 - For debugging to transmit num packets received 
     */	
    interface Timer<TMilli> as MilliTimer1;
    interface Timer<TMilli> as MilliTimer2;
    interface Timer<TMilli> as MilliTimer3;
    interface Timer<TMilli> as MilliTimer4;
#if defined(DEBUG)
    interface Timer<TMilli> as MilliTimer5;
    interface Timer<TMilli> as MilliTimer6;
#endif

    interface Timer<TMilli> as SwitchTimer;
    interface Timer<TMilli> as BlinkTimer;
#if defined(ENHANCEMENT)
    interface Receive as SerialReceive_CS;
    interface Init as TopoInit;
#endif 
    
    /* Roja Feb 10 
    For sending packets through UART */
    interface AMSend as AMSendS;
    interface SplitControl as AMControlS; 	
    /* Roja Feb 10 */

    /*
      Interfaces for sending packets wireless */
    interface SplitControl;
    interface Packet;
    interface Random;
    interface ParameterInit<uint16_t> as SeedInit;
   
    /* LPL interface */
//#if defined(LOW_POWER_LISTENING) || defined(RANDOM_DUTY_CYCLING)
    interface LowPowerListening as LPL;
//#endif	
    interface StopDutyCycling;

    //interface SplitControl as RSplitControl;
    /*interface AMSend as RAMSend;
    interface Receive as RReceive;
    interface Packet as RPacket;
    interface LowPowerListening as RandLPL; */
  }
}

implementation {
  /* normal and UART packets and flags */
  message_t packet1;
  message_t packet;
  bool locked;
  bool uartlocked=FALSE;
  /* different counters */
  uint16_t countabc=0;
  uint16_t counter = 0;
  uint8_t scounter = 0;
  /* timer to call appropriate timer in sendDone for streaming */
  uint8_t timer = 0;
  uint8_t tempnack = 0;
  /* Different msg counts for debugging */
  uint16_t msgcount=0; 
  uint16_t Nmsgcount=0; 
  uint16_t RNmsgcount=0; 
  uint16_t toSend = 0;
  uint16_t blinks = 0;
  uint8_t ICounter=0;
  uint8_t RCounter=0;
  uint8_t mode=MODE_INIT;
  bool csselected=FALSE;
  uint8_t ncoding_scheme = 1;
  uint16_t rand=0;
  uint8_t shuffle = 0;
  uint8_t coding_scheme = 1;
#if defined(ENHANCEMENT)
  bool csreceived=FALSE;
  uint8_t optcoding_scheme = 1;
  int8_t hopcount = -1;
#endif
  bool DutyCycling = FALSE;
  bool SendDoneSwitch=FALSE;
  uint16_t decoded = 0;
  uint16_t exp = 1;
  uint16_t neighbor = AM_BROADCAST_ADDR;
  radio_count_msg_t pkt_queue[QUEUE_SIZE1];
  //radio_count_msg_t UartMsg[QUEUE_SIZE1];
  uint8_t uhead = 0;
  uint8_t utail = 0;
  uint8_t qhead = 0;
  uint8_t qtail = 0;
  uint16_t curNeighbor=0;
  uint16_t avgNeighbor=100;
  uint16_t pageNeighbor[CACHE_PAGE];
  uint8_t nack_msg[CACHE_PAGE], useful_pkt[CACHE_PAGE], renack_msg;
  uint16_t renack_num;
  uint32_t power5[8] = {1,5,25,125,625,3125,15625, 78125};
  uint8_t power2[8] = {1,2,4,8,16,32,64,128};
  
  uint8_t coeff[CACHE_PAGE][8][8];
  int32_t input_n[8][8], input_d[8][8], result_n[8][8], result_d[8][8];
  
  void SendDone(message_t* bufPtr, error_t error);
  message_t* HandlePacket(message_t* bufPtr, 
				   void* payload, uint8_t len); 
  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  int32_t abs(int32_t a){
    if(a < 0)
      return -1*a;
    return a;
  }


  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  int32_t gcd(int32_t a, int32_t b){
    if(a == 0)
      return b;
    else if(b == 0)
      return a;
    else 
      return gcd(b, a%b);
  }


  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  void g_interchange(int8_t num_var, int8_t x, int8_t y) { //return rank
    int8_t i;
    int32_t temp;
    for(i = 0; i < num_var; i++){
      temp = input_n[x][i];
      input_n[x][i] = input_n[y][i];
      input_n[y][i] = temp;
      temp = input_d[x][i];
      input_d[x][i] = input_d[y][i];
      input_d[y][i] = temp;
      temp = result_n[x][i];
      result_n[x][i] = result_n[y][i];
      result_n[y][i] = temp;
      temp = result_d[x][i];
      result_d[x][i] = result_d[y][i];
      result_d[y][i] = temp;
    }
  }
  
  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  void g_multiply(int8_t num_var, int8_t x, int32_t nomi, int32_t deno) //return rank
  {
    int8_t i;
    int32_t j,k;
    for(i = 0; i < num_var;i++){
      if(nomi != 0){
        if(input_n[x][i] != 0){      
          j = gcd(abs(input_n[x][i]), abs(deno));
          input_n[x][i] /= j;
          k = gcd(abs(input_d[x][i]), abs(nomi));
          input_d[x][i] /= k;
          input_n[x][i] *= nomi/k;
          input_d[x][i] *= deno/j;
          if(input_d[x][i] == 0)
            dbg("Gaussian", "wahaha %u %u\n", deno, j);
        }
        
        if(result_n[x][i] != 0){      
          j = gcd(abs(result_n[x][i]), abs(deno));
          result_n[x][i] /= j;
          k = gcd(abs(result_d[x][i]), abs(nomi));
          result_d[x][i] /= k;
          result_n[x][i] *= nomi/k;
          result_d[x][i] *= deno/j;
        }
      }
      else{
        input_n[x][i] = 0;
        input_d[x][i] = 1;
        result_n[x][i] = 0;
        result_d[x][i] = 1;
      }
      if(input_d[x][i] < 0){
        input_n[x][i] *= -1;
        input_d[x][i] *= -1;
      }
      if(result_d[x][i] < 0){
        result_n[x][i] *= -1;
        result_d[x][i] *= -1;
      }
    }
  }

  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  void g_add(int8_t num_var, int8_t x, int8_t y, int32_t nomi, int32_t deno)
  {    
    uint8_t i;
    int32_t j;
    int32_t temp_n_input, temp_d_input, temp_n_n, temp_d_d;
    if(nomi == 0)
      return;
    for(i = 0; i < num_var;i++){
      j = gcd(abs(input_d[y][i]),abs(nomi));
      temp_n_input = nomi/j;
      temp_d_input = input_d[y][i]/j;      
      j = gcd(abs(input_n[y][i]),abs(deno));
      temp_n_input *= input_n[y][i]/j;
      temp_d_input *= deno/j;
      /*      temp_d_input = deno*input_d[y][i];
	      temp_n_input = nomi*input_n[y][i];
	      j = gcd(abs(temp_d_input), abs(temp_n_input));
	      temp_d_input /= j;
	      temp_n_input /= j;
      */ 
      j = gcd(abs(temp_d_input), abs(input_d[x][i]));     
      input_n[x][i] = input_n[x][i]*(temp_d_input/j) + temp_n_input*(input_d[x][i]/j);
      input_d[x][i] *= temp_d_input/j;
      j = gcd(abs(input_n[x][i]), abs(input_d[x][i]));
      input_n[x][i] /= j;
      input_d[x][i] /= j;      
      j = gcd(abs(result_d[y][i]),abs(nomi));
      temp_n_input = nomi/j;
      temp_d_input = result_d[y][i]/j;      
      j = gcd(abs(result_n[y][i]),abs(deno));
      temp_n_input *= result_n[y][i]/j;
      temp_d_input *= deno/j;
      
      j = gcd(abs(temp_d_input), abs(result_d[x][i]));     
      result_n[x][i] = result_n[x][i]*(temp_d_input/j) + 
	temp_n_input*(result_d[x][i]/j);
      result_d[x][i] *= temp_d_input/j;
      j = gcd(abs(result_n[x][i]), abs(result_d[x][i]));
      result_n[x][i] /= j;
      result_d[x][i] /= j;      
      if(input_d[x][i] < 0){
        input_n[x][i] *= -1;
        input_d[x][i] *= -1;
      }
      if(result_d[x][i] < 0){
        result_n[x][i] *= -1;
        result_d[x][i] *= -1;
      }
    }
  }


  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  int8_t gaussian(int8_t num_eq, int8_t num_var, uint8_t gen_nack) { //return rank 
    int8_t cur_row = 0;
    int8_t i,j,k;
    nack_msg[gen_nack] = 255;
        
    for( i = 0;i < num_var;i++)
      {
	for(j = cur_row; j < num_eq;j++)
	  if(input_n[j][i]){
	  if(j > cur_row)
	    g_interchange(num_var, cur_row,j);
	  break;
	}
	if(input_n[cur_row][i] == 0)
	  continue;
		    
	//      dbg("Gaussian", "multipy, %d, %d\n",input_d[cur_row][i], input_n[cur_row][i]);
	g_multiply(num_var, cur_row, input_d[cur_row][i], input_n[cur_row][i]);
	//      dbg("Gaussian", "end multipy %d %d\n", input_n[cur_row][i], input_d[cur_row][i]);
	for(j = cur_row+1;j < num_eq;j++){
	  dbg("Gaussian", "add 1, %d, %d\n",-1 * input_n[j][i], input_d[j][i]);
	  g_add(num_var, j, cur_row, -1 * input_n[j][i], input_d[j][i]);
	}
	cur_row++;
	if(cur_row >= num_eq)
	  break;
      }
    num_eq = cur_row;
    cur_row = 0;
    for( i = 0;i < num_eq;i++)
      {
	for(j = cur_row; j < num_var;j++)
	  if(input_n[i][j])
	  break;
	if(j == num_var)
	  continue;
	cur_row = j;
	nack_msg[gen_nack] &= (power2[cur_row]^255);
	for(k = i-1; k >= 0; k--){
	  //     dbg("Gaussian", "add 2, %d, %d\n",-1 * input_n[k][j], input_d[k][j]);
	  g_add(num_var, k, i, -1 * input_n[k][j], input_d[k][j]);
	  //      dbg("Gaussian", "end add 2\n");
	}
      }
    return num_eq;
  }

  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  /* Roja Feb 16 Added type in the prototype */
  void enqueue(uint8_t num_pkts, uint16_t pkt0, uint16_t pkt1, uint8_t isNACK, uint16_t dest, uint32_t coeff, uint8_t nackeff, uint8_t rem_pkts){
    radio_count_msg_t* rcm = &pkt_queue[qtail];
    //rcm->type=type;
    rcm->num_pkts=num_pkts;
    rcm->pkt[0]=pkt0;
    rcm->pkt[1]=pkt1;
    rcm->isNACK=isNACK;
    rcm->dest=dest;
    rcm->coeff = coeff;
    rcm->nackeff = nackeff;
    //rcm->timer5fired=timer5fired;    
    rcm->rem_pkts=rem_pkts;
    qtail=(qtail+1)%QUEUE_SIZE1;
    
    if(isNACK==1){
      dbg("NetworkCoding", "E,%hu,%hu,%hu,%hu+Enqueued NACK %hu ~ %hu sending to %hu, queue size %hu.\n",rcm->pkt[0],rcm->pkt[1],rcm->dest,call MilliTimer2.getNow(), rcm->pkt[0], rcm->pkt[1], rcm->dest, (qtail+QUEUE_SIZE1-qhead)%QUEUE_SIZE1);
    }else{
      dbg("NetworkCoding", "E,%hu,%hu,-1,%hu+Enqueued packet %hu ~ %hu, current time is %hu, queue size %hu.\n",rcm->pkt[0],rcm->pkt[1],call MilliTimer2.getNow(), rcm->pkt[0], rcm->pkt[1], call MilliTimer2.getNow(), (qtail+QUEUE_SIZE1-qhead)%QUEUE_SIZE1);
    }
    
    if(qtail==((qhead+1)%QUEUE_SIZE1)){
      rand = call Random.rand16();
      rand = rand%BACKOFF;
      //      toSend = 1;
      //      dbg("NetworkCoding", "D,0,0,0,0+Start timer for sending, current time is %hhu, time to send is %hu.\n", call MilliTimer2.getNow(), 10+rand);
       //call Leds.led0Toggle();
       call MilliTimer2.startPeriodic(10 + rand);
      //call MilliTimer2.startPeriodic(2);
    }
  }



  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  //************************ NETWORKING CODE ********************************
  //**************************************************************************
  //**************************************************************************
  //**************************************************************************

  event void Boot.booted() {
   /*if(mode==LPLM)
   {
    //call Leds.led2Toggle();
    if (TOS_NODE_ID != SOURCE) { 
      call LPL.setLocalSleepInterval(LPL_INTERVAL);
    }
   } */
    call SplitControl.start();
    call AMControlS.start();
    //call TopoInit.init();	
    nack_msg[1]=127;
  }

  
  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
   /*event void RSplitControl.startDone(error_t err) {
    //call SplitControl.start();
    call Leds.led2Toggle();
  } */
  
  event void SplitControl.startDone(error_t err) {
  
    if (err == SUCCESS) {
      uint8_t i;
      //call Leds.led0Toggle();
      for(i = 0; i < 8; i++)
	useful_pkt[i] = 0;
      for(i = 0; i < CACHE_PAGE;i++)
        pageNeighbor[i] = 0;
//#if defined(RANDOM_DUTY_CYCLING)   
      //call LPL.setLocalSleepInterval(RLPL);
//#endif	
//#if defined(LOW_POWER_LISTENING)   
   if(mode==LPLM)
   {
    //call Leds.led2Toggle();
    if (TOS_NODE_ID != SOURCE) { 
      DutyCycling=TRUE;
      call LPL.setLocalSleepInterval(LPL_INTERVAL);
    }
   } 
//#endif	
    call BlinkTimer.startPeriodic(15);
#if !defined(ENHANCEMENT)
      if(TOS_NODE_ID == SOURCE){
        toSend = TOTAL_PKT;
        //call Leds.led2Toggle();
        call MilliTimer1.startPeriodic(SOURCETIME);
      }
#if defined(DEBUG)
      /*else
      {
        call MilliTimer6.startOneShot(27000+TOS_NODE_ID);
	      } */
#endif
#endif
    }
    else {
      //call Leds.led1Toggle();
      call SplitControl.start();
    }
  }


  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  /*event void RSplitControl.stopDone(error_t err) {
    // do nothing
  } */ 
  event void SplitControl.stopDone(error_t err) {
    // do nothing
  }  

#if defined(DEBUG) 
  /* Timer 6 for collecting data for analysis
   */
  event void MilliTimer6.fired() {
     // call MilliTimer6.stop();
     radio_count_msg_t* rcm = (radio_count_msg_t*) call Packet.getPayload(&packet1, 0 /*NULL*/);
        if(uartlocked) return;
        rcm->num_pkts=2;
        rcm->pkt[0]=msgcount;
        rcm->pkt[1]=Nmsgcount;
        rcm->coeff=RNmsgcount;
        rcm->isNACK=decoded;
        if(decoded==TOTAL_PKT) rcm->num_pkts=6;
        rcm->source=TOS_NODE_ID;
        if(call AMSendS.send(AM_BROADCAST_ADDR, &packet1, sizeof(radio_count_msg_t)) == SUCCESS) {
           uartlocked=TRUE;
           call MilliTimer6.stop();
        }
        else
         {
            call MilliTimer6.startPeriodic(100+TOS_NODE_ID);
         }
 
   }
 /*
  For Collecting data for analysis - Debug
  */
  event void MilliTimer5.fired() {
    /*timer5fired++;
    call MilliTimer5.stop();
    call MilliTimer5.startOneShot(TIMER5); */
    radio_count_msg_t* rcm = (radio_count_msg_t*) call Packet.getPayload(&packet1, 0 /*NULL*/);
        if(uartlocked) return;
        rcm->num_pkts=6;
        //timer5fired++;
        rcm->source=TOS_NODE_ID;
        rcm->pkt[0]=msgcount;
        rcm->pkt[1]=Nmsgcount;
        rcm->coeff=RNmsgcount;
        if(call AMSendS.send(AM_BROADCAST_ADDR, &packet1, sizeof(radio_count_msg_t)) == SUCCESS) {
           uartlocked=TRUE;
           call MilliTimer5.stop();
           call MilliTimer6.stop();
        }

  }
#endif
  event void BlinkTimer.fired() {
  blinks++;
   //call Leds.led0Toggle();
  if(blinks==5000)
  {
   call BlinkTimer.stop();
   call Leds.led0Off();
  }
 }

 /*
  Timer1 for source 
  */
  event void MilliTimer1.fired() {
    countabc++;
    //call Leds.led0Toggle();
    /* Just to check if the code download was successful to the source */
    if(countabc%10==0)
         call Leds.led1Toggle(); 
    while(countabc<2000) 
         return;	
    if(toSend <= 0){
      call MilliTimer1.stop();
      return;
    }
    if (locked) {
      //call MilliTimer1.stop();
      //call MilliTimer1.startPeriodic(10);
      //dbg("NetworkCoding", "here...............");
      return;
    }
    else {
      if(TOS_NODE_ID == SOURCE){
	//if(counter==0) call MilliTimer1.startPeriodic(1);
        radio_count_msg_t* rcm = (radio_count_msg_t*) call Packet.getPayload(&packet, 0 /*NULL*/);
        if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
	   //call Leds.led0Toggle();
           return;
        }
        rcm->num_pkts = 1;
        rcm->pkt[0] = counter;
        rcm->pkt[1] = counter;
        rcm->source = TOS_NODE_ID;
        rcm->coeff = power5[counter%8];
	/* Roja 02-27-09 */
        rcm->nackeff = 0;
        rcm->isNACK=0; /* Roja NEW */
        //rcm->dummy[19]=45;
#if defined(PKT_SIZE)
        memset(rcm->dummy,200,sizeof(rcm->dummy));
#endif
        scounter=8-(counter%8);
        timer=1;
//#if defined(RANDOM_DUTY_CYCLING)
      if(mode==RLPLM)
	call LPL.setRxSleepInterval(&packet,(scounter-1));
//#endif  
//#if defined(LOW_POWER_LISTENING)
      else if(mode==LPLM)
	call LPL.setRxSleepInterval(&packet, LPL_INTERVAL);
//#endif
      rcm->mode=mode;
      rcm->rem_pkts=scounter;
        if (call AMSend.send(AM_BROADCAST_ADDR, &packet, sizeof(radio_count_msg_t)) == SUCCESS) {
          locked = TRUE;
          msgcount++;
          counter++;
          decoded = counter;
          toSend--;
          dbg("NetworkCoding", "I,%hu,%hu,-1,%hu+Sink ready to send, packet is %hu, toSend is %hu\n", rcm->pkt[0],rcm->pkt[1], call MilliTimer1.getNow(),rcm->pkt[0], toSend);
          //dbg("DbgSource", "I,%hu,%hu,-1,%hu+Sink ready to send, packet is %hu, toSend is %hu\n", rcm->pkt[0],rcm->pkt[1], call MilliTimer1.getNow(),rcm->pkt[0], toSend);
          
	  if(counter %8 == 0)
	    {
	      //MKS
	      //dbg("NetworkCoding"," calling timer1");
	      call MilliTimer1.stop();
	      call MilliTimer4.startOneShot(ROUNDBACK);
	    }
	  //dbg("NetworkCoding"," calling timer1 not followed");
        }        
      }
    }
  }


  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  event void MilliTimer2.fired(){   // used to send packets
    dbg("NetworkCoding", "qtail %hu qhead %hu locked %hu \n", qtail,qhead,locked);
      //call Leds.led2Toggle();
    if(((qtail+QUEUE_SIZE1-qhead)%QUEUE_SIZE1)<=0){
      dbg("NetworkCoding", "qtail %hu qhead %hu in first iflocked %hu \n", qtail,qhead,locked);
      call MilliTimer2.stop();
      return;
    }
    if (locked) 
      {
        //call MilliTimer2.stop();
        //call MilliTimer2.startPeriodic(10+2*TOS_NODE_ID);
	dbg("NetworkCoding", "qtail %hu qhead %hu locked if locked%hu \n", qtail,qhead,locked);
	return;
      }
    else{
      radio_count_msg_t* rcm = (radio_count_msg_t*)call Packet.getPayload(&packet,0 /*NULL*/);
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
	return;
      }
      rcm->num_pkts = pkt_queue[qhead].num_pkts;
      rcm->pkt[0] = pkt_queue[qhead].pkt[0];
      rcm->pkt[1] = pkt_queue[qhead].pkt[1];
      rcm->isNACK = pkt_queue[qhead].isNACK;		
      rcm->coeff = pkt_queue[qhead].coeff;		
      rcm->nackeff = pkt_queue[qhead].nackeff;
#if defined(PKT_SIZE)
        memset(rcm->dummy,200,sizeof(rcm->dummy));
#endif
      if(scounter==0)
      {
           if(rcm->isNACK==1) scounter=1;
           if(rcm->isNACK==0) scounter=pkt_queue[qhead].rem_pkts;
      }
      rcm->mode=mode;
      rcm->source = TOS_NODE_ID;
      rcm->dest = pkt_queue[qhead].dest;
      rcm->rem_pkts=scounter;
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
      	return;
      }
      timer=2; 
      // Radu
//#if defined(LOW_POWER_LISTENING)
     if(mode==LPLM)   call LPL.setRxSleepInterval(&packet, LPL_INTERVAL);
//#endif
//#if defined(RANDOM_DUTY_CYCLING)
      if(mode==RLPLM) call LPL.setRxSleepInterval(&packet, (scounter-1));
//#endif
      //call Leds.led2Toggle();     
      if (call AMSend.send(rcm->dest, &packet, sizeof(radio_count_msg_t)) == SUCCESS) {
	locked = TRUE;
	qhead=(qhead+1)%QUEUE_SIZE1;
        msgcount++;
        dbg("NetworkCoding", "S,%hu,%hu,-1,%hu+Send packet, packet is %hu ~ %hu, queue size %hu.\n",rcm->pkt[0],rcm->pkt[1],call MilliTimer2.getNow(), rcm->pkt[0], rcm->pkt[1], (qtail+QUEUE_SIZE1-qhead)%QUEUE_SIZE1);
      }
    }
  }


  //**************************************************************************
  //**************************************************************************
  //************************************************************************** 
  event void MilliTimer3.fired(){  // NACKs
    uint8_t temp_nack_msg;
    uint8_t sum_nack;
    if(decoded == TOTAL_PKT){
      return;
    }
    if (locked){
      call MilliTimer3.startOneShot(5); 
      return;
    }
    else{
    
      radio_count_msg_t* rcm = (radio_count_msg_t*)call Packet.getPayload(&packet, 0 /*NULL*/);
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
	return;
      }
      rcm->num_pkts = 1;
      rcm->pkt[0] = decoded;
      rcm->pkt[1] = decoded;
      rcm->isNACK = 1;
      rcm->coeff = 0;
      rcm->nackeff = nack_msg[(decoded/8)%CACHE_PAGE];
      if(rcm->nackeff==0)rcm->nackeff=255;
      rcm->source = TOS_NODE_ID;
      rcm->dest = neighbor;
      neighbor = AM_BROADCAST_ADDR;
      rcm->mode=mode;
      rcm->rem_pkts=1;//scounter;
      scounter=1;     
      temp_nack_msg = nack_msg[(decoded/8)%CACHE_PAGE];
#if defined(PKT_SIZE)
        memset(rcm->dummy,200,sizeof(rcm->dummy));
#endif      
      sum_nack = 0;
      while(temp_nack_msg!=0){
	if(temp_nack_msg%2 == 1)
	  sum_nack++;
	temp_nack_msg /= 2;
      }
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
      	return;
      }
//#if defined(LOW_POWER_LISTENING)
      if(mode==LPLM) call LPL.setRxSleepInterval(&packet, LPL_INTERVAL);
//#endif	  
//#if defined(RANDOM_DUTY_CYCLING)
      if(mode==RLPLM) call LPL.setRxSleepInterval(&packet, (scounter-1));
//#endif
      if (call AMSend.send(AM_BROADCAST_ADDR, &packet, sizeof(radio_count_msg_t)) == SUCCESS) {
        locked = TRUE;
        msgcount++;
        Nmsgcount++;
        dbg("NetworkCoding", "N,%hu,%hu,%hu,%hu+Send NACK, packet is %hu ~ %hu.\n",rcm->pkt[0],rcm->pkt[1],sum_nack,call MilliTimer2.getNow(), rcm->pkt[0], rcm->pkt[1]);
        dbg("NACK", "Send NACK, packet is %hu ~ %hu num nack %d %d.\n", rcm->pkt[0], rcm->pkt[1], sum_nack, useful_pkt[(decoded/8)%CACHE_PAGE]);
        if(rcm->dest != AM_BROADCAST_ADDR)
          dbg("NACK", "with receiver %hu to %hu.\n",rcm->source,rcm->dest );
      }
      if(exp < 128)
        exp = exp * 2;
      rand = call Random.rand16();
      rand = rand%NACKBACKOFF;
      call MilliTimer3.startOneShot(exp*NACKTIME + rand);
    }
  }


  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  event void MilliTimer4.fired() {
    //call Leds.led0Toggle();
    if(renack_msg == 0){
      if(TOS_NODE_ID == SOURCE){
        call MilliTimer1.startPeriodic(SOURCETIME);
      }
      else if(((qtail+QUEUE_SIZE1-qhead)%QUEUE_SIZE1) > 0){
        rand = call Random.rand16();
        rand = rand%BACKOFF;
        call MilliTimer2.startPeriodic(10 + rand);
      }
      call MilliTimer4.stop();
      return;
    }
    if(renack_num % 8 == 0){
      call MilliTimer1.stop();
      call MilliTimer2.stop();
      call MilliTimer4.stop();
      call MilliTimer4.startPeriodic(7);
    }  
    while(renack_msg%2 == 0){
      renack_num++;
      renack_msg /= 2;
    }
    if(locked)
      {
       //call MilliTimer4.stop();
       //call MilliTimer4.startPeriodic(7);
      return;
       }
    else{
      radio_count_msg_t* rcm = (radio_count_msg_t*)call Packet.getPayload(&packet, 0 /*NULL*/);
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
        return;
      }
      rcm->num_pkts = 1;
      rcm->pkt[0] = renack_num;
      rcm->pkt[1] = renack_num;
      rcm->isNACK = 2;
      rcm->coeff = 0;
      rcm->nackeff = renack_msg;
#if defined(PKT_SIZE)
        memset(rcm->dummy,200,sizeof(rcm->dummy));
#endif
      tempnack=renack_msg;
       scounter=0;
       while(tempnack>0)
       {
        if(tempnack%2==1)scounter++;
        tempnack/=2;
       } 
      timer=4;
      rcm->source = TOS_NODE_ID;
      rcm->mode=mode;
      rcm->rem_pkts=scounter;
      rcm->dest = AM_BROADCAST_ADDR;
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
        return;
      }
      //#if 0
//#if defined(LOW_POWER_LISTENING)
      if(mode==LPLM)  call LPL.setRxSleepInterval(&packet, LPL_INTERVAL);
//#endif
//#if defined(RANDOM_DUTY_CYCLING)
      if(mode==RLPLM) call LPL.setRxSleepInterval(&packet, (scounter-1));
//#endif
      if (call AMSend.send(rcm->dest, &packet, sizeof(radio_count_msg_t)) == SUCCESS) {
        //call Leds.led0Toggle();
        locked = TRUE;
        msgcount++;
        RNmsgcount++;
        renack_msg /= 2;
        renack_num++;
        dbg("NetworkCoding", "L,%hu,%hu,%hu,%hu+Send ReNACK, packet is %hu ~ %hu.\n",rcm->pkt[0],rcm->pkt[1],decoded,call MilliTimer2.getNow(), rcm->pkt[0], rcm->pkt[1]);
      }
    }
  }

#if defined(ENHANCEMENT)
   event message_t* SerialReceive_CS.receive(message_t* bufPtr,void* payload,
                                          uint8_t len)
   {
      /* All nodes get the optimal coding scheme and hop count */
      /* Source node if received this message twice start MilliTimer1 */
        //call Leds.led2Toggle();
      if(len!=sizeof(control_msg_t)) 
       {
         call Leds.led0Toggle();
         return bufPtr;
       }
      else
      {
        control_msg_t* ctrl_pkt=(control_msg_t*) payload;
        call Leds.led1Toggle();
        if(TOS_NODE_ID==SOURCE && csreceived)
          {
           call Leds.led1Toggle();
           //call MilliTimer1.startPeriodic(SOURCETIME);
          }
        optcoding_scheme=ctrl_pkt->coding_scheme;
        hopcount=ctrl_pkt->hc;  
        csreceived=TRUE;
        return bufPtr;
      }
   } 
#endif
                                     
  /*event message_t* RReceive.receive(message_t* bufPtr, 
				   void* payload, uint8_t len) {
    call Leds.led0Toggle();
    if(mode==RLPLM) return HandlePacket(bufPtr,payload,len);
    return bufPtr;
   } */
  event message_t* Receive.receive(message_t* bufPtr, 
				   void* payload, uint8_t len) {
    call Leds.led1Toggle();
    //if(mode==LPLM) 
          return HandlePacket(bufPtr,payload,len);
    //return bufPtr;
   }

  event void SwitchTimer.fired()
  {
    SendDoneSwitch=TRUE;
   /*atomic{ 
    if(locked)
     {
       
     }
    else
     {
     }
   } */
  } 
  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  message_t* HandlePacket(message_t* bufPtr, 
				   void* payload, uint8_t len) {
    uint8_t i,j,rp;
    uint32_t temp_coeff;

    if(shuffle == 0){
      shuffle = 1;
      call SeedInit.init(call MilliTimer2.getNow());
    }
    
    if (len != sizeof(radio_count_msg_t)) {return bufPtr;}
    /* Unlike original Adapcode a node might defer transmission */
    //if (!locked) 
    {
      radio_count_msg_t* rcv = (radio_count_msg_t*) payload;
          
      if (call Packet.maxPayloadLength() < sizeof(radio_count_msg_t)) {
      	return bufPtr;
      }
     
      if(mode==LPLM && DutyCycling)call StopDutyCycling.NoDutyCycling();
      if(mode==LPLM && !(call SwitchTimer.isRunning()))
        {
           call SwitchTimer.startOneShot(TIME_FOR_FP);
        }
      if(rcv->isNACK==1){  // NACK packet received
	dbg("NACK", "receive NACK, packet is from %d to %d.\n",rcv->source, rcv->dest);
	if(rcv->pkt[0] == decoded && TOS_NODE_ID!=SOURCE){ //same nack
	  call MilliTimer3.stop();
	  rand = call Random.rand16();
	  rand = rand%NACKBACKOFF;
	  call MilliTimer3.startOneShot(exp*NACKTIME + rand);
	}
	else if(rcv->pkt[0] < decoded && renack_msg == 0){ // have those packets
	  //dbg("NetworkCoding", "Response of NACK to %hu.\n", rcv->source);
	    
	  renack_msg = rcv->nackeff;
	  renack_num = rcv->pkt[0];
	  call MilliTimer4.stop();
	  rand = call Random.rand16();
//#if defined(LOW_POWER_LISTENING)
       if(mode==LPLM)
	  rand = rand%NACKBACKOFF+LPL_INTERVAL;
//#else
       else	  rand = rand%NACKBACKOFF;
//#endif
	  call MilliTimer4.startOneShot(rand);
	}
//DEBUG
       else if(TOS_NODE_ID==SOURCE)
       {
         call MilliTimer1.startPeriodic(SOURCETIME);
       } 
      }else{  // coded packets received
	if(TOS_NODE_ID == SOURCE)
       {
//#if defined(RANDOM_DUTY_CYCLING)
    if(mode==RLPLM)
    {
     if(rcv->pkt[0]/8<(counter)/8 && rcv->rem_pkts>1) 
        {
           //call Leds.led2Toggle();
           atomic{
           call LPL.setLocalSleepInterval(((2*rcv->rem_pkts)-3)*RLPL);
           }
          }
    }
//#endif 
	  return bufPtr;
       }
	if(rcv->isNACK == 2 && rcv->pkt[0]/8 == renack_num/8 && renack_msg != 0){ //somoeone is renacking
	  renack_msg = 0;
	   call MilliTimer4.stop();
//#if defined(RANDOM_DUTY_CYCLING)
    if(mode==RLPLM)
    {
     if(((rcv->pkt[0]/8)<((decoded)/8)) && (rcv->rem_pkts>1)) //&& (nackreq!=TRUE))
        {
           //call Leds.led2Toggle();
           atomic{
           call LPL.setLocalSleepInterval(((2*rcv->rem_pkts)-3)*RLPL);
           }
          }
    }
//#endif 
           return bufPtr;
	}
/* FIX LATER ROJA */
//#if defined(RANDOM_DUTY_CYCLING)
    if(mode==RLPLM)
     {
     if(((rcv->pkt[0])/8)<(decoded)/8 && (rcv->rem_pkts>1)) //&& (nackreq!=TRUE))
        {
	if( rcv->isNACK == 0) {
	  if(rcv->pkt[0] < decoded || useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE] >= 8)
	    curNeighbor++;
	    //curNeighbor+=rcv->rem_pkts;
	  else if(rcv->pkt[0] < decoded + 8*CACHE_PAGE)
	    pageNeighbor[(rcv->pkt[0]/8)%CACHE_PAGE]++; } //=rcv->rem_pkts; }
           
           atomic{
           call LPL.setLocalSleepInterval(((2*rcv->rem_pkts)-3)*RLPL);
           }
        }
      }
//#else
    else{
	if((rcv->pkt[0]%8)==0 && rcv->isNACK == 0) {
	  if(rcv->pkt[0] < decoded || useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE] >= 8)
	    curNeighbor++;
	  else if(rcv->pkt[0] < decoded + 8*CACHE_PAGE)
	    pageNeighbor[(rcv->pkt[0]/8)%CACHE_PAGE]++;
	} }
//#endif
	if((rcv->pkt[0]< decoded)){  // useless (previous packets received)
	  return bufPtr;
	}
	if(rcv->pkt[0]>= decoded+8*CACHE_PAGE){  // future packets: drop (and send NACK immediately)
	  //	    neighbor = rcv->source;
  	  call MilliTimer3.stop(); 
	  rand = call Random.rand16();
	  rand = rand%NACKBACKOFF;
	  //if(decoded<8)
	  call MilliTimer3.startOneShot(rand);
	  //      dbg("NetworkCoding", "future packet, curtime is %d, pktnum is %d.\n", call MilliTimer2.getNow(), rcv->pkt[0]);
	  return bufPtr;
	}
	// appropriate packets, reset timer
	  
	exp = 1;
	//	  if(rcv->pkt[0] < decoded + 8){
	call MilliTimer3.stop(); 
	rand = call Random.rand16();
	rand = rand%NACKBACKOFF;
	call MilliTimer3.startOneShot(exp*NACKTIME + rand);
	//    }
	neighbor = rcv->source;
	if(useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE] >= 8)
        {
	  return bufPtr;
        }
	//    dbg("NetworkCoding", "timeout when %d, curtime is %d.\n",exp*NACKTIME + rand, call MilliTimer2.getNow());
	// update structure S
	  
	//	  buffered_msg[rcv->pkt[0]%(8*CACHE_PAGE)] = *bufPtr;
	temp_coeff = rcv->coeff;
	for(i = 0; i < 8; i++){
	  coeff[(rcv->pkt[0]/8)%CACHE_PAGE][useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE]][i] = (uint8_t)(temp_coeff%5);
	  temp_coeff /= 5;
	}
	coeff[(rcv->pkt[0]/8)%CACHE_PAGE][useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE]][rcv->pkt[1]%8] = 1;
	useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE]++;
	for(i = 0; i < useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE];i++){
	  for(j = 0;j < 8;j++){
	    input_n[i][j] = (int32_t)coeff[(rcv->pkt[0]/8)%CACHE_PAGE][i][j];
	    input_d[i][j] = 1;
	    result_n[i][j] = (i==j);
	    result_d[i][j] = 1;
	  }
	}
	useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE] = gaussian(useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE], 8,(rcv->pkt[0]/8)%CACHE_PAGE);
    
	if(useful_pkt[(rcv->pkt[0]/8)%CACHE_PAGE] == 8){  
	  // reset neighbor count
          /* Roja NEW */
          //call MilliTimer3.stop();
          //call MilliTimer3.startOneShot(NACK_NEWPAGE);
	  dbg("NetworkCoding", " Packet count :%hu.\n",rcv->num_pkts);
	  curNeighbor *= 10;
	  pageNeighbor[(rcv->pkt[0]/8)%CACHE_PAGE] *= 10;
	  if(avgNeighbor == 0)
	    avgNeighbor = curNeighbor;
	  else
	    avgNeighbor = (avgNeighbor*2 + curNeighbor + pageNeighbor[(rcv->pkt[0]/8)%CACHE_PAGE])/3;
	  curNeighbor=0;
	  pageNeighbor[(rcv->pkt[0]/8)%CACHE_PAGE] = 0;
	  // determine next coding scheme
#if !defined(ENHANCEMENT)
	  if(avgNeighbor> 120) coding_scheme=8;
	  else if(avgNeighbor > 77) coding_scheme=4;
	  else if(avgNeighbor > 44) coding_scheme = 2;
	  else  coding_scheme = 1;
#else
          coding_scheme=optcoding_scheme;
#endif
	  //		coding_scheme = 2;
	  //    dbg("NetworkCoding", "prepare to send %d\n", coding_scheme);
	  temp_coeff = 0;
	  if(coding_scheme==1){  // no coding
	    for(i=0;i<8;i++){ 
	      enqueue(1, (rcv->pkt[0]/8)*8+i, (rcv->pkt[0]/8)*8+i, 0, AM_BROADCAST_ADDR, 0,0,(8-i));
	    }
	  }else if(coding_scheme==2){
            rp=4;
	    for(i=0;i<8;i++){	
	      if(i%2 == 0)
		temp_coeff=0;
	      if((i+1)%2==0){
		enqueue(2, (rcv->pkt[0]/8)*8 - 1 + i, (rcv->pkt[0]/8)*8 + i, 0, AM_BROADCAST_ADDR, temp_coeff,0,rp);
                rp--;
	      }
	      else{
		rand = call Random.rand16();
		rand = rand%5;
		temp_coeff += rand*power5[i];
	      }
	    }
	  }else if(coding_scheme==4){
            rp=2;
	    for(i=0;i<8;i++){	
	      if(i%4 == 0)
		temp_coeff=0;
	      if((i+1)%4==0){
		enqueue(4, (rcv->pkt[0]/8)*8 - 3 + i, (rcv->pkt[0]/8)*8 + i, 0, AM_BROADCAST_ADDR, temp_coeff,0,rp);
                rp--;
	      }
	      else{
		rand = call Random.rand16();
		rand = rand%5;
		temp_coeff += rand*power5[i];
	      }
	    }
	  }else if(coding_scheme==8){
	    for(i=0;i<8;i++){	
	      if(i%8 == 0)
		temp_coeff=0;
	      if((i+1)%8==0){
		enqueue(8, (rcv->pkt[0]/8)*8 - 7 + i, (rcv->pkt[0]/8)*8 + i, 0, AM_BROADCAST_ADDR, temp_coeff,0,1);
	      }
	      else{
		rand = call Random.rand16();
		rand = rand%5;
		temp_coeff += rand*power5[i];
	      }
	    }
	  }
  	}
  	
	if(useful_pkt[(decoded/8)%CACHE_PAGE] == 8){  //solvable, proceed to next page
    
	  while(useful_pkt[(decoded/8)%CACHE_PAGE] == 8){
	    useful_pkt[(decoded/8)%CACHE_PAGE] = 0;
	    nack_msg[(decoded/8)%CACHE_PAGE] = 255;
	    decoded += 8;
	    if(decoded%8 == 0)
	      {
		dbg("NetworkCoding", "U,%hu,%hu,%hu,%hu+Useful packet decoded.\n",decoded,decoded,rcv->source, call MilliTimer2.getNow());
	      }
	  }
    if(decoded == TOTAL_PKT ){
#if defined(DEBUG)
      /*radio_count_msg_t* rcm = (radio_count_msg_t*) call Packet.getPayload(&packet1, 0 );
        if(uartlocked)
          {
           call Leds.led2Toggle();
           //return;
          }
        else{
        rcm->num_pkts=6;
        rcm->source=TOS_NODE_ID;
        rcm->pkt[0]=msgcount;
        rcm->pkt[1]=Nmsgcount;
        rcm->coeff=RNmsgcount;
        if(call AMSendS.send(AM_BROADCAST_ADDR, &packet1, sizeof(radio_count_msg_t)) == SUCCESS) {
            uartlocked=TRUE;
         }
        else
         {
           call MilliTimer5.startPeriodic(200);
         } } */
#endif
      //enqueue(6, msgcount, 0, 8, AM_BROADCAST_ADDR, 0, 0,0);
      //enqueue(0, 0, 0, 4, AM_BROADCAST_ADDR, time, 0);
      call Leds.led0On();
      call MilliTimer3.stop();
     } 
	  for(i = 0; i < 8;i++)
	    for(j = 0;j < 8;j++){
	    input_n[i][j] = 0;
	    input_d[i][j] = 1;
	  }
	}
  	
  	
      }
    }


    return bufPtr;
  }

  /*void copytouqueue(radio_count_msg_t* orcm)
  {
    radio_count_msg_t* rcm = &pkt_queue[utail];
    memcpy(rcm,orcm,sizeof(rcm));
    utail=(utail+1)%QUEUE_SIZE1;
    if(utail==((uhead+1)%QUEUE_SIZE1)){
     call UTimer.startPeriodic(
    } 
  } */

  /*event void RAMSend.sendDone(message_t* bufPtr, error_t error) {
    //dbg("NetworkCoding", "Radu : sendDone");
   if(mode==RLPLM) SendDone(bufPtr,error); 
  } */
  //**************************************************************************
  //**************************************************************************
  //**************************************************************************
  event void AMSend.sendDone(message_t* bufPtr, error_t error) {
    //dbg("NetworkCoding", "Radu : sendDone");
   if(mode==LPLM) SendDone(bufPtr,error); 
    /*if (&packet == bufPtr) {
      dbg("NetworkCoding", "X,0,0,0,0+Finish sending, current time is %hu.\n", call MilliTimer1.getNow());
     {
     locked = FALSE;
     scounter--;
     if(scounter>0)
     {
       //post send_next();
        switch(timer)
        {
          case 1://call MilliTimer1.stop();
                 // ROJA changed OneShot to Periodic 
                 call MilliTimer1.startOneShot(3);
                 break;
          case 2://call MilliTimer2.stop();
                 call MilliTimer2.startOneShot(3);
                 break;
          case 4://call MilliTimer4.stop();
                 call MilliTimer4.startOneShot(3);
                 break;
        } 
     }
     //else {signaldone=FALSE; } 
    }
    }
    else{
      dbg("NetworkCoding", "ridiculous\n");
    }*/
  }

  void SendDone(message_t* bufPtr, error_t error)
  {
    //dbg("NetworkCoding", "Radu : sendDone");
    if (&packet == bufPtr) {
      dbg("NetworkCoding", "X,0,0,0,0+Finish sending, current time is %hu.\n", call MilliTimer1.getNow());
     {
     if(mode==RLPLM)
     {
     scounter--;
     if(scounter>0)
     {
       //post send_next();
        switch(timer)
        {
          case 1://call MilliTimer1.stop();
                 // ROJA changed OneShot to Periodic 
                 call MilliTimer1.startOneShot(3);
                 break;
          case 2://call MilliTimer2.stop();
                 call MilliTimer2.startOneShot(3);
                 break;
          case 4://call MilliTimer4.stop();
                 call MilliTimer4.startOneShot(3);
                 break;
        } 
     }
    }}
     /* First change the mode and then changed locked */
     if(SendDoneSwitch) {
                          call StopDutyCycling.SwitchToRlpl();
                        }
     else locked = FALSE;
    }
    else{
      dbg("NetworkCoding", "ridiculous\n");
    }
   
  }

  /* task void send_next()
  {
          switch(timer)
        {
          case 1:call MilliTimer1.stop();
                 call MilliTimer1.startOneShot(0);
                 break;
          case 2:call MilliTimer2.stop();
                 call MilliTimer2.startOneShot(0);
                 break;
          case 4:call MilliTimer4.stop();
                 call MilliTimer4.startOneShot(0);
                 break;
        }
 
  } */

  
   /* Roja Feb 10 */
     event void AMSendS.sendDone(message_t* bufPtr, error_t error) {
     dbg("NetworkCoding", "ridiculous\n");
     /*if(TOS_NODE_ID==SOURCE)
     {
     locked = FALSE;
     scounter--;
     } */
     uartlocked=FALSE; 
     /*if(scounter>0)
     {
       //post send_next();
        switch(timer)
        {
          case 1:call MilliTimer1.stop();
                 // ROJA changed OneShot to Periodic 
                 call MilliTimer1.startOneShot(0);
                 break;
          case 2:call MilliTimer2.stop();
                 call MilliTimer2.startOneShot(0);
                 break;
          case 4:call MilliTimer4.stop();
                 call MilliTimer4.startOneShot(0);
                 break;
        } 
     } */
     }
     event void AMControlS.startDone(error_t error) {
     if (error == SUCCESS) {
     dbg("NetworkCoding", "AMContols startDone\n");
     }
     else
     {
     call AMControlS.start();
     }
     }
     event void AMControlS.stopDone(error_t error) {
     dbg("NetworkCoding", "AMContols stopDone\n");
     }
     /* Roja Feb 10 */
     /* async event void RadioBackoff.requestCca(message_t *msg) {
      if(signaldone)
        call RadioBackoff.setCca(FALSE);
     }
    async event void RadioBackoff.requestInitialBackoff(message_t *msg) {
     }
    async event void RadioBackoff.requestCongestionBackoff(message_t *msg) {
    } */

     event void StopDutyCycling.ChangetoRlpl()
    {
     SendDoneSwitch=FALSE;
     mode=RLPLM;
     locked=FALSE;
     //DutyCycling=FALSE;
    }
     event void StopDutyCycling.StopSR()
    {
     DutyCycling=FALSE;
    }
}
