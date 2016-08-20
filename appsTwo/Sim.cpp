#include <iostream>
using namespace std;

class table
{
 int node_id;
}; 

class Ptabel:public table
{
};

class Ctabel:public table
{
int coding_scheme;
};

class node{
int node_id;
int x;
int y;
};

int main(int argc,char** argv)
{
/* Network Dimension */
int l;
int N; /* Number of nodes */
double ARate; /* Assymetric Rate */
/* Take either arguments in argument list or take from the user */
if(argc==3) 
  {
    /* Get the input parameters from console */
    l=(int)argv[1];
    N=(int)argv[2];
  }
else
{
  cout<< "Please Enter the dimension:";
  cin>>l;
  cout<<"Please Enter the Number of nodes:";
  cin>>N;
  
}
cout<<"starting the simulation with "<<N<<" nodes in a "<<l"X"l<<" network"<<endl;
/*First place N nodes randomly*/
/* In a for loop check for nodes in R distance */

}
