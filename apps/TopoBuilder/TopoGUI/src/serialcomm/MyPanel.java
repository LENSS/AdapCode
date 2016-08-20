
package serialcomm;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author alok and radu, based on work by mgwon
 */
public class MyPanel extends JPanel {

    private int dest, src;
    private boolean init;
    //private SavedLines savedLines;
	private boolean buttonClicked;
	private ArrayList<Node> nodeArray;
	private boolean showAll;
	

    public MyPanel () {
        dest = src = 0;
        init = true;
		buttonClicked = false;
		showAll = true;
        //savedLines = new SavedLines();
		nodeArray = new ArrayList<Node>();
    }

	public void addNode(Node n) {
		nodeArray.add(n);
		n.printNode();
		init = true;
		repaint();
	}
	
	public ArrayList<Node> getnodeArray() {
		return nodeArray;
	}
	
	public void setnodeArray(ArrayList<Node> nodeArrayFromFile) {
		nodeArray.addAll(nodeArrayFromFile);
		showAll = true;
		Show(showAll);
	}
	
	public void nodeClicked(short nodeID) {
		for(int i = 0; i < nodeArray.size(); i++) {
			Node n = (Node) nodeArray.get(i);
			if(n.nodeID == nodeID) {
				if(n.displayable == true)
					n.displayable = false;
				else
					n.displayable = true;
			}
		}
		repaint();
	}
	
	public void toggleShowState() {
		if(showAll) {
			showAll = false;
		} else {
			showAll = true;
		}
		Show(showAll);
	}
	
	public void OnThresholdChange() {
		repaint();
	}
	
	private void Show(boolean bShowHide) {
		for(int i = 0; i < nodeArray.size(); i++) {
			Node n = (Node) nodeArray.get(i);
			n.displayable = bShowHide;
		}
		repaint();
	}
	
        public int getNodeId(int index)
        {
          if(index>=nodeArray.size()) return -1;
          Node n = (Node) nodeArray.get(index);
          return n.nodeID();

        }
        public short hopcount(int i)
        {
                        System.out.println("Size is "+ nodeArray.size());
                        if(i>=nodeArray.size()) return 1;
			Node n = (Node) nodeArray.get(i);
                        return n.hopcount();
        }
	
        public short coding_scheme(int i)
        {
                        if(i>=nodeArray.size()) return 1;
			Node n = (Node) nodeArray.get(i);
                        return n.coding_scheme();
        }
	public void build_tree()
	{
		Queue<Node> ql=new LinkedList<Node>();
		Node cnode,temp;
		double RadioRange=0.0;
		int Nodes=nodeArray.size();
		int i=8;
		ql.add((Node) nodeArray.get(i));
		((Node)nodeArray.get(i)).sethc((short)0);
		
       java.util.Iterator<Node> itr = nodeArray.iterator();
		
		/*while(itr.hasNext())
		  {
			cnode=itr.next();
			for (int j=0;j<Nodes;j++)
			  {
				if(cnode.InNeighborT(j, RadioRange))
				{
					cnode.inc_lncs();
				}
			  }
		  }*/
		
		while(!ql.isEmpty())
		{
		  cnode=ql.remove();
                  System.out.println("Processing :" + cnode.nodeID); 
		  itr = nodeArray.iterator();
		  /* Set cnode and update children and parent lists */
		  while(itr.hasNext())
		  {
			  temp=itr.next();
			  if(temp.isset())
			  {
				  if(cnode.ifParent(temp) && cnode.InNeighborT(temp.nodeID, RadioRange))
				  {
                                         System.out.println(" Pare to" +temp.nodeID);
					  cnode.updatelists(temp);
				  }
			  }
			  else
			{
				/* find the distance */
				if(cnode.InNeighborT(temp.nodeID, RadioRange))
				{
                                         System.out.println(" Newto" +temp.nodeID);
					/* Set this node */
					temp.sethc(cnode);
					/* Add to the queue */
					ql.add(temp);
					/* Update lists */
					cnode.updatelists(temp);
				}
			}
		  }
		  /* Update plist nodes of cnode */
		  cnode.update_clist();
		}
		
		itr = nodeArray.iterator();
		 while(itr.hasNext())
		  {
			  temp=itr.next();
			temp.decide_cs();
		  }
		 itr = nodeArray.iterator();
		 while(itr.hasNext())
		  {
			  temp=itr.next();
			temp.finalize_cs();
		  }
		
		itr = nodeArray.iterator();
		 while(itr.hasNext())
		  {
			  temp=itr.next();
			temp.decide_cs();
		  }
		 itr = nodeArray.iterator();
		 while(itr.hasNext())
		  {
			  temp=itr.next();
			  temp.print();
		  }
		 itr = nodeArray.iterator();
                 int count=0;
                 try{
                 Calendar thisMoment = Calendar.getInstance();
                 String fName=thisMoment.get(Calendar.YEAR) + "_" +thisMoment.get(Calendar.MONTH)+"_"+thisMoment.get(Calendar.DAY_OF_MONTH) +"_"+ thisMoment.get(Calendar.HOUR_OF_DAY) +":"+ thisMoment.get(Calendar.MINUTE)+".cs";

                 File file=new File(fName);
                 if(file.createNewFile())
                 {
                 BufferedWriter out=new BufferedWriter(new FileWriter(file));
		 while(itr.hasNext())
		  {
			  temp=itr.next();
                          try{
                          out.write(temp.nodeID+": "+temp.msg_cnt());
                          out.newLine();
                           }
                 catch(IOException e)
                 {
                  System.out.println("Error "+ e.toString());
                 } 
			  if(temp.msg_cnt()!=0) count++;
		  }
		System.out.println("Count " +count);
                   out.write("Count "+count);
                   out.close();
                  }}
                 catch(IOException e)
                 {
                  System.out.println("Error "+ e.toString());
                 } 
		/*int tp=0;
		for (i=0;i<Nodes;i++)
		  {
			tp+=node_list[i].message_cnt();
		  }
		int adap=0;
		for (i=0;i<Nodes;i++)
		  {
			adap+=node_list[i].adapmessage_cnt();
		  }
		int ladap=0;
		for (i=0;i<Nodes;i++)
		  {
			ladap+=node_list[i].adaplmessage_cnt();
		  }
		
		System.out.println("Messages : " + tp +" Adap count: " + adap +" Lower bound" + ladap);*/
	}
	
    public void paintComponent(Graphics g) {
        Rectangle r1, r2;
        double x1, y1, x2, y2, ctrlx, ctrly;
        JButton button1 = null, button2 = null;
        Random rand = new Random();
        double rand_double, rand_int;
        Color color = null;
        Graphics2D graph = (Graphics2D)g;
		
		//System.out.println("paint component called");
		
        super.paintComponent(g);

		for(int i = 0; i < nodeArray.size(); i++) {
			
			Node n = (Node) nodeArray.get(i);
			if(n.displayable == false)
				continue;
				
			dest = n.nodeID;
			button1 = SerialCommApp.getApplication().getView().getButton(dest);
			
			for(int j = 0; j < n.nbrTable.size(); j++) {
				if(((Neighbor) n.nbrTable.get(j)).prr < SerialCommApp.getApplication().getView().getThreshold())
					continue;
				src = ((Neighbor) n.nbrTable.get(j)).nodeID;
				button2 = SerialCommApp.getApplication().getView().getButton(src);				
	
				r1 = button1.getBounds();
				x1 = r1.getCenterX();
				y1 = r1.getCenterY();
				r2 = button2.getBounds();
				x2 = r2.getCenterX();
				y2 = r2.getCenterY();
				
				Point p1 = new Point((int) x1, (int) y1);
				Point p2 = new Point((int) x2, (int) y2);
				
		        double dy = p2.y - p1.y;
				double dx = p2.x - p1.x;
				double theta = Math.atan2(dy, dx);
				double distance = p1.distance(p2);

				// Find center point on line between points.
				Point2D.Double center = new Point2D.Double();
				center.x = p1.x + (distance/2)*Math.cos(theta);
				center.y = p1.y + (distance/2)*Math.sin(theta);

				// Arbitrary decision:
				// Locate ctrl 90 degrees (clockwise relative to p1)
				// from center of line between points.
				Point2D.Double p = new Point2D.Double();
				int offset = 40;
				p.x = center.x + offset*Math.cos(theta+Math.PI/2);
				p.y = center.y + offset*Math.sin(theta+Math.PI/2);
				
				QuadCurve2D q = new QuadCurve2D.Float();
				q.setCurve(x1, y1, p.x, p.y, x2, y2);
				
				//graph.setColor(color);
				graph.setColor(new Color(Constants.nodeColors[dest][0], 
					Constants.nodeColors[dest][1], Constants.nodeColors[dest][2]));
				graph.draw(q);
				
			}
        }
    }
}
