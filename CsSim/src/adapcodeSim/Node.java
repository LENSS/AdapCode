package adapcodeSim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.text.html.HTMLDocument.Iterator;

public class Node implements Comparable {
   private int node_id;
   private double x;
   private double y;
   private int pref_cs;
   private int ncs;
   private int lncs;
   private int cs=16;
   private int hc;
   private boolean set=false;
   private ArrayList<Node> plist=new ArrayList<Node>();
   private ArrayList<chlist> clist=new ArrayList<chlist>();
   
   public Node(int node_id, double x,double y)
   {
	this.node_id=node_id; lncs=0;
	this.x=x;
	this.y=y;
   }
   
   public void sethc(int i)
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
		   int j=n.child_ind(node_id);
		   n.clist.remove(j);
	   }
   }
   
   public int message_cnt()
   {
	   System.out.println("Cs is" + cs);
	   if(cs==16) return 0;
	   else return 8/cs;
   }
   
   public int adapmessage_cnt()
   {
	   System.out.println("NCs is" + ncs);
	   //if(ncs==16) return 0;
	   if(ncs==0) return 8;
	   return 8/ncs;
   }
   
   public int adaplmessage_cnt()
   {
	   System.out.println("LNCs is" + lncs);
	   if(lncs<5) return 8;
	   else if(lncs<8) return 4;
	   else if(lncs<11) return 2;
	   return 1;
   }
   public void update_clist()
   {
	   int cspow=(int) (Math.log(this.plist.size())/Math.log(2));
	   this.pref_cs=(int) Math.pow(2, cspow);
	   
	   ncs=1;
	   if(plist.size()>11) ncs=8;
	   else if(plist.size()>8) ncs=4;
	   else if(plist.size()>5) ncs=2;
	   
	  /* lncs=1;
	   if(plist.size()>4) lncs=8;
	   else if(plist.size()>3) lncs=4;
	   else if(plist.size()>2) lncs=2; */
	   
	   /* for all nodes's clist in plist add this */
	   java.util.Iterator<Node> itr = plist.iterator();
	   //pref_cs=0;
	   chlist child=new chlist(node_id,pref_cs);  
	   while(itr.hasNext())
	   {
		  
	      Node n=itr.next();
	      n.clist.add(child);
	   }
	  //chlist child=new chlist(c.node_id,c.pref_cs); 
	  //this.clist.add(child); */
   }
   public void print_parents()
   {
	   java.util.Iterator<Node> itr = plist.iterator();
	   
	   System.out.print("Parents ");
	   
	   while(itr.hasNext())
	   {
		   Node n=itr.next();
		   System.out.println(n.node_id);
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
   public void inc_lncs()
   {
	   lncs++;
   }
   public void print()
   {
	   System.out.println(node_id + " :");
	   print_parents();
	   print_children();
   }
   
   public void sethc(Node parent)
   {
	   hc=parent.hc+1;
	   set=true;
   }
   public boolean isset()
   {
	   return set;
   }
   
   private double Distance(Node N)
   {
	   if (this==N) return 0;
	   else
	   {
		   double dist;
		   dist=Math.sqrt(Math.pow((this.x-N.x),2)+Math.pow((y-N.y),2));
		   return dist;
	   }
   }
   
   public Boolean Isneighbor(Node N,double R)
   {
	   if(this==N) return false;
	   if (this.Distance(N)< R) return true;
	   return false;
   }
   
   private class chlist{
     private int node_id;
     private int cs;
     
     public chlist(int n,int c)
     {
    	 node_id=n;
    	 cs =c;
     }
     
     public void change_cs(int new_cs)
     {
    	 cs=new_cs;
     }
   }
   
   private class Nodecomprator implements Comparator<Node>{

	@Override
	public int compare(Node x, Node y) {
		// TODO Auto-generated method stub
		if (x.cs<y.cs) return -1;
		else if(x.cs > y.cs) return 1;
		return 0;
	}
	   
   }

@Override
public int compareTo(Object n) throws ClassCastException{
	// TODO Auto-generated method stub
	if(!(n instanceof Node)) throw new ClassCastException("Node Expected");
	int ncs=((Node)n).cs;
	return cs-ncs;
}
}
