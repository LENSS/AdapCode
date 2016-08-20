/*									tab:4
 * "Copyright (c) 2005 The Regents of the University  of California.  
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and
 * its documentation for any purpose, without fee, and without written
 * agreement is hereby granted, provided that the above copyright
 * notice, the following two paragraphs and the author appear in all
 * copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY
 * PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
 * DAMAGES ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS
 * DOCUMENTATION, EVEN IF THE UNIVERSITY OF CALIFORNIA HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 */

package serialcomm;

import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;

import net.tinyos.message.*;

public class TestSerial implements MessageListener {

  private MoteIF moteIF;
  
  public TestSerial(MoteIF moteIF) {
    this.moteIF = moteIF;
    this.moteIF.registerListener(new NeighborTblMsg(), this);
  }
  
  public void resetStatus() {
	  StartMsg payload = new StartMsg();
	  payload.set_id((short) 0);
	  
	  try {
		  //System.out.println("Sending resetStatus");
		  moteIF.send(0, payload);
	  }
	  catch (IOException exception) {
		  System.err.println("Exception thrown when sending packets. Exiting.");
		  System.err.println(exception);
	  }
	  
  }

  public void sendCS(short cs,short hc)
  {
   CSMsg payload=new CSMsg();
   payload.set_cs(cs);
   payload.set_hc(hc);
    try {
    	//System.out.println("Sending startBeacon");
    	moteIF.send(0, payload);
		    	
    }
    catch (IOException exception) {
      System.err.println("Exception thrown when sending packets. Exiting.");
      System.err.println(exception);
    }
   
  }
  public void startBeacon() {
	StartMsg payload = new StartMsg();
	payload.set_id((short) 1);
	  
    try {
    	//System.out.println("Sending startBeacon");
    	moteIF.send(0, payload);
		    	
    }
    catch (IOException exception) {
      System.err.println("Exception thrown when sending packets. Exiting.");
      System.err.println(exception);
    }
    
  }
  
  public void dumpNbrTbl() {
	short cmd;
	
    NeighborTblMsg payload = new NeighborTblMsg();
	
    try {
    	//System.out.println("Sending dumpNbrTbl");
    	//cmd = 1;
//    	payload.set_turnoff_cmd(cmd);
    	moteIF.send(0, payload);
		    	
    }
    catch (IOException exception) {
      System.err.println("Exception thrown when sending packets. Exiting.");
      System.err.println(exception);
    }
    
  }

  public void messageReceived(int to, Message message) {
    
    /*NeighborTblMsg msg = (NeighborTblMsg)message;
    Graphics2D g = (Graphics2D) SerialCommApp.getApplication().getView().getPanel().getGraphics();
    //System.out.println("hehe");
    //textArea.append("Received neighbor table size " + msg.get_num_nbr() +
    //		"from" + msg.get_source_id() + "\n");
    //textArea.append("Neighbor is" + msg.getElement_id(0) + "\n");
    //textArea.setCaretPosition(textArea.getDocument().getLength());

    //this.sendPackets();

    System.out.print(msg.get_source_id() + "'s Neighbors:");
    for (int i = 0; i < msg.get_num_nbr(); i++) {
        //textArea.append("[" + msg.get_source_id() + "] neighbor id is " + msg.getElement_id(i) + "\n");
        //textArea.setCaretPosition(textArea.getDocument().getLength());
        //view.drawConnectors(g2, msg.getElement_id(i), msg.get_source_id());
        SerialCommApp.getApplication().getView().getPanel().selectNode(msg.get_source_id(), msg.getElement_id(i));
        SerialCommApp.getApplication().getView().getPanel().paintComponent(g);
        System.out.print( msg.getElement_id(i) + " ");
    }
    System.out.print("\n");*/
	NeighborTblMsg msg = (NeighborTblMsg) message;
	System.out.println("Received neighbor table size " + msg.get_nbrNum() + 
	      " from " + msg.get_senderID());
	//print neighbor table here
	
	Graphics2D g = (Graphics2D) SerialCommApp.getApplication().getView().getPanel().getGraphics();
	
	Node newNode = new Node(msg.get_senderID());
	
	for(int i=0; i<msg.get_nbrNum(); i++) {
		
		System.out.println("Received " + msg.getElement_nbrTbl_beaconsReceived(i) + 
		      " beacons from neighbor " + msg.getElement_nbrTbl_nbrID(i) + ", prr = " + msg.getElement_nbrTbl_beaconsReceived(i)/Constants.maxPktCnt);
			newNode.addNeighbor(msg.getElement_nbrTbl_nbrID(i),
			  msg.getElement_nbrTbl_beaconsReceived(i));
			  
		//SerialCommApp.getApplication().getView().getPanel().selectNode(msg.get_senderID(), msg.getElement_nbrTbl_nbrID(i));
		//SerialCommApp.getApplication().getView().getPanel().paintComponent(g);
	}
	SerialCommApp.getApplication().getView().getPanel().addNode(newNode);
	
  }
  
  private static void usage() {
    System.err.println("usage: TestSerial [-comm <source>]");
  }

}
