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
 * The Active Message layer for the CC2420 radio. This configuration
 * just layers the AM dispatch (CC2420ActiveMessageM) on top of the
 * underlying CC2420 radio packet (CC2420CsmaCsmaCC), which is
 * inherently an AM packet (acknowledgements based on AM destination
 * addr and group). Note that snooping may not work, due to CC2420
 * early packet rejection if acknowledgements are enabled.
 *
 * @author Philip Levis
 * @author David Moss
 * @version $Revision: 1.2 $ $Date: 2010/06/03 00:29:34 $
 */

#include "RCC2420.h"
#include "AM.h"

configuration WrapperC {
  provides {
    interface SplitControl;
    interface AMSend;
    interface Receive;
    interface Packet;
    interface LowPowerListening;
    interface Mode;
    interface ModeS;
    interface StopDutyCycling;
  }
}
implementation {

 components WrapperP;
 SplitControl = WrapperP; 
 AMSend = WrapperP;
 Receive = WrapperP;
 Packet = WrapperP;
 LowPowerListening = WrapperP;
 Mode = WrapperP;
 ModeS = WrapperP;

 components CC2420ActiveMessageC;
 //StopDutyCycling = CC2420ActiveMessageC;
 WrapperP.lplSend -> CC2420ActiveMessageC.AMSend[10];
 WrapperP.lplReceive -> CC2420ActiveMessageC.Receive[10];
 WrapperP.lplPacket -> CC2420ActiveMessageC.Packet;
 WrapperP.lplLPL -> CC2420ActiveMessageC.LowPowerListening;
 WrapperP.lplControl -> CC2420ActiveMessageC.SplitControl;  
 WrapperP.AMPacket -> CC2420ActiveMessageC;

 components RCC2420ActiveMessageC;
 WrapperP.rlplSend -> RCC2420ActiveMessageC.AMSend[10];
 WrapperP.rlplReceive -> RCC2420ActiveMessageC.Receive[10];
 WrapperP.rlplPacket -> RCC2420ActiveMessageC.Packet;
 WrapperP.rlplLPL -> RCC2420ActiveMessageC.LowPowerListening;
 WrapperP.rlplControl -> RCC2420ActiveMessageC.SplitControl;  
 
 components DefaultLplC as LplC;
 StopDutyCycling = LplC; 
 components LedsC;
 WrapperP.Leds -> LedsC;
}
