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
 * @version $Revision: 1.1 $ $Date: 2010/06/01 01:01:43 $
 */

#include "RCC2420.h"
#include "AM.h"

configuration RCC2420ActiveMessageC {
  provides {
    interface SplitControl;
    interface AMSend[am_id_t id];
    interface Receive[am_id_t id];
    interface Receive as Snoop[am_id_t id];
    interface AMPacket;
    interface Packet;
    interface CC2420Packet;
    interface PacketAcknowledgements;
    interface LinkPacketMetadata;
    interface RadioBackoff[am_id_t amId];
    interface LowPowerListening;
    interface PacketLink;
    interface SendNotifier[am_id_t amId];
  }
}
implementation {

  components RCC2420ActiveMessageP as AM;
  components RCC2420CsmaC as CsmaC;
  components ActiveMessageAddressC;
  components RUniqueSendC;
  components RUniqueReceiveC;
  components RCC2420TinyosNetworkC;
  components RCC2420PacketC;
  components RandomC;
  components CC2420ControlC;
  components WrapperC;
  
//#if defined(LOW_POWER_LISTENING) || defined(ACK_LOW_POWER_LISTENING)
  //components DefaultLplC as LplC;
//#elif defined(RANDOM_DUTY_CYCLING) 
  components RandomLplC as LplC;
//#else
//  components DummyLplC as LplC;
//#endif 
  components LedsC;

#if defined(PACKET_LINK)
  components RPacketLinkC as LinkC;
#else
  components RPacketLinkDummyC as LinkC;
#endif

  
  RadioBackoff = AM;
  Packet = AM;
  AMSend = AM;
  SendNotifier = AM;
  Receive = AM.Receive;
  Snoop = AM.Snoop;
  AMPacket = AM;
  PacketLink = LinkC;
  LowPowerListening = LplC;
  CC2420Packet = RCC2420PacketC;
  PacketAcknowledgements = RCC2420PacketC;
  LinkPacketMetadata = RCC2420PacketC;
  AM.Random->RandomC.Random;
  // SplitControl Layers
  SplitControl = LplC;
  LplC.SubControl -> CsmaC;
  
  // Send Layers
  AM.SubSend -> RUniqueSendC;
  RUniqueSendC.SubSend -> LinkC;
  LinkC.SubSend -> LplC.Send;
  LplC.SubSend -> RCC2420TinyosNetworkC.Send;
  RCC2420TinyosNetworkC.SubSend -> CsmaC;
  
  // Receive Layers
  AM.SubReceive -> LplC;
  LplC.SubReceive -> RUniqueReceiveC.Receive;
  RUniqueReceiveC.SubReceive -> RCC2420TinyosNetworkC.Receive;
  RCC2420TinyosNetworkC.SubReceive -> CsmaC;

  AM.ActiveMessageAddress -> ActiveMessageAddressC;
  AM.CC2420Packet -> RCC2420PacketC;
  AM.CC2420PacketBody -> RCC2420PacketC;
  AM.CC2420Config -> CC2420ControlC;
  AM.Leds -> LedsC;
  AM.SubBackoff -> CsmaC;
  AM.Mode -> WrapperC;
}
