package com.kvstore.coordinator;

import com.kvstore.common.*;
import java.io.*;
import java.net.Socket;

/**
 * Handles individual connections to the Coordination Server
 * Can handle both client connections and slave server registrations
 */
public class ConnectionHandler implements Runnable {
    private final Socket socket;
    private final HashRing hashRing;
    private final LRUCache<String, String> cache;
    private final String csAddress;
    private BufferedReader in;
    private PrintWriter out;

    public ConnectionHandler(Socket socket, HashRing hashRing, LRUCache<String, String> cache, String csAddress) {
        this.socket = socket;
        this.hashRing = hashRing;
        this.cache = cache;
        this.csAddress = csAddress;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send initial connection acknowledgment
            sendMessage(Message.ack("connected"));

            // Receive identification message
            String idMsg = in.readLine();
            if (idMsg == null || idMsg.isEmpty()) {
                return;
            }

            Message idMessage = new Message(idMsg);
            String id = idMessage.getId();

            if ("client".equals(id)) {
                handleClient();
            } else if ("slave_server".equals(id)) {
                handleSlaveRegistration(idMessage);
            }

        } catch (Exception e) {
            System.err.println("Error handling connection: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * Handle client requests (GET, PUT, UPDATE, DELETE)
     */
    private void handleClient() throws IOException {
        System.out.println("[CLIENT] Client connected");
        sendMessage(Message.ack("ready_to_serve"));

        String request;
        while ((request = in.readLine()) != null) {
            System.out.println("[CLIENT] Received: " + request);

            try {
                Message reqMsg = new Message(request);
                String reqType = reqMsg.getReqType();
                String key = reqMsg.getKey();

                switch (reqType) {
                    case "get":
                        handleGet(key);
                        break;
                    case "put":
                        handlePut(key, reqMsg.getValue());
                        break;
                    case "update":
                        handleUpdate(key, reqMsg.getValue());
                        break;
                    case "delete":
                        handleDelete(key);
                        break;
                    default:
                        sendMessage(Message.ack("unknown_request"));
                }

                // Display current state (for debugging)
                System.out.println();
                hashRing.display();
                cache.display();
                System.out.println();

            } catch (Exception e) {
                System.err.println("[CLIENT] Error processing request: " + e.getMessage());
                sendMessage(Message.ack("parse_error"));
            }
        }
    }

    /**
     * Handle GET request
     */
    private void handleGet(String key) {
        // Check cache first
        if (cache.containsKeySafe(key)) {
            System.out.println("[CACHE HIT] Key '" + key + "' found in cache");
            String value = cache.getSafe(key);
            sendMessage(Message.data(value));
            return;
        }

        System.out.println("[CACHE MISS] Key '" + key + "' not in cache, fetching from slave...");

        // Calculate hash and find responsible server
        int hash = ConsistentHash.hash(key);
        ServerNode successor = hashRing.getSuccessor(hash);

        if (successor == null) {
            sendMessage(Message.ack("no_servers_available"));
            return;
        }

        System.out.println("[GET] Hash(" + key + ") = " + hash + " -> Server at position " + successor.getHashPosition());

        // Get value from slave server
        String value = getFromSlave(successor, key, "own");

        if (value != null) {
            // Store in cache for future requests
            cache.putSafe(key, value);
            sendMessage(Message.data(value));
        } else {
            sendMessage(Message.ack("key_error"));
        }
    }

    /**
     * Handle PUT request (insert new key-value)
     */
    private void handlePut(String key, String value) {
        int hash = ConsistentHash.hash(key);
        ServerNode primary = hashRing.getSuccessor(hash);
        ServerNode replica = hashRing.getPredecessor(hash);

        if (primary == null || replica == null) {
            sendMessage(Message.ack("insufficient_servers"));
            return;
        }

        System.out.println("[PUT] Hash(" + key + ") = " + hash);
        System.out.println("[PUT] Primary: " + primary.getAddress() + " (pos " + primary.getHashPosition() + ")");
        System.out.println("[PUT] Replica: " + replica.getAddress() + " (pos " + replica.getHashPosition() + ")");

        // Store on primary server (OWN table)
        boolean primarySuccess = putOnSlave(primary, key, value, "own");

        // Store on replica server (PREV table)
        boolean replicaSuccess = putOnSlave(replica, key, value, "prev");

        if (primarySuccess && replicaSuccess) {
            sendMessage(Message.ack("put_success"));
        } else {
            sendMessage(Message.ack("put_failed"));
        }
    }

    /**
     * Handle UPDATE request
     */
    private void handleUpdate(String key, String value) {
        int hash = ConsistentHash.hash(key);
        ServerNode primary = hashRing.getSuccessor(hash);
        ServerNode replica = hashRing.getPredecessor(hash);

        if (primary == null || replica == null) {
            sendMessage(Message.ack("insufficient_servers"));
            return;
        }

        // Update on both servers
        boolean primarySuccess = updateOnSlave(primary, key, value, "own");
        boolean replicaSuccess = updateOnSlave(replica, key, value, "prev");

        if (primarySuccess && replicaSuccess) {
            // Invalidate cache
            cache.removeSafe(key);
            sendMessage(Message.ack("update_success"));
        } else {
            sendMessage(Message.ack("update_failed"));
        }
    }

    /**
     * Handle DELETE request
     */
    private void handleDelete(String key) {
        int hash = ConsistentHash.hash(key);
        ServerNode primary = hashRing.getSuccessor(hash);
        ServerNode replica = hashRing.getPredecessor(hash);

        if (primary == null || replica == null) {
            sendMessage(Message.ack("insufficient_servers"));
            return;
        }

        // Delete from both servers
        boolean primarySuccess = deleteFromSlave(primary, key, "own");
        boolean replicaSuccess = deleteFromSlave(replica, key, "prev");

        if (primarySuccess && replicaSuccess) {
            // Remove from cache
            cache.removeSafe(key);
            sendMessage(Message.ack("delete_success"));
        } else {
            sendMessage(Message.ack("delete_failed"));
        }
    }

    /**
     * Handle slave server registration
     */
    private void handleSlaveRegistration(Message message) {
        String address = message.getMessage();
        if (address == null || address.isEmpty()) {
            // Extract from socket if not in message
            address = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }

        int hash = ConsistentHash.hash(address);
        hashRing.insert(hash, address);

        System.out.println("[SLAVE] Registered: " + address + " at position " + hash);
        sendMessage(Message.ack("registration_successful"));
    }

    // Helper methods for slave communication

    private String getFromSlave(ServerNode server, String key, String table) {
        try (Socket slaveSocket = new Socket(server.getIpAddress(), server.getPort());
             PrintWriter slaveOut = new PrintWriter(slaveSocket.getOutputStream(), true);
             BufferedReader slaveIn = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {

            Message request = Message.request("get", key).setTable(table);
            slaveOut.println(request);

            String response = slaveIn.readLine();
            if (response != null) {
                Message respMsg = new Message(response);
                if ("data".equals(respMsg.getReqType())) {
                    return respMsg.getMessage();
                }
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to get from slave " + server.getAddress() + ": " + e.getMessage());
        }
        return null;
    }

    private boolean putOnSlave(ServerNode server, String key, String value, String table) {
        try (Socket slaveSocket = new Socket(server.getIpAddress(), server.getPort());
             PrintWriter slaveOut = new PrintWriter(slaveSocket.getOutputStream(), true);
             BufferedReader slaveIn = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {

            Message request = Message.request("put", key, value).setTable(table);
            slaveOut.println(request);

            String response = slaveIn.readLine();
            if (response != null) {
                Message respMsg = new Message(response);
                return "put_success".equals(respMsg.getMessage());
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to put on slave " + server.getAddress() + ": " + e.getMessage());
        }
        return false;
    }

    private boolean updateOnSlave(ServerNode server, String key, String value, String table) {
        try (Socket slaveSocket = new Socket(server.getIpAddress(), server.getPort());
             PrintWriter slaveOut = new PrintWriter(slaveSocket.getOutputStream(), true);
             BufferedReader slaveIn = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {

            Message request = Message.request("update", key, value).setTable(table);
            slaveOut.println(request);

            String response = slaveIn.readLine();
            if (response != null) {
                Message respMsg = new Message(response);
                return "update_success".equals(respMsg.getMessage());
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to update on slave " + server.getAddress() + ": " + e.getMessage());
        }
        return false;
    }

    private boolean deleteFromSlave(ServerNode server, String key, String table) {
        try (Socket slaveSocket = new Socket(server.getIpAddress(), server.getPort());
             PrintWriter slaveOut = new PrintWriter(slaveSocket.getOutputStream(), true);
             BufferedReader slaveIn = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {

            Message request = Message.request("delete", key).setTable(table);
            slaveOut.println(request);

            String response = slaveIn.readLine();
            if (response != null) {
                Message respMsg = new Message(response);
                return "delete_success".equals(respMsg.getMessage());
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to delete from slave " + server.getAddress() + ": " + e.getMessage());
        }
        return false;
    }

    private void sendMessage(Message message) {
        out.println(message.toString());
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
