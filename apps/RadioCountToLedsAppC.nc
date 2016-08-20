// $Id: RadioCountToLedsAppC.nc,v 1.6 2010/06/02 23:34:26 rojac Exp $

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
 
#include "RadioCountToLeds.h"
//#define RANDOM_DUTY_CYCLING
/**
 * Configuration for the RadioCountToLeds application. RadioCountToLeds 
 * maintains a 4Hz counter, broadcasting its value in an AM packet 
 * every time it gets updated. A RadioCountToLeds node that hears a counter 
 * displays the bottom three bits on its LEDs. This application is a useful 
 * test to show that basic AM communication and timers work.
 *
 * @author Philip Levis
 * @date   June 6 2005
 */

  
configuration RadioCountToLedsAppC {}
implementation {
  components MainC, RadioCountToLedsC as App, LedsC;

  //  components new AMSenderC(AM_RADIO_COUNT_MSG);
  //  components new AMReceiverC(AM_RADIO_COUNT_MSG);

  /* Roja Feb 10 */
  components new CounterToLocalTimeC(TMilli) as CLT;
  //components AlarmCounterMilliP;
  components CounterMilli32C, TopoBuilderC;
  //components Msp430CounterMicroC;
  components SerialActiveMessageC;
  //components new SerialAMSenderC(AM_RADIO_COUNT_MSG);
  components new SerialAMSenderC(160);

  /* Roja Feb 10 */
  components new TimerMilliC() as Timer1;
  components new TimerMilliC() as Timer2;
  components new TimerMilliC() as Timer3;
  components new TimerMilliC() as Timer4;
#if defined(DEBUG)
  components new TimerMilliC() as Timer5;
  components new TimerMilliC() as Timer6;
#endif
  components new TimerMilliC() as STimer;
  components new TimerMilliC() as BTimer;
  //components new TimerMilliC() as PostponeTimer;
  components CC2420ActiveMessageC;
  components new TimerMilliC() as TopoTimer1;
  components RandomC;
 
  App.Boot -> MainC.Boot;

  App.SplitControl -> CC2420ActiveMessageC;  
  App.AMSend -> CC2420ActiveMessageC.AMSend[10];
  App.Receive -> CC2420ActiveMessageC.Receive[10];
  App.Packet -> CC2420ActiveMessageC;
 App.LPL ->  CC2420ActiveMessageC;

  //components RCC2420ActiveMessageC;
  //App.RSplitControl -> RCC2420ActiveMessageC;  
  /*App.RAMSend -> RCC2420ActiveMessageC.AMSend[10];
  App.RReceive -> RCC2420ActiveMessageC.Receive[10];
  App.RPacket -> RCC2420ActiveMessageC;
 App.RandLPL ->  RCC2420ActiveMessageC; */

  //App.RadioBackoff->CC2420ActiveMessageC.RadioBackoff[12];
//#if defined(LOW_POWER_LISTENING) || defined(RANDOM_DUTY_CYCLING)
//#endif
#if defined(ENHANCEMENT)
 App.SerialReceive_CS -> SerialActiveMessageC.Receive[162];
 App.TopoInit -> TopoBuilderC;
#endif
  App.Leds -> LedsC;
  App.MilliTimer1 -> Timer1;
  App.MilliTimer2 -> Timer2;
  App.MilliTimer3 -> Timer3;
  App.MilliTimer4 -> Timer4;
#if defined(DEBUG)
  App.MilliTimer5 -> Timer5;
  App.MilliTimer6 -> Timer6;
#endif
  App.SwitchTimer -> STimer;
  App.BlinkTimer -> BTimer;
  //App.ppTimer->PostponeTimer;
  App.Random->RandomC.Random;
  App.SeedInit->RandomC.SeedInit;
  
   //components DefaultLplC;
  //App.StopDutyCycling -> DefaultLplC;
  App.StopDutyCycling -> CC2420ActiveMessageC;
  // MKS
  //App.LPL->DefaultLplC.LowPowerListening;AC
  /* Roja Feb 10 */

  //App.Init->AlarmCounterMilliP;
  //App.Init->AlarmCounterMilliP;

  CLT.Counter->CounterMilli32C;
  App.AMSendS -> SerialAMSenderC;
  App.AMControlS -> SerialActiveMessageC;

  /* TopoBuilder */
  TopoBuilderC.SerialReceive_StartBeacon -> SerialActiveMessageC.Receive[161];
  TopoBuilderC.SerialReceive_DumpNbrTbl -> SerialActiveMessageC.Receive[160];
  //TopoBuilderC.SerialSendNbrTbl -> SerialActiveMessageC.AMSend[160];
  TopoBuilderC.SerialSendNbrTbl -> SerialAMSenderC;

    TopoBuilderC.ReceiveBeaconMsg -> CC2420ActiveMessageC.Receive[12];
  TopoBuilderC.SendBeaconMsg -> CC2420ActiveMessageC.AMSend[12];
 
  TopoBuilderC.Leds -> LedsC;
  TopoBuilderC.Packet -> CC2420ActiveMessageC;
  TopoBuilderC.CC2420Packet -> CC2420ActiveMessageC; 
  TopoBuilderC.SendBeaconTimer -> TopoTimer1;
}


