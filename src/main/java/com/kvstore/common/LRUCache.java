package com.kvstore.common;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) Cache implementation
 * Thread-safe implementation with synchronized methods
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true); // true = access-order (LRU)
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    // Thread-safe get
    public synchronized V getSafe(K key) {
        return super.get(key);
    }

    // Thread-safe put
    public synchronized V putSafe(K key, V value) {
        return super.put(key, value);
    }

    // Thread-safe containsKey
    public synchronized boolean containsKeySafe(K key) {
        return super.containsKey(key);
    }

    // Thread-safe remove
    public synchronized V removeSafe(K key) {
        return super.remove(key);
    }

    // Display cache contents (for debugging)
    public synchronized void display() {
        System.out.println("=== Cache Contents (Size: " + size() + "/" + capacity + ") ===");
        if (isEmpty()) {
            System.out.println("  [Empty]");
        } else {
            forEach((key, value) -> System.out.println("  " + key + " = " + value));
        }
        System.out.println("=================================================");
    }
}
