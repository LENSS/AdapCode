#include "TopoBuilder.h"

// $Id: TopoBuilderAppC.nc,v 1.2 2010/04/05 02:14:41 rojac Exp $

configuration TopoBuilderAppC {
provides{
 interface Init;
}

implementation {
  //components MainC, TopoBuilderC, ActiveMessageC, 
  /* components  MainC, TopoBuilderC, CC2420ActiveMessageC, 
	SerialActiveMessageC as SerialAM, LedsC; */

  components TopoBuilderC, LedsC;

  //components new AMSenderC(1) as Sender1;
  //components new AMReceiverC(1) as Receiver1;
  //components new TimerMilliC() as Timer1;
  //components new SerialAMSenderC(160);
 
  //TopoBuilderC.Boot -> MainC.Boot;
  
  TopoBuilderC.Leds -> LedsC;
  //TopoBuilderC.Packet -> Sender1;
  /*TopoBuilderC.Packet -> CC2420ActiveMessageC;
  TopoBuilderC.CC2420Packet -> CC2420ActiveMessageC;
  
  TopoBuilderC.SerialReceive_StartBeacon -> SerialAM.Receive[161];
  TopoBuilderC.SerialReceive_DumpNbrTbl -> SerialAM.Receive[160];
  //TopoBuilderC.SerialSendNbrTbl -> SerialAM.AMSend[160];
  TopoBuilderC.SerialSendNbrTbl -> SerialAMSenderC;
 
  TopoBuilderC.SendBeaconTimer -> Timer1;

  //TopoBuilderC.ReceiveBeaconMsg -> Receiver1;
  TopoBuilderC.ReceiveBeaconMsg -> CC2420ActiveMessageC.Receive[1];
  //TopoBuilderC.SendBeaconMsg -> Sender1;
  TopoBuilderC.SendBeaconMsg -> CC2420ActiveMessageC.AMSend[1];

  //TopoBuilderC.AMControl -> ActiveMessageC;
  TopoBuilderC.AMControl -> CC2420ActiveMessageC;
  TopoBuilderC.Control -> SerialAM; */
   
}
