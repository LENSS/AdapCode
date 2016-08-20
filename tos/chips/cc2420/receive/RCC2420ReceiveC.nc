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
 * Implementation of the receive path for the ChipCon CC2420 radio.
 *
 * @author Jonathan Hui <jhui@archrock.com>
 * @version $Revision: 1.1 $ $Date: 2010/06/01 01:01:44 $
 */

configuration RCC2420ReceiveC {

  provides interface StdControl;
  provides interface CC2420Receive;
  provides interface Receive;
  provides interface ReceiveIndicator as PacketIndicator;
  provides interface SetBackoff;
}

implementation {
  components MainC;
  components RCC2420ReceiveP;
  components RCC2420PacketC;
  components new CC2420SpiC() as Spi;
  components CC2420ControlC;
  
  components HplCC2420PinsC as Pins;
  components HplCC2420InterruptsC as InterruptsC;

  components LedsC as Leds;
  RCC2420ReceiveP.Leds -> Leds;

  StdControl = RCC2420ReceiveP;
  CC2420Receive = RCC2420ReceiveP;
  SetBackoff=RCC2420ReceiveP;
  Receive = RCC2420ReceiveP;
  PacketIndicator = RCC2420ReceiveP.PacketIndicator;

  MainC.SoftwareInit -> RCC2420ReceiveP;
  
  RCC2420ReceiveP.CSN -> Pins.CSN;
  RCC2420ReceiveP.FIFO -> Pins.FIFO;
  RCC2420ReceiveP.FIFOP -> Pins.FIFOP;
  RCC2420ReceiveP.InterruptFIFOP -> InterruptsC.InterruptFIFOP;
  RCC2420ReceiveP.SpiResource -> Spi;
  RCC2420ReceiveP.RXFIFO -> Spi.RXFIFO;
  RCC2420ReceiveP.SFLUSHRX -> Spi.SFLUSHRX;
  RCC2420ReceiveP.SACK -> Spi.SACK;
  RCC2420ReceiveP.CC2420Packet -> RCC2420PacketC;
  RCC2420ReceiveP.CC2420PacketBody -> RCC2420PacketC;
  RCC2420ReceiveP.PacketTimeStamp -> RCC2420PacketC;
  RCC2420ReceiveP.CC2420Config -> CC2420ControlC;

  components WrapperC;
  RCC2420ReceiveP.Mode -> WrapperC;
}
