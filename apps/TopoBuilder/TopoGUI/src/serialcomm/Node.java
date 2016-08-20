package serialcomm;

import java.util.ArrayList;
import java.util.Collections;
import java.io.*;

public class Node implements Serializable, Comparable{
	public int nodeID;
	public ArrayList<Neighbor> nbrTable;
	public boolean displayable;
	private boolean set=false;
	private short hc;
	private short cs=16;
	private short pref_cs;
	private ArrayList<Node> plist=new ArrayList<Node>();
	   private ArrayList<chlist> clist=new ArrayList<chlist>();
	//public ArrayList<Neighbor> HearMe;

	public Node(short id) {
		nodeID = id;
		nbrTable = new ArrayList<Neighbor>();
		displayable = true;
		set=false;
		cs=16;
	}

        public short hopcount()
        {
           return hc;
        }
        public short coding_scheme()
        {
             if(cs==16) return 0;
             else return (short)(8/cs);
          //return cs;
        }
	public void addNeighbor(short id, short count) {
		Neighbor nbr = new Neighbor();
		nbr.nodeID = id;
		nbr.prr = count/Constants.maxPktCnt;
		nbrTable.add(nbr);
	}
	
	public boolean isset()
	{
		return set;
	}
	
	public int nodeID()
        {
           return nodeID;
        }
	/*public void addToHearMe(short id,double prr)
	{
		Neighbor n=new Neighbor(id,prr);
		HearMe.add(n);
	}*/
	
	/*
	 * Returns true if in the neighbor table else false
	 */
	
	public void sethc(Node parent)
	   {
		   hc=parent.hc;
                   hc++;
		   set=true;
	   }
	public void update_clist()
	   {
		   int cspow=(int) (Math.log(this.plist.size())/Math.log(2));
		   this.pref_cs=(short) Math.pow(2, cspow);
		   
		   		   
		  /* lncs=1;
		   if(plist.size()>4) lncs=8;
		   else if(plist.size()>3) lncs=4;
		   else if(plist.size()>2) lncs=2; */
		   
		   /* for all nodes's clist in plist add this */
		   java.util.Iterator<Node> itr = plist.iterator();
		   //pref_cs=0;
		   chlist child=new chlist(nodeID,pref_cs);  
		   while(itr.hasNext())
		   {
			  
		      Node n=itr.next();
		      n.clist.add(child);
		   }
		  //chlist child=new chlist(c.node_id,c.pref_cs); 
		  //this.clist.add(child); */
	   }
	public boolean InNeighborT(int j,double p)
	{
		java.util.Iterator<Neighbor> itr = nbrTable.iterator();
		
		while(itr.hasNext())
		{
			Neighbor n=itr.next();
			if(n.nodeID==j) 
				{
				  if(p<=n.prr) return true;
				  else return false;
				}
		}
		return false;
	}
	public void sethc(short i)
	   {
		   hc=i;
		   set=true;
		   /* set pref_cs */
		   /* check in the parent_list nodes with cs equal to hc-1 */
		   
	       
	   }
	   public boolean ifParent(Node n)
	   {
		   if(this.hc==(n.hc-1)) return true;
		   else return false;
	   }
	   public void print_parents()
	   {
		   java.util.Iterator<Node> itr = plist.iterator();
		   
		   System.out.print("Parents ");
		   
		   while(itr.hasNext())
		   {
			   Node n=itr.next();
			   System.out.println(n.nodeID);
		   }
	   }
	   public void print_children()
	   {
	java.util.Iterator<chlist> itr = clist.iterator();
		   
		   System.out.print("Children ");
		   
		   while(itr.hasNext())
		   {
			   chlist n=itr.next();
			   System.out.println(n.node_id + " cs " + n.cs);
		   }
	   }
	   public void print()
	   {
		   System.out.println(nodeID + " :");
		   print_parents();
		   print_children();
	   }
	   public void updatelists(Node c)
	   {
		   /*java.util.Iterator<Node> itr = plist.iterator();
		   pref_cs=0;
		   	   
		   while(itr.hasNext())
		   {
			  
		      Node n=itr.next();
		      System.out.println("HCs : "+ hc + "Roja " + n.hc);
		      if(n.hc==hc-1)
		    	  pref_cs++;
		   }
		  chlist child=new chlist(c.node_id,c.pref_cs); 
		  this.clist.add(child); */
		  c.plist.add(this);
	   }
	   public void decide_cs()
	   {
		   /* find out the min of pref_cs of its children */
		   java.util.Iterator<chlist> itr = clist.iterator();
		   cs=16;
		   while(itr.hasNext())
		   {
			   chlist c=itr.next();
			   if(c.cs<cs) cs=c.cs;
		   }
		   //if(cs==16) cs=-1;
	   }
           public int msg_cnt()
           {
             System.out.println("Node "+ nodeID +": cs is "+ cs);
             if(cs==16) return 0;
             else return cs;
           }
	   
	   private int child_ind(int nodeid)
	   {
		   int i=0;
		   java.util.Iterator<chlist> itr = clist.iterator();
		   
		   while(itr.hasNext())
		   {
			   chlist c=itr.next();
			   if(c.node_id==nodeid) return i;
			   i++;
		   }
		   return i; 
	   }
	   
	   public void finalize_cs()
	   {
	       Collections.sort(plist);
		   java.util.Iterator<Node> itr = plist.iterator();
		   
		   //System.out.print("Parents ");
		   int sum=0;
		   while(itr.hasNext()&&sum<8)
		   {
			   Node n=itr.next();
			   sum+=(8/n.cs);
			   //System.out.println(n.node_id);
		   }
		   while(itr.hasNext())
		   {
			   /* Search in its parents clist for this node's entry and remove it */
			   Node n=itr.next();
			   int j=n.child_ind(nodeID);
			   n.clist.remove(j);
		   }
	   }
	   
	public void printNode() {
		System.out.println("Node id="+nodeID);
		for(int i = 0; i < nbrTable.size(); i++) {
			Neighbor n = (Neighbor) nbrTable.get(i);
			System.out.print("  (" + n.nodeID + ", " + n.prr + ") ");
		}
		System.out.println("");
		
	}
	private class chlist implements Serializable{
	    private int node_id;
	    private short cs;
	    
	    public chlist(int n,short c)
	    {
	   	 node_id=n;
	   	 cs =c;
	    }
	    
	    public void change_cs(short new_cs)
	    {
	   	 cs=new_cs;
	    }
	  }
	public int compareTo(Object n) throws ClassCastException{
		// TODO Auto-generated method stub
		if(!(n instanceof Node)) throw new ClassCastException("Node Expected");
		int ncs=((Node)n).cs;
		return cs-ncs;
	}
}

class Neighbor implements Serializable{
	public int nodeID;
	public double prr;
	
	Neighbor()
	{
		
	}
	
	Neighbor(short id,double pr)
	{
		prr=pr;
		nodeID=id;
	}
	
}


