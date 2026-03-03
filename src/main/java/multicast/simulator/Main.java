package multicast.simulator;

public class Main {
    public static void main(String[] args) {
        int port = 2702;
        int numberOfNodes = 5;
        String hostname = "localhost";
        Node[] nodes = new Node[numberOfNodes];
        Thread[] nodeThreads = new Thread[numberOfNodes];

        Thread server_thread = new Thread(() -> {
            try {
                Server multicastServer = new Server(port, numberOfNodes);
                multicastServer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        server_thread.start();

        try {
            Thread.sleep(500);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        for(int i = 0; i < numberOfNodes; i++){
            final int node_id = i+1;
            try{
                nodes[i] = new Node(node_id, hostname, port);
                final Node node = nodes[i];
                nodeThreads[i] = new Thread(node::start);
                nodeThreads[i].start();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
        for (Thread t : nodeThreads) {
            try {
                t.join();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        try {
            server_thread.join();
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println(ANSIColors.CYAN + "MAIN PROCESS: simluation ended." + ANSIColors.RESET);
    }

}