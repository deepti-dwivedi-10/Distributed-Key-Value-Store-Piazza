package com.kvstore.common;

/**
 * AVL Tree-based Hash Ring for consistent hashing
 * Maintains servers in sorted order for efficient lookup
 */
public class HashRing {
    private Node root;

    private static class Node {
        int key;
        String address;
        Node left, right;
        int height;

        Node(int key, String address) {
            this.key = key;
            this.address = address;
            this.height = 1;
        }
    }

    // Get height of node
    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    // Get balance factor
    private int getBalance(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    // Right rotate
    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        x.right = y;
        y.left = T2;

        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        return x;
    }

    // Left rotate
    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        y.left = x;
        x.right = T2;

        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        return y;
    }

    /**
     * Insert a server into the hash ring
     */
    public synchronized void insert(int hashPosition, String address) {
        root = insertNode(root, hashPosition, address);
        System.out.println("Inserted server at position " + hashPosition + ": " + address);
    }

    private Node insertNode(Node node, int key, String address) {
        if (node == null) {
            return new Node(key, address);
        }

        if (key < node.key) {
            node.left = insertNode(node.left, key, address);
        } else if (key > node.key) {
            node.right = insertNode(node.right, key, address);
        } else {
            return node; // Duplicate key
        }

        node.height = 1 + Math.max(height(node.left), height(node.right));

        int balance = getBalance(node);

        // Left Left
        if (balance > 1 && key < node.left.key) {
            return rightRotate(node);
        }

        // Right Right
        if (balance < -1 && key > node.right.key) {
            return leftRotate(node);
        }

        // Left Right
        if (balance > 1 && key > node.left.key) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // Right Left
        if (balance < -1 && key < node.right.key) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    /**
     * Delete a server from the hash ring
     */
    public synchronized void delete(int hashPosition) {
        root = deleteNode(root, hashPosition);
        System.out.println("Deleted server at position " + hashPosition);
    }

    private Node deleteNode(Node root, int key) {
        if (root == null) {
            return root;
        }

        if (key < root.key) {
            root.left = deleteNode(root.left, key);
        } else if (key > root.key) {
            root.right = deleteNode(root.right, key);
        } else {
            if ((root.left == null) || (root.right == null)) {
                Node temp = root.left != null ? root.left : root.right;
                if (temp == null) {
                    root = null;
                } else {
                    root = temp;
                }
            } else {
                Node temp = minValueNode(root.right);
                root.key = temp.key;
                root.address = temp.address;
                root.right = deleteNode(root.right, temp.key);
            }
        }

        if (root == null) {
            return root;
        }

        root.height = Math.max(height(root.left), height(root.right)) + 1;

        int balance = getBalance(root);

        // Left Left
        if (balance > 1 && getBalance(root.left) >= 0) {
            return rightRotate(root);
        }

        // Left Right
        if (balance > 1 && getBalance(root.left) < 0) {
            root.left = leftRotate(root.left);
            return rightRotate(root);
        }

        // Right Right
        if (balance < -1 && getBalance(root.right) <= 0) {
            return leftRotate(root);
        }

        // Right Left
        if (balance < -1 && getBalance(root.right) > 0) {
            root.right = rightRotate(root.right);
            return leftRotate(root);
        }

        return root;
    }

    private Node minValueNode(Node node) {
        Node current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    private Node maxValueNode(Node node) {
        Node current = node;
        while (current.right != null) {
            current = current.right;
        }
        return current;
    }

    /**
     * Find the successor (next server clockwise on the ring) for a given hash
     */
    public synchronized ServerNode getSuccessor(int hash) {
        Node successor = findSuccessor(root, hash);
        if (successor == null) {
            successor = minValueNode(root);
        }
        if (successor == null) {
            return null;
        }
        return new ServerNode(successor.key, successor.address);
    }

    private Node findSuccessor(Node node, int hash) {
        if (node == null) {
            return null;
        }

        if (hash < node.key) {
            Node leftResult = findSuccessor(node.left, hash);
            return leftResult != null ? leftResult : node;
        } else if (hash > node.key) {
            return findSuccessor(node.right, hash);
        } else {
            return node;
        }
    }

    /**
     * Find the predecessor (previous server counter-clockwise on the ring)
     */
    public synchronized ServerNode getPredecessor(int hash) {
        Node predecessor = findPredecessor(root, hash);
        if (predecessor == null) {
            predecessor = maxValueNode(root);
        }
        if (predecessor == null) {
            return null;
        }
        return new ServerNode(predecessor.key, predecessor.address);
    }

    private Node findPredecessor(Node node, int hash) {
        if (node == null) {
            return null;
        }

        if (hash > node.key) {
            Node rightResult = findPredecessor(node.right, hash);
            return rightResult != null ? rightResult : node;
        } else if (hash < node.key) {
            return findPredecessor(node.left, hash);
        } else {
            return node;
        }
    }

    /**
     * Check if ring is empty
     */
    public synchronized boolean isEmpty() {
        return root == null;
    }

    /**
     * Get server count
     */
    public synchronized int getServerCount() {
        return countNodes(root);
    }

    private int countNodes(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + countNodes(node.left) + countNodes(node.right);
    }

    /**
     * Display ring contents (for debugging)
     */
    public synchronized void display() {
        System.out.println("=== Hash Ring (Servers: " + getServerCount() + ") ===");
        displayInOrder(root);
        System.out.println("=============================================");
    }

    private void displayInOrder(Node node) {
        if (node != null) {
            displayInOrder(node.left);
            System.out.println("  Position " + node.key + ": " + node.address);
            displayInOrder(node.right);
        }
    }
}
