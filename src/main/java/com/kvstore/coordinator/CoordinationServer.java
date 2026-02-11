package com.kvstore.coordinator;

import com.kvstore.common.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Coordination Server (Master Node)
 * - Routes client requests to appropriate slave servers
 * - Maintains hash ring of slave servers
 * - Caches frequently accessed data (LRU cache)
 * - Monitors slave health via heartbeat
 * - Handles server failures and data migration
 */
public class CoordinationServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int CACHE_SIZE = 4;
    private static final int THREAD_POOL_SIZE = 10;

    private final String ipAddress;
    private final int port;
    private final HashRing hashRing;
    private final LRUCache<String, String> cache;
    private final HeartbeatMonitor heartbeatMonitor;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;

    public CoordinationServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.hashRing = new HashRing();
        this.cache = new LRUCache<>(CACHE_SIZE);
        this.heartbeatMonitor = new HeartbeatMonitor(hashRing);
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void start() throws IOException {
        // Write configuration file for slaves and clients
        writeConfigFile();

        // Start heartbeat monitor thread
        new Thread(heartbeatMonitor, "HeartbeatMonitor").start();

        // Start timer thread for failure detection
        new Thread(this::timerThread, "TimerThread").start();

        // Create server socket
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ipAddress));
        System.out.println("================================");
        System.out.println("Coordination Server Started");
        System.out.println("================================");
        System.out.println("IP: " + ipAddress);
        System.out.println("Port: " + port);
        System.out.println("Cache Size: " + CACHE_SIZE);
        System.out.println("================================");
        System.out.println("Ready to accept connections...");
        System.out.println();

        // Accept connections in loop
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress() +
                                      ":" + clientSocket.getPort();
                System.out.println("New connection from: " + clientAddress);

                // Handle each connection in separate thread
                threadPool.submit(new ConnectionHandler(clientSocket, hashRing, cache, ipAddress + ":" + port));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Timer thread that periodically checks for failed slaves
     */
    private void timerThread() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(30000); // Check every 30 seconds
                System.out.println("\n[TIMER] Waking up to check heartbeats...");
                heartbeatMonitor.checkForFailures();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Write CS configuration to file for slaves and clients
     */
    private void writeConfigFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("cs_config.txt"))) {
            writer.println(ipAddress);
            writer.println(port);
            System.out.println("Configuration written to cs_config.txt");
        } catch (IOException e) {
            System.err.println("Failed to write config file: " + e.getMessage());
        }
    }

    public void shutdown() {
        System.out.println("\nShutting down Coordination Server...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            heartbeatMonitor.shutdown();
        } catch (IOException e) {
            System.err.println("Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java CoordinationServer <ip> <port>");
            System.out.println("Example: java CoordinationServer 127.0.0.1 8080");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);

        CoordinationServer server = new CoordinationServer(ip, port);

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
