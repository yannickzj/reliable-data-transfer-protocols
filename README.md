# Assignment 2

## How to run the program

+ Compile source code

In the main directory, run the following command:

```
make clean
make all
```

+ Run the Receiver
```
java Receiver <protocol selector> <filename>
```

The Receiver will output the *recvInfo* file.

+ Run the Sender
```
java Sender <protocol selector> <timeout> <filename>
```

Before you run the Sender, please remember to run the channel emulator first.

## Build and test environment

+ Build: 
```
ubuntu1604-006.student.cs.uwaterloo.ca
```
+ Test: 

```
ubuntu1604-006.student.cs.uwaterloo.ca
```

## Design ideas

All parts of the assignment have been completed. Both protocols use the common class, called *Packet*, for the transmission packet. This class follows the packet format described in the assignment, which can easily convert to the *Bytes* format and vice versa. Here are some more design ideas for the two protocols.

+ Go-Back-N protocol

The design follows the description of Go-Back-N as discussed in class. In the *GBNSender*, a queue and a semaphore with initial value of 10 are used to implement sender's sliding window. The sender uses a single timer to schedule the timeout task, which sends out all the packets in the current window. For the *GBNReceiver*, cumulative acknowledgement is used to inform the sender of the receiving status. Some multi-threading techniques such object lock are used to synchronize the operations for the sender queue and timer.

+ Selective Repeat protocol

The Selective Repeat version of pipelined reliable data transfer also follows the description as discussed in class. Each packet comes with its own logical timer, called *TimerPacket*. The *SRSender* uses a hashmap, a queue and a semaphore to implement the sliding window, which allows O(1) runtime access to the packet and fast queue operations (*poll*, *peek* and *offer*). The selective acknowledgement is used on the receiver side, which allows to reduce unnecessary retransmissions of packets.


## Tools

+ *make* version: *GNU Make 4.1*

+ *Java* version: *openjdk "9-internal"*