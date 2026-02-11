package com.kvstore.common;

/**
 * Consistent hashing utility for distributing keys across servers
 */
public class ConsistentHash {
    private static final int RING_SIZE = 31;
    private static final int MULTIPLIER = 99999989;

    /**
     * Calculate hash position for a given string
     * @param s String to hash (key or server address)
     * @return Hash position (0 to RING_SIZE-1)
     */
    public static int hash(String s) {
        int j = RING_SIZE;
        int hashedVal = 0;

        for (int i = 0; i < s.length(); i++) {
            hashedVal = (hashedVal + (s.charAt(i) * MULTIPLIER) % j) % j;
        }

        return (hashedVal + j) % j;
    }

    public static int getRingSize() {
        return RING_SIZE;
    }
}
