package adapcodeSim;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/***
 * 
 * @author Roja
 *  First draft ignoring the link quality for now.
 */

public class cssim {
	private int lDimension;
	private double RadioRange;
	private int Nodes;
	private Node[] node_list=new Node[100];
	private int PlinkQuality;
	private int Redundancy;
	private int hopcount=-1;
	private int source=0;
	
	public cssim(int l,double r,int n,int Plq,int Redundancy)
	{
		lDimension=l;
		RadioRange=r;
		Nodes=n;
		PlinkQuality=100;//PlinkQuality=Plq;
		this.Redundancy=Redundancy;
	}
	public static void print(String s)
	{
	  System.out.println(s);	
	}
	
	public void simulate()
	{
		set();
		build_tree();
	}
	
	public void build_tree()
	{
		Queue<Node> ql=new LinkedList<Node>();
		Node cnode;
		int i=0;
		ql.add(node_list[source]);
		node_list[source].sethc(0);
		
		for (i=1;i<Nodes;i++)
		  {
			for (int j=0;j<Nodes;j++)
			  {
				if(node_list[i].Isneighbor(node_list[j], RadioRange))
				{
					node_list[i].inc_lncs();
				}
			  }
		  }
		
		while(!ql.isEmpty())
		{
		  cnode=ql.remove();
		  /* Set cnode and update children and parent lists */
		  for (i=0;i<Nodes;i++)
		  {
			  if(node_list[i].isset())
			  {
				  if(cnode.ifParent(node_list[i]))
				  {
					  cnode.updatelists(node_list[i]);
				  }
			  }
			  else
			{
				/* find the distance */
				if(cnode.Isneighbor(node_list[i], RadioRange))
				{
					/* Set this node */
					node_list[i].sethc(cnode);
					/* Add to the queue */
					ql.add(node_list[i]);
					/* Update lists */
					cnode.updatelists(node_list[i]);
				}
			}
		  }
		  /* Update plist nodes of cnode */
		  cnode.update_clist();
		}
		
		for (i=0;i<Nodes;i++)
		  {
			node_list[i].decide_cs();
		  }
		for (i=0;i<Nodes;i++)
		  {
			node_list[i].finalize_cs();
		  }
		for (i=0;i<Nodes;i++)
		  {
			node_list[i].decide_cs();
		  }
		for (i=0;i<Nodes;i++)
		  {
			node_list[i].print();
		  }
		int tp=0;
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
		
		System.out.println("Messages : " + tp +" Adap count: " + adap +" Lower bound" + ladap);
	}
	public void set()
	{
		/* Arrange nodes in lXl grid */
		int i=0;
		double x,y,s,j;
		int k;
		Random generator = new Random( 19580427 );
		s=Math.sqrt(Nodes);
		j=lDimension/s;
		while(i<Nodes)
		{
			k=(int)(i/s);
			/* Genrate number between (k)j - (k+1)j 
			 * So generate a number between 0-j and add kj*/
			
			y=generator.nextInt((int)j);//%j;
			y+=k*j;
			
			k=(int)(i%s);
			/* Genrate number between (k)j - (k+1)j 
			 * So generate a number between 0-j and add kj*/
			
			x=generator.nextInt((int)j);//%j;
			x+=k*j;
			System.out.println(i+ " "+ x + " y "+y);
			node_list[i]=new Node(i,x,y);
			i++;
		}
	}
	
    public static void main(String[] args)
    {
    	print("Starting simulator");
    	//print("Usgae: cssim lDimension RadioRange Nodes PlinkQuality Redundancy");
    	if(args.length<5)	
    		{
    		print("Usgae: cssim lDimension RadioRange Nodes PlinkQuality Redundancy");
    		  System.exit(0);
    		}
    	int l=Integer.parseInt(args[0]);
    	double r=Double.parseDouble(args[1]);
    	int n=Integer.parseInt(args[2]);
    	int Plq=Integer.parseInt(args[3]);
    	int Redundancy=Integer.parseInt(args[4]);
    	//cssim sim=new cssim(l,r,n,Plq,Redundancy);
    	cssim sim=new cssim(200,70,16,100,0);
    	sim.simulate();
    	/* Now Create a BFS 
    	 * 1. Assign hop counts
    	 * 2. Update parent table and coding scheme
    	 * 3. Update children table*/
    	
    	/* For each child get the proposed coding scheme
    	 * Find the minimum coding scheme */
    	
    	/* */
    }
}
