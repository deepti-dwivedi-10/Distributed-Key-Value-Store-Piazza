package com.kvstore.client;

import com.kvstore.common.Message;
import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

/**
 * Client for accessing the distributed key-value store
 * Connects to Coordination Server to perform operations
 */
public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String csIP;
    private int csPort;

    public void connect() throws IOException {
        // Load CS configuration
        Properties props = loadCoordinatorConfig();
        csIP = props.getProperty("ip");
        csPort = Integer.parseInt(props.getProperty("port"));

        System.out.println("Connecting to Coordination Server at " + csIP + ":" + csPort);

        // Connect to CS
        socket = new Socket(csIP, csPort);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Wait for connection acknowledgment
        String ack = in.readLine();
        System.out.println("Server: " + ack);

        // Send client identification
        Message idMsg = new Message().setId("client");
        out.println(idMsg);

        // Wait for ready signal
        String ready = in.readLine();
        System.out.println("Server: " + ready);

        System.out.println("\n================================");
        System.out.println("Connected to Distributed KV Store!");
        System.out.println("================================\n");
    }

    public void run() throws IOException {
        Scanner scanner = new Scanner(System.in);

        printHelp();

        while (true) {
            System.out.print("command >> ");
            String command = scanner.nextLine().trim();

            if (command.isEmpty()) {
                continue;
            }

            if ("exit".equalsIgnoreCase(command)) {
                System.out.println("\nTATA!!!!");
                break;
            }

            if ("help".equalsIgnoreCase(command)) {
                printHelp();
                continue;
            }

            if ("clear".equalsIgnoreCase(command)) {
                clearScreen();
                continue;
            }

            // Process command
            processCommand(command);
        }

        scanner.close();
        socket.close();
    }

    private void processCommand(String command) {
        String[] parts = command.split(":", 3);

        if (parts.length < 2) {
            System.out.println("Error: Invalid format!");
            System.out.println("Format: <command>:<key>[:<value>]");
            return;
        }

        String cmdType = parts[0].toLowerCase();
        String key = parts[1];
        String value = parts.length > 2 ? parts[2] : "";

        try {
            Message request;
            switch (cmdType) {
                case "get":
                    request = Message.request("get", key);
                    break;
                case "put":
                    if (value.isEmpty()) {
                        System.out.println("Error: PUT requires a value!");
                        return;
                    }
                    request = Message.request("put", key, value);
                    break;
                case "update":
                    if (value.isEmpty()) {
                        System.out.println("Error: UPDATE requires a value!");
                        return;
                    }
                    request = Message.request("update", key, value);
                    break;
                case "delete":
                    request = Message.request("delete", key);
                    break;
                default:
                    System.out.println("Error: Unknown command '" + cmdType + "'");
                    return;
            }

            // Send request
            out.println(request);

            // Receive response
            String response = in.readLine();
            if (response != null) {
                Message respMsg = new Message(response);
                displayResponse(cmdType, key, respMsg);
            } else {
                System.out.println("Error: No response from server");
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void displayResponse(String cmdType, String key, Message respMsg) {
        String reqType = respMsg.getReqType();
        String message = respMsg.getMessage();

        if ("data".equals(reqType)) {
            // GET response
            System.out.println("✓ Value for '" + key + "' is: " + message);
        } else if ("ack".equals(reqType)) {
            // Acknowledgment response
            switch (message) {
                case "put_success":
                    System.out.println("✓ PUT successful: " + key);
                    break;
                case "update_success":
                    System.out.println("✓ UPDATE successful: " + key);
                    break;
                case "delete_success":
                    System.out.println("✓ DELETE successful: " + key);
                    break;
                case "key_error":
                    System.out.println("✗ Key not found: " + key);
                    break;
                case "no_servers_available":
                    System.out.println("✗ Error: No slave servers available");
                    break;
                case "insufficient_servers":
                    System.out.println("✗ Error: Insufficient servers for replication");
                    break;
                default:
                    System.out.println("Server response: " + message);
            }
        } else {
            System.out.println("Server response: " + respMsg);
        }
    }

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

    private void printHelp() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   Distributed Key-Value Store Client   ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║ Commands:                              ║");
        System.out.println("║                                        ║");
        System.out.println("║   put:key:value    Insert new data     ║");
        System.out.println("║   get:key          Retrieve data       ║");
        System.out.println("║   update:key:value Modify data         ║");
        System.out.println("║   delete:key       Remove data         ║");
        System.out.println("║   help             Show this help      ║");
        System.out.println("║   clear            Clear screen        ║");
        System.out.println("║   exit             Exit client         ║");
        System.out.println("║                                        ║");
        System.out.println("║ Examples:                              ║");
        System.out.println("║   put:username:alice                   ║");
        System.out.println("║   get:username                         ║");
        System.out.println("║   update:username:bob                  ║");
        System.out.println("║   delete:username                      ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void main(String[] args) {
        Client client = new Client();

        try {
            client.connect();
            client.run();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
