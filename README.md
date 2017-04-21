# cnnode

rz2357
Ruomeng Zhang

## Makefile
Make a new project:
```bash
make new
```

Compile the project:
```bash
make build
```

Create executable jar file:
```bash
make jar
```

## Run program
To run `gbnnode.jar`, use jar file:
```bash
java -jar gbnnode.jar <self-port> <peer-port> <window-size> [-d <value-of-n> | -p <value-of-p>]
```

To run `dvnode.jar`, use jar file:
```bash
java -jar dvnode.jar <local-port> <neighbor1-port> <loss-rate-1> <neighbor2-port> <loss-rate-2> ... [last]
```

To run cnnode.java, use jar file:
```bash
java -jar cnnode.jar <local-port> receive <neighbor1-port> <loss-rate-1> <neighbor2-port> <loss-rate-
2> ... <neighborM-port> <loss-rate-M> send <neighbor(M+1)-port> <neighbor(M+2)-
port> ... <neighborN-port> [last]
```
## GBN
### Features
#### Send message
`send <message>`
#### Status messages
Sender:
```
[<timestamp>] packet<packet-num> <packet-content> sent
[<timestamp>] ACK<packet-num> received, window moves to <packet-num>
[<timestamp>] ACK<packet-num> discarded
[<timestamp>] packet<packet-num> timeout
```
Receiver:
```
[<timestamp>] packet<packet-num> <packet-content> received
[<timestamp>] packet<packet-num> <packet-content> discarded
[<timestamp>] ACK<packet-num> sent, expecting packet<packet-num>
```

#### Loss Rate Calculation
At the end of the transmission, have both the sender and receiver print out the ratio of total packet failures to total
packets received, to see how it relates to the failure rate specified on the command line:
```
[Summary] <# dropped>/<# total> packets discarded, loss rate = <value>%
```

### Algorithm
Using go back N algorithm to simulate TCP-like connection between two node. The window size is cuntomized by user and user can also choose two mode to drop packages.

The sended message is in format `seqNm char`.

The ACK message is in format `ackNum`.

The final message is `f i n`.

### Data Structure
Main Class: To start sending thread and receiving thread.

SenderThread: To run as Sender and implements Runnable.

ReceiverThread: To receive message and process the message, including replying ASK.

TimeoutTask: To resend after timeout.

## DV
### Features
#### Status Messages
1. Routing message sent:
```
[<timestamp>] Message sent from Node <port-xxxx> to Node <port-vvvv>
```
2. Routing message received:
```
[<timestamp>] Message received at Node <port-vvvv> from Node <port-xxxx>
```
3. Routing table (every time after a message is received, and for the initial routing table):
```
[<timestamp>] Node <port-xxxx> Routing Table
- (<distance>) -> Node <port-yyyy>
- (<distance>) -> Node <port-zzzz> ; Next hop -> Node <port-yyyy>
- (<distance>) -> Node <port-vvvv>
- (<distance>) -> Node <port-wwww> ; Next hop -> Node <port-vvvv>
```

### Algorithm
The sended update message is in format `<time stamp> <port-from1> <distance1> <port-from2> <distance2> ...`

Each time, the node check the change in routing table, if it changes, update and show new table to user.

Every time, compare the distance between the to port and the distance in routing table, update based on the changes.

### Data Structure
Main Class: To run different threads.

UpdateTable: To update routing table by Bellman-Ford algorithm.

Broadcast: To send DV to neighbors.

RoutingTable: To save each node data.

## CN
### Features
Combine two program together.

### Algorithm

### Data Structure
Main Class: To run different threads and process inputs.

UpdateTable: To update routing table by Bellman-Ford algorithm. Adding reply ACK and probe functions.

Broadcast2: To send DV to neighbors, modified from last program.

RoutingTable2: To save each node data, modified from last program.

Sender:  To run as Sender and process ACK.

TimeourTask2: resend packages.

DisplayLoss: display the loss rate once a second.

UpdateRoute: Update the routing table info mation every 5 seconds.

RoutingTable2: Modified routing table.

