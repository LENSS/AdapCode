from TOSSIM import *
from random import *
import sys

t = Tossim([])
r = t.radio()
f = open("linkgain.out", "r")

lines = f.readlines()
for line in lines:
  s = line.split()
  if (len(s) > 0):
    if (s[0] == "gain"):
      r.add(int(s[1]), int(s[2]), float(s[3]))
    elif (s[0] == "noise"):
      r.setNoise(int(s[1]), float(s[2]), float(s[3]))

for i in range(0,4):
  if(i == 1):
    m = t.getNode(i);
    m.bootAtTime(t.ticksPerSecond()/100*51);
    print i;
    print t.ticksPerSecond()/10000000000;
  elif(i < 4):    
    m = t.getNode(i);
    j = t.ticksPerSecond()/100*randint(0,50) + t.ticksPerSecond()/10000*i;
    m.bootAtTime(j);
    print i;
    print j/100000000;
t.addChannel("NetworkCoding", sys.stdout)
t.addChannel("DbgSource", sys.stdout)
t.addChannel("NACK", sys.stdout)

while t.time()/t.ticksPerSecond() < 600:
 for i in range(0, 10000000):
  t.runNextEvent()
