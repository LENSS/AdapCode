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
 * @version $Revision: 1.1 $ $Date: 2010/06/01 01:07:33 $
 */

#include "RCC2420.h"
#include "AM.h"

configuration LWrapperC {
  provides {
    interface SplitControl;
    interface AMSend;
    interface Receive;
    interface Packet;
    interface LowPowerListening;
    //interface StopDutyCycling;
  }
}
implementation {

 components CC2420ActiveMessageC;
 SplitControl = CC2420ActiveMessageC;
 AMSend = CC2420ActiveMessageC.AMSend[10];
 Receive = CC2420ActiveMessageC.Receive[10];
 Packet = CC2420ActiveMessageC;
 LowPowerListening = CC2420ActiveMessageC;
 //StopDutyCycling = CC2420ActiveMessageC;
}
