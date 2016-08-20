/*
 * Copyright (c) 2005-2006 Arch Rock Corporation
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
 * - Neither the name of the Arch Rock Corporation nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * ARCHED ROCK OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE
 */

/**
 * Basic implementation of a CSMA MAC for the ChipCon CC2420 radio.
 *
 * @author Jonathan Hui <jhui@archrock.com>
 * @version $Revision: 1.1 $ $Date: 2010/06/01 01:01:43 $
 */

#include "RCC2420.h"
#include "IEEE802154.h"

configuration RCC2420CsmaC {

  provides interface SplitControl;
  provides interface Send;
  provides interface Receive;
  provides interface RadioBackoff;

}

implementation {

  components RCC2420CsmaP as CsmaP;
  RadioBackoff = CsmaP;
  SplitControl = CsmaP;
  Send = CsmaP;
  
  components CC2420ControlC;
  CsmaP.Resource -> CC2420ControlC;
  CsmaP.CC2420Power -> CC2420ControlC;

  components RCC2420TransmitC;
  CsmaP.SubControl -> RCC2420TransmitC;
  CsmaP.CC2420Transmit -> RCC2420TransmitC;
  CsmaP.SubBackoff -> RCC2420TransmitC;
  CsmaP.ValSleepReq -> RCC2420TransmitC;
  CsmaP.CC2420PacketDefer -> RCC2420TransmitC;

  components RCC2420ReceiveC;
  Receive = RCC2420ReceiveC;
  CsmaP.SubControl -> RCC2420ReceiveC;

  components RCC2420PacketC;
  CsmaP.CC2420Packet -> RCC2420PacketC;
  CsmaP.CC2420PacketBody -> RCC2420PacketC;
  
  components RandomC;
  CsmaP.Random -> RandomC;

  components new StateC();
  CsmaP.SplitControlState -> StateC;
  
  components LedsC as Leds;
  CsmaP.Leds -> Leds;
 
  components WrapperC;
  CsmaP.Mode -> WrapperC; 
}