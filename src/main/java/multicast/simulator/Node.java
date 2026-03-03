package multicast.simulator;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Node {
    private int node_id;
    private int current_message_id = 0;

    private static final int MAX_MESSAGES = 100;
    private static final double LOSS_PROBABILITY = 0.2;

    private MulticastSocket udpSocket;
    private InetAddress multicastGroup;
    private int multicastPort = 5000;

    private Socket serverSocket;
    private ObjectOutputStream serverOut;
    private ObjectInputStream serverIn;

    private ConcurrentHashMap<Integer,Integer> expected_messages = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Message> sent_messages = new ConcurrentHashMap<>();

    private volatile boolean running = true;

    public Node(int node_id, String server_host, int server_port) throws Exception {
        this.node_id = node_id;

        serverSocket = new Socket(server_host, server_port);
        System.out.println("Node " + node_id + " connected");
        serverOut = new ObjectOutputStream(serverSocket.getOutputStream());
        serverIn = new ObjectInputStream(serverSocket.getInputStream());

        udpSocket = new MulticastSocket(multicastPort);
        multicastGroup = InetAddress.getByName("230.0.0.0");
        udpSocket.joinGroup(multicastGroup);
    }

    public void start() {
        System.out.println("Node " + node_id + " started");
        new Thread(this::listenMulticast).start();
        new Thread(this::sendMessages).start();
        new Thread(this::listenServer).start();
    }

    private void sendMessages() {
        Random random = new Random();

        while (!started) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        while (running && current_message_id < MAX_MESSAGES) {
            try {
                Thread.sleep(1000);

                current_message_id++;

                Message msg = new Message(
                        MessageType.NORMAL,
                        node_id,
                        current_message_id
                );

                sent_messages.put(current_message_id, msg);

                if (random.nextDouble() > LOSS_PROBABILITY) {
                    sendMulticast(msg);
                    System.out.println("Node " + node_id + " sent: " + msg);
                } else {
                    System.out.println("Node " + node_id + " message lost: " + msg);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        notifyCompletion();
    }

    private void listenMulticast() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Message msg = (Message) ois.readObject();
                handleMessage(msg);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(Message msg) {

        if (msg.getSenderID() == node_id) return;

        expected_messages.putIfAbsent(msg.getSenderID(), 1);
        int expected = expected_messages.get(msg.getSenderID());

        if (msg.getType() == MessageType.NORMAL ||
                msg.getType() == MessageType.RETRANSMISSION) {

            if (msg.getMessageID() == expected) {
                expected_messages.put(msg.getSenderID(), expected + 1);
            }
            else if (msg.getMessageID() > expected) {
                sendLossRequest(msg.getSenderID(), expected);
            }
        }

        if (msg.getType() == MessageType.LOSS) {
            if (msg.getSenderID() == node_id) {
                Message missing = sent_messages.get(msg.getMessageID());
                if (missing != null) {
                    Message retransmission = new Message(
                            MessageType.RETRANSMISSION,
                            node_id,
                            missing.getMessageID()
                    );
                    sendMulticast(retransmission);
                }
            }
        }
    }

    private void sendLossRequest(int originalSenderId, int missingId) {
        Message lossMsg = new Message(
                MessageType.LOSS,
                originalSenderId,
                missingId
        );
        sendMulticast(lossMsg);
    }

    private void sendMulticast(Message msg) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(msg);
            oos.flush();

            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, multicastGroup, multicastPort);

            udpSocket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private volatile boolean started = false;

    private void listenServer() {
        try {
            while (running) {
                Message msg = (Message) serverIn.readObject();
                if (msg.getType() == MessageType.START) {
                    started = true;
                    System.out.println("Node " + node_id + " received START");
                } else if (msg.getType() == MessageType.STOP) {
                    running = false;
                    udpSocket.close();
                    serverSocket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyCompletion() {
        try {
            Message completed = new Message(
                    MessageType.COMPLETED,
                    node_id,
                    current_message_id
            );
            serverOut.writeObject(completed);
            serverOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
