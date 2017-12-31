# Reliable Data Transfer Protocols

## Introduction

This is a course assignment to implement both the *Go-Back-N* and *Selective Repeat* versions of pipelined reliable data transfer, as well as a simple file transfer application that can transfer any file (including binary files) over the unreliable channel emulator. The reliable transfer protocols are able to handle network errors such as packet loss, duplication, and reordering.

## Assignment requirements
The overall setup is shown in the diagram below with 'S', 'B', 'C', and 'R' denoting the various UDP sockets.

<p align="center"><img src="/README/overview.png" width="500"></p>

When the sender needs to send packets to the receiver, it sends them to the channel emulator at 'B'. The channel emulator forwards the packets to the receiver at 'R'. However, it may randomly delay or discard packets. The receiver sends ACKs back to the channel at 'C', which may also randomly discard or delay ACKs before forwarding them to 'S'.

### Packet format

### Sender program

### Receiver program

## Addressing

In order to keep addressing simple, but enable running the programs in a shared environment, the following addressing scheme is used with OS-assigned port numbers. 

+ The receiver program is started first and must write its 'R' socket address information (hostname and port number) into a file *recvInfo* that is read by the channel emulator. 

+ The channel emulator is started next and uses this information to send packets towards the receiver. 

+ The same mechanism is used between the sender and the emulator, i.e., the emulator writes its 'B' addressing information into a file *channelInfo* which is then read by the sender. 

All files are read and written in the current directory. The contents of each file are the IP address (or hostname) and port number, separated by space.

## Channel emulator

The channel emulator is started with the following syntax:
```
java -jar Emulator/channel.jar <max delay> <discard probability> <random seed> <verbose>
```

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

+ Build and Test: 
```
ubuntu 16.04
```

## Design ideas

Both protocols use the common class, called *Packet*, for the transmission packet. This class follows the packet format described in the assignment, which can easily convert to the *Bytes* format and vice versa. Here are some more design ideas for the two protocols.

+ Go-Back-N protocol

The design follows the description of Go-Back-N as discussed in class. In the *GBNSender*, a queue and a semaphore with initial value of 10 are used to implement sender's sliding window. The sender uses a single timer to schedule the timeout task, which sends out all the packets in the current window. For the *GBNReceiver*, cumulative acknowledgement is used to inform the sender of the receiving status. Some multi-threading techniques such object lock are used to synchronize the operations for the sender queue and timer.

+ Selective Repeat protocol

The Selective Repeat version of pipelined reliable data transfer also follows the description as discussed in class. Each packet comes with its own logical timer, called *TimerPacket*. The *SRSender* uses a hashmap, a queue and a semaphore to implement the sliding window, which allows O(1) runtime access to the packet and fast queue operations (*poll*, *peek* and *offer*). The selective acknowledgement is used on the receiver side, which allows to reduce unnecessary retransmissions of packets.


## Tools

+ *make* version: *GNU Make 4.1*

+ *Java* version: *openjdk-9*