package multicast.simulator;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

public class Server {

    private ServerSocket serverSocket;

    private int port;
    private int totalNodes;

    private Set<Integer> completedNodes = ConcurrentHashMap.newKeySet();
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private CountDownLatch shutdownLatch = new CountDownLatch(1);
    private List<Thread> handlerThreads = new CopyOnWriteArrayList<>();

    public Server(int port, int totalNodes) {
        this.port = port;
        this.totalNodes = totalNodes;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println(ANSIColors.YELLOW + "SERVER: Server started..." + ANSIColors.RESET);

        while (clients.size() < totalNodes) {
            System.out.println(ANSIColors.YELLOW + "SERVER: Waiting for nodes..." + ANSIColors.RESET);
            //Aspetta che l'n-esimo client si connette
            Socket socket = serverSocket.accept();
            System.out.println(ANSIColors.YELLOW + "SERVER: Node accepted..." + ANSIColors.RESET);
            //assegna l'handler al cliente
            ClientHandler handler = new ClientHandler(socket);
            clients.add(handler);
            new Thread(handler).start();
        }

        System.out.println(ANSIColors.YELLOW + "SERVER: All nodes connected -> Starting..." + ANSIColors.RESET); 
        //Invio del messaggio di start dal server
        broadcast(new Message(MessageType.START, -1, 0));
        try{
            shutdownLatch.await();
        }
        catch (Exception e){
            e.printStackTrace();
        }

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
        private volatile boolean handlerRunning = true;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                while (handlerRunning && !socket.isClosed()) {
                    Message msg = (Message) in.readObject();

                    if (msg.getType() == MessageType.COMPLETED) {
                        System.out.println(ANSIColors.PURPLE + "SERVER: Node " + msg.getSenderID() + " has completed." + ANSIColors.RESET);
                        completedNodes.add(msg.getSenderID());

                        if (completedNodes.size() == totalNodes) {
                            System.out.println(ANSIColors.YELLOW + "SERVER: All node completed. Terminating." + ANSIColors.RESET);
                            broadcast(new Message(MessageType.STOP, -1, 0));

                            for (ClientHandler client : clients) {
                                client.closeHandler();
                            }

                            if (serverSocket != null && !serverSocket.isClosed()) {
                                serverSocket.close();
                            }

                            shutdownLatch.countDown();
                        }
                    }
                }
            } catch (EOFException | SocketException e){
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }

        public void close() {
            try {
                if (in != null){
                    in.close();
                } 
                if (out != null){
                    out.close();
                }
                if (socket != null && !socket.isClosed()){
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void closeHandler() {
            handlerRunning = false;
            close();
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