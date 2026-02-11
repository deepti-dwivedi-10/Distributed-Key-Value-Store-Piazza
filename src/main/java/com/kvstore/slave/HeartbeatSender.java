package com.kvstore.slave;

import com.kvstore.common.Message;
import java.io.IOException;
import java.net.*;

/**
 * Sends heartbeat messages to Coordination Server
 * Runs in a separate thread and sends UDP messages every 5 seconds
 */
public class HeartbeatSender implements Runnable {
    private static final int UDP_PORT = 3769;
    private static final int HEARTBEAT_INTERVAL = 5000; // 5 seconds

    private final String ipAddress;
    private final int port;
    private final String address;
    private volatile boolean running = true;

    public HeartbeatSender(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.address = ipAddress + ":" + port;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress csAddress = InetAddress.getByName("127.0.0.1");

            System.out.println("[HEARTBEAT] Starting heartbeat sender (every " + (HEARTBEAT_INTERVAL/1000) + "s)");

            while (running) {
                try {
                    // Create heartbeat message
                    Message heartbeat = new Message()
                            .setReqType("heartbeat")
                            .setMessage(address);

                    byte[] buffer = heartbeat.toString().getBytes();
                    DatagramPacket packet = new DatagramPacket(
                            buffer,
                            buffer.length,
                            csAddress,
                            UDP_PORT
                    );

                    // Send heartbeat
                    socket.send(packet);
                    System.out.println("[HEARTBEAT] Sent to CS");

                    // Wait for next interval
                    Thread.sleep(HEARTBEAT_INTERVAL);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("[HEARTBEAT] Error sending heartbeat: " + e.getMessage());
                }
            }
        } catch (SocketException e) {
            System.err.println("[HEARTBEAT] Failed to create UDP socket: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("[HEARTBEAT] Unknown host: " + e.getMessage());
        }

        System.out.println("[HEARTBEAT] Heartbeat sender stopped");
    }

    public void shutdown() {
        running = false;
    }
}
