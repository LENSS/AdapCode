/*
 * Copyright (c) 2005-2006 Rincon Research Corporation
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 * - Neither the name of the Rincon Research Corporation nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * RINCON RESEARCH OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE
 */

/**
 * Low Power Listening for the CC2420
 * @author David Moss
 */


#include "DefaultLpl.h"
#warning "*** USING RANDOM LOW POWER COMMUNICATIONS ***"

configuration RandomLplC {
  provides {
    interface LowPowerListening;
    interface Send;
    interface Receive;
    interface SplitControl;
    interface State as SendState;
    //interface DutySwitch;
  }
  
  uses { 
    interface Send as SubSend;
    interface Receive as SubReceive;
    interface SplitControl as SubControl;
  }
}

implementation {
  components MainC,
      RandomLplP,
      RPowerCycleC,
      RCC2420ActiveMessageC,
      RCC2420CsmaC,
      RCC2420TransmitC,
      RCC2420PacketC,
      RandomC,
      new StateC() as SendStateC,
      //new TimerMilliC() as OffTimerC,
      new TimerMilliC() as SendDoneTimerC,
      new TimerMilliC() as SendRetryTimerC,
      new TimerMilliC() as noSendTimerC,
      LedsC;
  
  LowPowerListening = RandomLplP;
  Send = RandomLplP;
  Receive = RandomLplP;
  SplitControl = RPowerCycleC;
  SendState = SendStateC;
  
  SubControl = RandomLplP.SubControl;
  SubReceive = RandomLplP.SubReceive;
  SubSend = RandomLplP.SubSend;
  //DutySwitch = RandomLplP.DutySwitch;
  
  MainC.SoftwareInit -> RandomLplP;
  
  RandomLplP.PendingMsg -> RPowerCycleC.PendingMsg;
  RandomLplP.PM -> RCC2420TransmitC.PendingMsg;
  RandomLplP.SplitControlState -> RPowerCycleC.SplitControlState;
  RandomLplP.RadioPowerState -> RPowerCycleC.RadioPowerState;
  RandomLplP.SendState -> SendStateC;
  //RandomLplP.OffTimer -> OffTimerC;
  RandomLplP.SendDoneTimer -> SendDoneTimerC;
  RandomLplP.SendRetryTimer -> SendRetryTimerC;
  RandomLplP.noSendTimer -> noSendTimerC;
  RandomLplP.PowerCycle -> RPowerCycleC;
  RandomLplP.Resend -> RCC2420TransmitC;
  RandomLplP.ValSleepReq -> RCC2420TransmitC.ValSleepReq;
  RandomLplP.CC2420PacketDefer-> RCC2420TransmitC;
  RandomLplP.PacketAcknowledgements -> RCC2420ActiveMessageC;
  RandomLplP.AMPacket -> RCC2420ActiveMessageC;
  RandomLplP.CC2420PacketBody -> RCC2420PacketC;
  RandomLplP.RadioBackoff -> RCC2420CsmaC;
  RandomLplP.Random -> RandomC;
  RandomLplP.Leds -> LedsC;
 
  components WrapperC;
  RandomLplP.Mode -> WrapperC;  
}
