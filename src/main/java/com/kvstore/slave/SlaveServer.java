package com.kvstore.slave;

import com.kvstore.common.Message;
import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Slave Server (Data Node)
 * - Stores actual key-value data in two tables (OWN and PREV)
 * - Sends heartbeat to Coordination Server
 * - Handles GET, PUT, UPDATE, DELETE operations
 */
public class SlaveServer {
    private static final int HEARTBEAT_INTERVAL = 5000; // 5 seconds

    private final String ipAddress;
    private final int port;
    private final DataStore dataStore;
    private final HeartbeatSender heartbeatSender;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;

    public SlaveServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.dataStore = new DataStore();
        this.heartbeatSender = new HeartbeatSender(ipAddress, port);
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        // Register with Coordination Server
        registerWithCoordinator();

        // Start server socket
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress));
        System.out.println("================================");
        System.out.println("Slave Server Started");
        System.out.println("================================");
        System.out.println("IP: " + ipAddress);
        System.out.println("Port: " + port);
        System.out.println("Address: " + ipAddress + ":" + port);
        System.out.println("================================");
        System.out.println("Ready to accept connections...");
        System.out.println();

        // Start heartbeat sender
        new Thread(heartbeatSender, "HeartbeatSender").start();

        // Accept connections
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(new RequestHandler(clientSocket, dataStore));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Register this slave server with the Coordination Server
     */
    private void registerWithCoordinator() throws IOException {
        Properties props = loadCoordinatorConfig();
        String csIP = props.getProperty("ip");
        int csPort = Integer.parseInt(props.getProperty("port"));

        System.out.println("Registering with Coordination Server at " + csIP + ":" + csPort);

        try (Socket csSocket = new Socket(csIP, csPort);
             PrintWriter out = new PrintWriter(csSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(csSocket.getInputStream()))) {

            // Wait for initial acknowledgment
            String ack = in.readLine();
            System.out.println("CS: " + ack);

            // Send identification
            Message idMsg = new Message()
                    .setId("slave_server")
                    .setMessage(ipAddress + ":" + port);
            out.println(idMsg);

            // Wait for registration response
            String response = in.readLine();
            System.out.println("CS: " + response);

            Message respMsg = new Message(response);
            if ("registration_successful".equals(respMsg.getMessage())) {
                System.out.println("Successfully registered with Coordination Server!");
            } else {
                System.err.println("Registration failed: " + respMsg.getMessage());
            }
        }
    }

    /**
     * Load Coordination Server configuration from file
     */
    private Properties loadCoordinatorConfig() throws IOException {
        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader("cs_config.txt"))) {
            String ip = reader.readLine();
            String port = reader.readLine();
            props.setProperty("ip", ip);
            props.setProperty("port", port);
        }
        return props;
    }

    public void shutdown() {
        System.out.println("\nShutting down Slave Server...");
        try {
            heartbeatSender.shutdown();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java SlaveServer <ip> <port>");
            System.out.println("Example: java SlaveServer 127.0.0.1 8081");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        SlaveServer server = new SlaveServer(ip, port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
