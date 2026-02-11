package com.kvstore.common;

/**
 * Represents a server node in the distributed system
 */
public class ServerNode {
    private final int hashPosition;
    private final String ipAddress;
    private final int port;
    private final String address; // ip:port

    public ServerNode(int hashPosition, String ipAddress, int port) {
        this.hashPosition = hashPosition;
        this.ipAddress = ipAddress;
        this.port = port;
        this.address = ipAddress + ":" + port;
    }

    public ServerNode(int hashPosition, String address) {
        this.hashPosition = hashPosition;
        this.address = address;
        String[] parts = address.split(":");
        this.ipAddress = parts[0];
        this.port = Integer.parseInt(parts[1]);
    }

    public int getHashPosition() {
        return hashPosition;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "ServerNode{" +
                "position=" + hashPosition +
                ", address='" + address + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerNode that = (ServerNode) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
