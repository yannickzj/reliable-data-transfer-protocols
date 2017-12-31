# Reliable Data Transfer Protocols

## Introduction

This project aims to implement both the [Go-Back-N](https://en.wikipedia.org/wiki/Go-Back-N_ARQ) and [Selective Repeat](https://en.wikipedia.org/wiki/Selective_Repeat_ARQ) versions of pipelined reliable data transfer, as well as a simple file transfer application that can transfer any file (including binary files) over the unreliable channel emulator. The reliable transfer protocols are able to handle network errors such as packet loss, duplication, and reordering.

## Architecture
The overall setup is shown in the diagram below with 'S', 'B', 'C', and 'R' denoting the various UDP sockets.

<p align="center"><img src="/README/overview.png" width="800"></p>

When the sender needs to send packets to the receiver, it sends them to the channel emulator at 'B'. The channel emulator forwards the packets to the receiver at 'R'. However, it may randomly delay or discard packets. The receiver sends ACKs back to the channel at 'C', which may also randomly discard or delay ACKs before forwarding them to 'S'.

### Packet format

All packets exchanged between the sender and the receiver adheres to the following format:

| Field           | Explanation                                         |
| :-------------: |:---------------------------------------------------:|
| Packet Type     | 32 bit unsigned integer, big endian (network order) |
| Packet Length   | 32 bit unsigned integer, big endian (network order) |
| Sequence Number | 32 bit unsigned integer, big endian (network order) |
| Payload         | byte sequence, maximum 500 bytes |

The *Packet Type* field indicates the type of the packet. It is set as follows:

| Type            | Explanation                  | 
| :-------------: |:----------------------------:|
| 0               | Data Packet                  |
| 1               | Acknowledgement (ACK) Packet |
| 2               | End-Of-Transfer (EOT) Packet |

The *Packet Length* field specifies the total length of the packet in bytes, including the packet header. For ACK and EOT packets, the size of the packet is just the size of the header.

For data packets, the *Sequence Number* is the modulo 256 sequence number of the packet, i.e., the sequence number range is [0...255]. For ACK packets, *Sequence Number* is the sequence number of the packet being acknowledged.

### Sender program

The sender program takes three arguments: 

+ protocol selector – 0 for Go-Back-N or 1 for Selective Repeat; 

+ the value of a timeout in milliseconds; 

+ the filename to be transferred. 

The sender transfers the file reliably to the receiver program. The timeout is used as the timeout period for the reliable data transfer protocol. During the transfer, the sender program creates packets as big as possible, i.e., containing 500 bytes payload, if enough data is available. After all contents of the file have been transmitted successfully to the receiver and the corresponding ACKs have been received, the sender sends an EOT packet to the receiver. The sender exits after receiving the response EOT from the receiver. It is assumed that EOT packets are never lost.

### Receiver program

The receiver program takes two arguments: 

+ protocol selector – 0 for Go-Back-N or 1 for Selective Repeat;

+ the filename to which the transferred file is written. 

When the receiver program receives the EOT packet, it sends an EOT packet back and exits.

Both sender and receive will run using the same protocol: Go-Back-N on both ends, or selective repeat on both ends.

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

+ All Data and ACK packets are subject to a random delay, uniformly distributed between 0 and `<max delay>` milliseconds.

+ All Data and ACK packets are subject to random discard with a probability of `<discard probability>`.

+ If `<random seed>` is set to a non-zero value, this seed is being used to initialize the random number generator. Multiple runs with the same seed produce the same channel behaviour. If `<random seed>` is set to zero, the random number generator is seeded with the current system time.

+ If `<verbose>` is set to a non-zero value, the channel emulator outputs information about
  its internal processing.

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