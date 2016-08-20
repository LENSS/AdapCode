#include "TopoBuilder.h"

// $Id: TopoBuilderC.nc,v 1.2 2010/04/05 02:14:41 rojac Exp $

module TopoBuilderC {
  provides interface Init;
	
  uses {

    interface Timer<TMilli> as SendBeaconTimer;

    interface AMSend as SendBeaconMsg;
    interface AMSend as SerialSendNbrTbl;

    interface Receive as ReceiveBeaconMsg;
    interface Receive as SerialReceive_StartBeacon;
    interface Receive as SerialReceive_DumpNbrTbl;
	
    interface Leds;
    //interface Init;
    //interface Boot;
    interface Packet;
    interface CC2420Packet;

    /*interface SplitControl as AMControl;
    interface SplitControl as Control; */
  
  }
}

implementation {

  uint16_t    beaconsSent;
  uint16_t    beaconSeqNum;
  Neighbor_t  nbrTbl[MAX_NBR_NUM];
  uint16_t    nbrNum;

  bool    sending;
  bool    locked;
  message_t  outPacket;

  //***************************************************************************
  //***************************************************************************
  //event void Boot.booted() {
  command error_t Init.init() {	
    atomic {
      uint8_t i;  

      for(i = 0; i < MAX_NBR_NUM; i++) {
	nbrTbl[i].nbrID = INVALID_NBR;
    	nbrTbl[i].beaconsReceived = 0;
      }

      beaconsSent = 0;
      beaconSeqNum = 1;
      sending = FALSE;
      locked = FALSE;
      nbrNum = 0;
    }

    /*call Control.start();
    call AMControl.start(); */
  }

 
  //*************************************************************************** 
  //***************************************************************************  
  /*event void AMControl.startDone(error_t err) {
    
    if (err == SUCCESS) {
      dbg("DBG_USR3", "AMControl start done\n");
    }
    else {
      call AMControl.start();
    }
  } */

  //*************************************************************************** 
  //***************************************************************************  
  //event void AMControl.stopDone(error_t err) {}

  //*************************************************************************** 
  //***************************************************************************  
  //event void Control.startDone(error_t err) {}

  //*************************************************************************** 
  //***************************************************************************  
  //event void Control.stopDone(error_t err) {}

  //*************************************************************************** 
  //***************************************************************************  
  event message_t* SerialReceive_StartBeacon.receive(message_t* bufPtr, 
				   void* payload, uint8_t len) {
    StartMsg *startMsg = (StartMsg*) payload;
	
	if(startMsg->id == 0) {
		//Reset the mote here
		atomic {
		  uint8_t i;  

		  for(i = 0; i < MAX_NBR_NUM; i++) {
			nbrTbl[i].nbrID = INVALID_NBR;
			nbrTbl[i].beaconsReceived = 0;
		  }

		  beaconsSent = 0;
		  beaconSeqNum = 1;
		  sending = FALSE;
		  locked = FALSE;
		  nbrNum = 0;
		}
		call Leds.led0Off();
		call Leds.led1Off();
		call Leds.led2Off();
	}
	else if(startMsg->id == 1) {
		call SendBeaconTimer.startPeriodic(100);
	}
    return bufPtr;
  }

  //*************************************************************************** 
  //***************************************************************************
  event void SendBeaconTimer.fired() {
    BeaconMsg *msgPtr = (BeaconMsg *)(call Packet.getPayload(&outPacket,
	sizeof(BeaconMsg)));
	//call Leds.led0Toggle();
    msgPtr->senderID = TOS_NODE_ID;
    msgPtr->seqNum = beaconSeqNum++;

    if(!sending) {
	  sending = TRUE;
          call CC2420Packet.setPower(&outPacket, 2);
	  call SendBeaconMsg.send(AM_BROADCAST_ADDR, &outPacket, 
		sizeof(BeaconMsg));
	  beaconsSent++;
	  if(beaconsSent == MAX_BEACON_CNT) {
	    call SendBeaconTimer.stop();
	  }
    }
		
  }

  //*************************************************************************** 
  //***************************************************************************  
  event void SendBeaconMsg.sendDone(message_t* bufPtr, error_t error) {
    if (&outPacket == bufPtr) {
      sending = FALSE;
    }
  }

  //*************************************************************************** 
  //***************************************************************************  
  event void SerialSendNbrTbl.sendDone(message_t* bufPtr, error_t error) {
    //call Leds.led1Toggle();
	if (&outPacket == bufPtr) {
      locked = FALSE;
    }
  }

  //*************************************************************************** 
  //********************************************************************Tbl*******  
  event message_t* SerialReceive_DumpNbrTbl.receive(message_t* bufPtr, 
				   void* payload, uint8_t len) {

    NeighborTbl_t *serialMsg = (NeighborTbl_t *)
	call Packet.getPayload(&outPacket, sizeof(NeighborTbl_t));

	//call Leds.led2Toggle();
     // send the neighbor information to PC via serial port
    if (locked) {
      	return bufPtr;
    } else {

        if (call Packet.maxPayloadLength() < sizeof(NeighborTbl_t))
	      return bufPtr;

        serialMsg->senderID = TOS_NODE_ID;
        serialMsg->nbrNum = nbrNum;
	memcpy(serialMsg->nbrTbl, nbrTbl, sizeof(Neighbor_t)*MAX_NBR_NUM);

        
        if (call SerialSendNbrTbl.send(AM_BROADCAST_ADDR, &outPacket, 
			sizeof(NeighborTbl_t)) == SUCCESS) {
	      locked = TRUE;
        }
      }

    return bufPtr;   
  }
  //*************************************************************************** 
  //***************************************************************************  
  event message_t* ReceiveBeaconMsg.receive(message_t *msg, void *payload, 
					uint8_t len) {
    BeaconMsg *msgPtr = (BeaconMsg *) payload;
    uint8_t i;
    //call Leds.led1Toggle();
    // find and update the state of the neighbor
    for(i = 0; i < MAX_NBR_NUM; i++) {
      if(nbrTbl[i].nbrID == msgPtr->senderID) {
	nbrTbl[i].beaconsReceived++;
	break;
      } 
      else if( nbrTbl[i].nbrID == INVALID_NBR) { //
	nbrTbl[i].nbrID = msgPtr->senderID;
	nbrTbl[i].beaconsReceived = 1; 
	nbrNum++;
	break;
      }
    }

    return msg;

  }

} 
