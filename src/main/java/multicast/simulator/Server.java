package multicast.simulator;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {

    private int port;
    private int totalNodes;

    private Set<Integer> completedNodes = ConcurrentHashMap.newKeySet();
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Server(int port, int totalNodes) {
        this.port = port;
        this.totalNodes = totalNodes;
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started...");

        while (clients.size() < totalNodes) {
            System.out.println("Waiting for nodes...");
            Socket socket = serverSocket.accept();
            System.out.println("Node accepted...");
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }

        System.out.println("All nodes connected. Starting...");
        broadcast(new Message(MessageType.START, -1, 0));
    }

    private void broadcast(Message msg) {
        for (ClientHandler client : clients) {
            client.send(msg);
        }
    }

    private class ClientHandler implements Runnable {

        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();

                    if (msg.getType() == MessageType.COMPLETED) {
                        System.out.println("Node " + msg.getSenderID() + " has completed.");
                        completedNodes.add(msg.getSenderID());

                        if (completedNodes.size() == totalNodes) {
                            System.out.println("All node completed. Terminating.");
                            broadcast(new Message(MessageType.STOP, -1, 0));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}