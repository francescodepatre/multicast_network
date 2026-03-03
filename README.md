# Multicast Communication Simulator in Java

This project implements a **distributed client-server system in Java** that simulates **multicast communication among multiple nodes**. It demonstrates key concepts in distributed systems such as message loss, retransmission, and server coordination.

## Features

- **Node Communication**: Nodes send and receive messages containing their own ID and a sequential message ID.
- **Message Loss Simulation**: Messages may be lost based on a configurable probability. Nodes detect missing messages and request retransmission.
- **Message Types**:
  - `NORMAL`: Standard message sent by a node.
  - `LOSS`: Indicates a missing message detected by a node.
  - `RETRANSMISSION`: A retransmitted message in response to a LOSS request.
  - `START` / `STOP` / `COMPLETED`: Server control messages.
- **Server Coordination**: Server coordinates the start and stop of the simulation and tracks node completions.
- **Concurrency**: Each node handles sending, receiving, and server communication in separate threads.
- **Logging**: Detailed console output to monitor message states.

## Getting Started

### Requirements

- Java 17 or higher
- IntelliJ IDEA or any Java IDE

### Running the Project

1. **Start the server**:

```bash
java -cp out/production/MulticastSimulator multicast.simulator.Main
