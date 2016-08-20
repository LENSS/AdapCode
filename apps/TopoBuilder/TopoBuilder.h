#ifndef __TOPOBUILDER_H__
#define __TOPOBUILDER_H__

// $Id: TopoBuilder.h,v 1.1 2010/04/03 00:05:45 rojac Exp $

typedef enum {
  MAX_NBR_NUM     = 20,
  INVALID_NBR     = -1,
  AM_MSG_STOP          = 155,
  AM_MSG_DATA          = 156,
  AM_MSG_START         = 157,
  AM_MSG_SEND          = 158,
  AM_MSG_BEACON        = 159,
  AM_NEIGHBORTBL    = 160,
  AM_STARTMSG       = 161,
  MAX_BEACON_CNT	= 10
} Constants;


typedef nx_struct StartMsg{
  nx_int16_t   id;  // initialized to -1
} StartMsg;

typedef nx_struct {
  nx_int16_t   nbrID;  // initialized to -1
  nx_uint8_t   beaconsReceived;
} Neighbor_t;

typedef nx_struct BeaconMsg{
  nx_uint16_t  senderID;
  nx_uint16_t  seqNum;
} BeaconMsg;

typedef nx_struct NeighborTbl {
  nx_uint8_t  senderID;         
  nx_uint8_t  nbrNum;               // number of neighbors
  Neighbor_t  nbrTbl[MAX_NBR_NUM];  // neighbor table
} NeighborTbl_t;


#endif
