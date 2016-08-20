/*
 * "Copyright (c) 2005 Stanford University. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and
 * its documentation for any purpose, without fee, and without written
 * agreement is hereby granted, provided that the above copyright
 * notice, the following two paragraphs and the author appear in all
 * copies of this software.
 * 
 * IN NO EVENT SHALL STANFORD UNIVERSITY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN
 * IF STANFORD UNIVERSITY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 * 
 * STANFORD UNIVERSITY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE
 * PROVIDED HEREUNDER IS ON AN "AS IS" BASIS, AND STANFORD UNIVERSITY
 * HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS."
 */


/**
 * Active message implementation on top of the CC2420 radio. This
 * implementation uses the 16-bit addressing mode of 802.15.4: the
 * only additional byte it adds is the AM id byte, as the first byte
 * of the data payload.
 *
 * @author Philip Levis
 * @version $Revision: 1.2 $ $Date: 2010/06/03 00:29:34 $
 */
 
#include "CC2420.h"

module WrapperP @safe() {
  provides {
    interface AMSend;
    interface Receive;
    interface SplitControl;
    interface Packet;
    interface LowPowerListening;
    interface ModeS;
    interface Mode;
  }
  
  uses {
/********** RLPL ******************/
    interface AMSend as rlplSend;
    interface Receive as rlplReceive;
    interface Packet as rlplPacket;
    interface LowPowerListening as rlplLPL;
    interface SplitControl as rlplControl; 

/***********LPL *******************/
    interface AMSend as lplSend;
    interface Receive as lplReceive;
    interface Packet as lplPacket;
    interface LowPowerListening as lplLPL;
    interface SplitControl as lplControl;
    interface AMPacket;
    interface Leds;
  }
}
implementation {

  uint8_t mode=LPLM;
  bool appInitiated = FALSE;
  bool requested = FALSE;
  bool pendingSD = FALSE;
  //bool pendingpS = FALSE;
  bool modeSwitch = FALSE;
  bool noSend = FALSE;
  am_addr_t paddr;
  message_t* pmsg;
  uint8_t plen;
  bool lplsleeping = FALSE;
  uint16_t lplSI;

  void Switch();

  /***************** AMSend Commands ****************/
  command error_t AMSend.send(am_addr_t addr,
					  message_t* msg,
					  uint8_t len) {
  error_t res;
  atomic{
  if(noSend) return FAIL;
  if(mode==RLPLM)
    {
     res=call rlplSend.send(addr,msg,len);
     if(res==SUCCESS) pendingSD = TRUE;
     return res;
    }

  else if(mode==LPLM)
    {
     res=call lplSend.send(addr,msg,len); 
     if(res==SUCCESS) pendingSD = TRUE;
     return res;
    }
  else return FAIL;
   }
  }

  command error_t AMSend.cancel(message_t* msg) {

  atomic{
  if(mode==RLPLM)
     return call rlplSend.cancel(msg);


  else if(mode==LPLM) 
     return call lplSend.cancel(msg);

  else return FAIL;
   }
  }

  command uint8_t AMSend.maxPayloadLength() {

  atomic{
  if(mode==RLPLM)
     return call rlplSend.maxPayloadLength();


  else if(mode==LPLM) 
     return call lplSend.maxPayloadLength();
  else return 0;
   }

  }

  command void* AMSend.getPayload(message_t* m, uint8_t len) {

  atomic{
  if(mode==RLPLM)
     return call rlplSend.getPayload(m,len);


  else if(mode==LPLM) 
     return call lplSend.getPayload(m,len);

  else return NULL;
   }
  }

  /***************** Packet Commands ****************/
  command void Packet.clear(message_t* msg) {

  atomic{
  if(mode==RLPLM)
     call rlplPacket.clear(msg);


  else if(mode==LPLM)
     call lplPacket.clear(msg);

   }
  
}
  
  command uint8_t Packet.payloadLength(message_t* msg) {

  atomic{
  if(mode==RLPLM)
     return call rlplPacket.payloadLength(msg);


  else if(mode==LPLM)
     return call lplPacket.payloadLength(msg);

    //return (call CC2420PacketBody.getHeader(msg))->length - CC2420_SIZE;
  else return 0;
   }
  }
  
  command void Packet.setPayloadLength(message_t* msg, uint8_t len) {

  atomic{
  if(mode==RLPLM)
     call rlplPacket.setPayloadLength(msg,len);


  else  if(mode==LPLM)
     call lplPacket.setPayloadLength(msg,len);

    //(call CC2420PacketBody.getHeader(msg))->length  = len + CC2420_SIZE;
   }
  }
  
  command uint8_t Packet.maxPayloadLength() {
    return TOSH_DATA_LENGTH;
  }
  
  command void* Packet.getPayload(message_t* msg, uint8_t len) {

  atomic{
  if(mode==RLPLM)
     return call rlplPacket.getPayload(msg,len);


  else  if(mode==LPLM)
     return call lplPacket.getPayload(msg,len);

  else return NULL;
   }
  }

  
  /***************** SubSend Events ****************/

  event void lplSend.sendDone(message_t* msg, error_t result) {
    pendingSD = FALSE;
    signal AMSend.sendDone(msg, result);
    if(requested)
       { 
         requested = FALSE;
         Switch();
  }}


  event void rlplSend.sendDone(message_t* msg, error_t result) {
    pendingSD = FALSE;
    //call Leds.led0Toggle();
    signal AMSend.sendDone(msg, result);
    if(requested) 
      {
       requested = FALSE;
       Switch();
      }
  } 

  
  /***************** SubReceive Events ****************/

  event message_t* lplReceive.receive(message_t* msg, void* payload, uint8_t len) {
      //call Leds.led2Toggle();
      return signal Receive.receive(msg, payload, len);
  }


  event message_t* rlplReceive.receive(message_t* msg, void* payload, uint8_t len) {
      //call Leds.led1Toggle();
      return signal Receive.receive(msg, payload, len);
  } 

  
  /***************** Defaults ****************/
  default event message_t* Receive.receive(message_t* msg, void* payload, uint8_t len) {
    return msg;
  }
  
  default event void AMSend.sendDone(message_t* msg, error_t err) {
  }

 /***********************************************/
 command void LowPowerListening.setLocalSleepInterval(uint16_t sleepIntervalMs)
 {

  atomic{
  if(mode==RLPLM)
     call rlplLPL.setLocalSleepInterval(sleepIntervalMs);


  else if(mode==LPLM)
    {
     lplsleeping = TRUE;
     lplSI = sleepIntervalMs;
     call lplLPL.setLocalSleepInterval(sleepIntervalMs);
    }
   }
  }
 command uint16_t LowPowerListening.getLocalSleepInterval()
 {

  atomic{
  if(mode==RLPLM)
     return call rlplLPL.getLocalSleepInterval();


  else  if(mode==LPLM)
     return call lplLPL.getLocalSleepInterval();

  else return 0;
   }
 }
  command void LowPowerListening.setLocalDutyCycle(uint16_t dutyCycle)
 {

  atomic{
   if(mode==RLPLM)
      call rlplLPL.setLocalDutyCycle(dutyCycle);


  else if(mode==LPLM)
     call lplLPL.setLocalDutyCycle(dutyCycle);

   }
 }
  command uint16_t LowPowerListening.getLocalDutyCycle()
 {

  atomic{
   if(mode==RLPLM)
     return call rlplLPL.getLocalDutyCycle();


  else if(mode==LPLM)
     return call lplLPL.getLocalDutyCycle();

  else return 0;
   }
 }
  command void LowPowerListening.setRxSleepInterval(message_t *msg, uint16_t sleepIntervalMs)
 {

  atomic{
   if(mode==RLPLM)
     call rlplLPL.setRxSleepInterval(msg,sleepIntervalMs);


  else  if(mode==LPLM)
     call lplLPL.setRxSleepInterval(msg,sleepIntervalMs);

   }
 }
  command uint16_t LowPowerListening.getRxSleepInterval(message_t *msg)
 {

  atomic{
  if(mode==RLPLM)
     return call rlplLPL.getRxSleepInterval(msg);


  else if(mode==LPLM)
     return call lplLPL.getRxSleepInterval(msg);

  else return 0;
   }
 }
  command void LowPowerListening.setRxDutyCycle(message_t *msg, uint16_t dutyCycle)
 {

  atomic{
  if(mode==RLPLM)
     call rlplLPL.setRxDutyCycle(msg,dutyCycle);


  else if(mode==LPLM)
     call lplLPL.setRxDutyCycle(msg,dutyCycle);

   }
 }
  command uint16_t LowPowerListening.getRxDutyCycle(message_t *msg)
 {

  atomic{
  if(mode==RLPLM)
     return call rlplLPL.getRxDutyCycle(msg);


  else if(mode==LPLM)
     return call lplLPL.getRxDutyCycle(msg);

  else return 0;
   }
 }
  command uint16_t LowPowerListening.dutyCycleToSleepInterval(uint16_t dutyCycle)
 {

  atomic{
  if(mode==RLPLM)
     return call rlplLPL.dutyCycleToSleepInterval(dutyCycle);


  else if(mode==LPLM)
     return call lplLPL.dutyCycleToSleepInterval(dutyCycle);

  else return 0;
   }
 }
  command uint16_t LowPowerListening.sleepIntervalToDutyCycle(uint16_t sleepInterval)
 {

  atomic{
  if(mode==RLPLM)
     return call rlplLPL.sleepIntervalToDutyCycle(sleepInterval);


  else if(mode==LPLM)
     return call lplLPL.sleepIntervalToDutyCycle(sleepInterval);

  else return 0;
   }
 }
/*******************************************************************/
command error_t SplitControl.start() {
/* First call CC2420's and then RCC2420's */
if(modeSwitch) return FAIL;
atomic{
 appInitiated = TRUE;
 if(mode==LPLM)
   return call lplControl.start();
 else  if(mode==RLPLM)
   return call rlplControl.start(); 
 else return FAIL;
}}

event void lplControl.startDone(error_t error)
{
  //call rlplControl.start();
atomic{
 if(mode==LPLM)
  {
    if(appInitiated)
    {
     appInitiated = FALSE;
     signal SplitControl.startDone(error);
    }
  else if(modeSwitch)
    {
      modeSwitch = FALSE;
      /*if(mode == LPLM) mode = RLPLM;
      else if(mode == RLPLM) mode = LPLM; */
      noSend = FALSE;
      requested = FALSE;
      signal ModeS.ModeSwitched(mode);
      if(lplsleeping && lplSI > 0)
       call lplLPL.setLocalSleepInterval(lplSI);
    }
  }
}}

event void rlplControl.startDone(error_t error)
{
atomic{
 if(mode==RLPLM)
  {
   if(appInitiated)
    {
    appInitiated = FALSE;
    signal SplitControl.startDone(error);
    }
  else if(modeSwitch)
    {
      modeSwitch = FALSE;
     /* if(mode == LPLM) mode = RLPLM;
      else if(mode == RLPLM) mode = LPLM; */
      noSend = FALSE;
      requested = FALSE;
      signal ModeS.ModeSwitched(mode);
      
    }
  }
}} 

command error_t SplitControl.stop()
{
if(modeSwitch) return FAIL;
atomic{
 appInitiated = TRUE;
 if(mode==LPLM)
  return call lplControl.stop();
   //return call lplControl.start();
 else if(mode==RLPLM)
  return call rlplControl.stop(); 
 else return FAIL;
}}

event void lplControl.stopDone(error_t error)
{
  //call rlplControl.stop();
//call Leds.led0Toggle();
atomic{
 if(mode==LPLM)
 {
  if(appInitiated)
    {
    appInitiated = FALSE;
    signal SplitControl.stopDone(error);
    }
  else if(modeSwitch)
    {
     if(mode == LPLM) mode = RLPLM;
     else if(mode == RLPLM) mode = LPLM;
     call rlplControl.start();
    }
  /* else error */
 }
}}

event void rlplControl.stopDone(error_t error)
{
//call Leds.led1Toggle();
atomic{
 if(mode==RLPLM)
  {
  if(appInitiated)
  {
    appInitiated = FALSE;
    signal SplitControl.stopDone(error);
  }
  else if(modeSwitch)
    {
     if(mode == LPLM) mode = RLPLM;
     else if(mode == RLPLM) mode = LPLM;
     call lplControl.start();
    }
  }
}} 


 async command void Mode.switchMode(uint8_t reqMode)
{
atomic{
 if((mode==LPLM && reqMode == RLPLM) || (mode==RLPLM && reqMode ==LPLM))
 {
  noSend=TRUE;
}
 modeSwitch = TRUE; 
 if(pendingSD)
  {
   requested = TRUE;
  }
  else
  {
   Switch();
  }
} }

 async command void Mode.setMode(uint8_t reqMode)
{
 atomic mode=reqMode;
} 

async command uint8_t Mode.getMode()
{
 atomic return mode;
}

void Switch()
{
 if(mode == LPLM)
  {
   /* Call lpl stop and rlpl start */
   //call Leds.led2Toggle();
   call lplControl.stop();
  }
 else if(mode == RLPLM)
 {
   call rlplControl.stop();
 }
}
}
