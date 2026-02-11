package com.kvstore.coordinator;

import com.kvstore.common.*;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors heartbeat messages from slave servers
 * Detects failures when no heartbeat received
 */
public class HeartbeatMonitor implements Runnable {
    private static final int UDP_PORT = 3769;
    private static final int BUFFER_SIZE = 1024;

    private final HashRing hashRing;
    private final ConcurrentHashMap<String, Integer> heartbeatCount;
    private DatagramSocket udpSocket;
    private volatile boolean running = true;

    public HeartbeatMonitor(HashRing hashRing) {
        this.hashRing = hashRing;
        this.heartbeatCount = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
            System.out.println("[HEARTBEAT] Listening on UDP port " + UDP_PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    String received = new String(packet.getData(), 0, packet.getLength());
                    processHeartbeat(received);

                } catch (SocketException e) {
                    if (running) {
                        System.err.println("[HEARTBEAT] Socket error: " + e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[HEARTBEAT] IO error: " + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("[HEARTBEAT] Failed to create UDP socket: " + e.getMessage());
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }

    private void processHeartbeat(String messageStr) {
        try {
            Message message = new Message(messageStr);
            if ("heartbeat".equals(message.getReqType())) {
                String address = message.getMessage();
                heartbeatCount.merge(address, 1, Integer::sum);
                System.out.println("[HEARTBEAT] Received from " + address +
                                 " (count: " + heartbeatCount.get(address) + ")");
            }
        } catch (Exception e) {
            System.err.println("[HEARTBEAT] Error processing heartbeat: " + e.getMessage());
        }
    }

    /**
     * Check for server failures (called by timer thread)
     */
    public void checkForFailures() {
        System.out.println("\n=== Heartbeat Check ===");

        if (heartbeatCount.isEmpty()) {
            System.out.println("No slave servers registered yet");
            System.out.println("=======================\n");
            return;
        }

        // Display current counts
        heartbeatCount.forEach((address, count) ->
            System.out.println(address + ": " + count + " heartbeats"));

        // Check for failures (count = 0)
        heartbeatCount.forEach((address, count) -> {
            if (count == 0) {
                System.out.println("\n[FAILURE DETECTED] Server " + address + " has failed!");
                handleServerFailure(address);
            }
        });

        // Reset counts for next interval
        heartbeatCount.replaceAll((address, count) -> 0);

        System.out.println("=======================\n");
    }

    private void handleServerFailure(String address) {
        // Remove from hash ring
        int hash = ConsistentHash.hash(address);
        hashRing.delete(hash);

        // Remove from heartbeat map
        heartbeatCount.remove(address);

        System.out.println("[RECOVERY] Server " + address + " removed from ring");
        System.out.println("[RECOVERY] System continues with remaining servers");

        // In production, would trigger data migration here
        // For simplicity, relying on replication for fault tolerance
    }

    public void shutdown() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}
