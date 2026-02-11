package com.kvstore.slave;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Data storage for slave server
 * Maintains two tables:
 * - OWN table: Primary data this server is responsible for
 * - PREV table: Replica data from predecessor server
 */
public class DataStore {
    private final ConcurrentHashMap<String, String> ownTable;
    private final ConcurrentHashMap<String, String> prevTable;

    public DataStore() {
        this.ownTable = new ConcurrentHashMap<>();
        this.prevTable = new ConcurrentHashMap<>();
    }

    /**
     * Get value from specified table
     */
    public String get(String key, String table) {
        ConcurrentHashMap<String, String> targetTable = getTable(table);
        return targetTable.get(key);
    }

    /**
     * Put key-value in specified table
     */
    public void put(String key, String value, String table) {
        ConcurrentHashMap<String, String> targetTable = getTable(table);
        targetTable.put(key, value);
    }

    /**
     * Update key-value in specified table
     * Returns true if key exists, false otherwise
     */
    public boolean update(String key, String value, String table) {
        ConcurrentHashMap<String, String> targetTable = getTable(table);
        if (targetTable.containsKey(key)) {
            targetTable.put(key, value);
            return true;
        }
        return false;
    }

    /**
     * Delete key from specified table
     * Returns true if key existed, false otherwise
     */
    public boolean delete(String key, String table) {
        ConcurrentHashMap<String, String> targetTable = getTable(table);
        return targetTable.remove(key) != null;
    }

    /**
     * Check if key exists in specified table
     */
    public boolean containsKey(String key, String table) {
        ConcurrentHashMap<String, String> targetTable = getTable(table);
        return targetTable.containsKey(key);
    }

    /**
     * Get the appropriate table (OWN or PREV)
     */
    private ConcurrentHashMap<String, String> getTable(String table) {
        return "own".equalsIgnoreCase(table) ? ownTable : prevTable;
    }

    /**
     * Display contents of both tables (for debugging)
     */
    public void display() {
        System.out.println("\n=== Data Store Contents ===");
        System.out.println("OWN Table (Primary): " + ownTable.size() + " entries");
        ownTable.forEach((k, v) -> System.out.println("  " + k + " = " + v));

        System.out.println("PREV Table (Replica): " + prevTable.size() + " entries");
        prevTable.forEach((k, v) -> System.out.println("  " + k + " = " + v));
        System.out.println("===========================\n");
    }

    /**
     * Get statistics
     */
    public int getOwnTableSize() {
        return ownTable.size();
    }

    public int getPrevTableSize() {
        return prevTable.size();
    }
}
