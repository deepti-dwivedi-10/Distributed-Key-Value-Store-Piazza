package com.kvstore.slave;

import com.kvstore.common.Message;
import java.io.*;
import java.net.Socket;

/**
 * Handles individual requests to the slave server
 * Processes GET, PUT, UPDATE, DELETE operations
 */
public class RequestHandler implements Runnable {
    private final Socket socket;
    private final DataStore dataStore;
    private BufferedReader in;
    private PrintWriter out;

    public RequestHandler(Socket socket, DataStore dataStore) {
        this.socket = socket;
        this.dataStore = dataStore;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read request
            String requestStr = in.readLine();
            if (requestStr == null || requestStr.isEmpty()) {
                return;
            }

            System.out.println("[REQUEST] Received: " + requestStr);

            // Process request
            Message request = new Message(requestStr);
            String reqType = request.getReqType();
            String key = request.getKey();
            String table = request.getTable();

            switch (reqType) {
                case "get":
                    handleGet(key, table);
                    break;
                case "put":
                    handlePut(key, request.getValue(), table);
                    break;
                case "update":
                    handleUpdate(key, request.getValue(), table);
                    break;
                case "delete":
                    handleDelete(key, table);
                    break;
                default:
                    sendMessage(Message.ack("unknown_request"));
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Error handling request: " + e.getMessage());
            sendMessage(Message.ack("error"));
        } finally {
            closeConnection();
        }
    }

    private void handleGet(String key, String table) {
        String value = dataStore.get(key, table);
        if (value != null) {
            System.out.println("[GET] Key '" + key + "' found in " + table + " table: " + value);
            sendMessage(Message.data(value));
        } else {
            System.out.println("[GET] Key '" + key + "' not found in " + table + " table");
            sendMessage(Message.ack("key_error"));
        }
    }

    private void handlePut(String key, String value, String table) {
        dataStore.put(key, value, table);
        System.out.println("[PUT] Stored in " + table + " table: " + key + " = " + value);
        sendMessage(Message.ack("put_success"));
    }

    private void handleUpdate(String key, String value, String table) {
        if (dataStore.update(key, value, table)) {
            System.out.println("[UPDATE] Updated in " + table + " table: " + key + " = " + value);
            sendMessage(Message.ack("update_success"));
        } else {
            System.out.println("[UPDATE] Key '" + key + "' not found in " + table + " table");
            sendMessage(Message.ack("key_error"));
        }
    }

    private void handleDelete(String key, String table) {
        if (dataStore.delete(key, table)) {
            System.out.println("[DELETE] Deleted from " + table + " table: " + key);
            sendMessage(Message.ack("delete_success"));
        } else {
            System.out.println("[DELETE] Key '" + key + "' not found in " + table + " table");
            sendMessage(Message.ack("key_error"));
        }
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
