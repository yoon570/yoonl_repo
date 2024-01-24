/**
 * This class implements the Node data structure, with each node
 * representing a
 * FreeBlock in the MemoryManager.
 * 
 * @author Danny Yang (dannyy8)
 * @author Yoon Lee (yoonl18)
 * @version September 11, 2023
 */

// On my honor:
// - I have not used source code obtained from another current or
// former student, or any other unauthorized source, either
// modified or unmodified.
//
// - All source code and documentation used in my program is
// either my original work, or was derived by me from the
// source code published in the textbook for this course.
//
// - I have not discussed coding details about this project with
// anyone other than my partner (in the case of a joint
// submission), instructor, ACM/UPE tutors or the TAs assigned
// to this course. I understand that I may discuss the concepts
// of this program with other students, and that another student
// may help me debug my program so long as neither of us writes
// anything during the discussion or modifies any computer file
// during the discussion. I have violated neither the spirit nor
// letter of this restriction.

public class Node {
    // As Node essentially serves as block, Node points to its
    // previous Node, its next Node, has a size (power of 2),
    // and a boolean isFull to check whether it is full or not.
    // It also has a isBuddy field used for buddy system. 
    private Node prev;
    private Node next;
    private int size;
    private boolean isFull;
    private int buddy; 

    /**
     * Constructor for Node objects.
     * 
     * @param size
     *            int size of each node
     * @param isFull
     *            boolean if the node is full
     * @param next
     *            Node's next node
     * @param prev
     *            Node next node
     */
    public Node(int size, boolean isFull, Node next, Node prev) {
        this.next = next;
        this.size = size;
        this.isFull = isFull;
        this.prev = prev;
        this.buddy = 0;
    }


    /**
     * Getter method for the next node.
     * 
     * @return this.next
     *         Next Node object
     */
    public Node getNext() {
        return this.next;
    }


    /**
     * Getter method for Node's size
     * 
     * @return this.size
     *         int size of the block
     */
    public int getSize() {
        return this.size;
    }


    /**
     * Getter method for whether Node is full or not
     * 
     * @return this.isFull
     */
    public boolean getIsFull() {
        return this.isFull;
    }


    /**
     * Getter method for the previous Node
     * 
     * @return this.prev
     */
    public Node getPrev() {
        return this.prev;
    }
    
    /**
     * Getter method for the buddy integer
     * 
     * @return this.buddy
     */
    public int getBuddy() {
        return this.buddy; 
    }


    /**
     * Sets the next node to the given node
     * 
     * @param next
     *            node that is being set to the next node to the current one
     */
    public void setNext(Node next) {
        this.next = next;
    }


    /**
     * Setter method for the Node's size
     * 
     * @param size
     *            new size of the Node
     */
    public void setSize(int size) {
        this.size = size;
    }


    /**
     * Setter method for whether a Node is full or not
     * 
     * @param isFull
     *            new boolean value of the Node
     */
    public void setIsFull(boolean isFull) {
        this.isFull = isFull;
    }


    /**
     * Setter method for previous Node
     * 
     * @param prev
     *            new Node being set to this Node's previous
     */
    public void setPrev(Node prev) {
        this.prev = prev;
    }
    
    /**
     * Setter method for buddy 
     * 
     * @param buddy 
     *          new int being set to buddy 
     */
    public void setBuddy(int buddy)
    {
        this.buddy = buddy;
    }


    /**
     * Splits the node into two nodes with half original size
     * 
     * @param node
     *            Node being split into two
     */
    public static void splitNode(Node node) {
        Node node2 = new Node(node.getSize() / 2, false, node.getNext(), node);
        node.setSize(node.getSize() / 2);
        node.setNext(node2);
        
        int buddyVal = node.getBuddy();
        
        node.setBuddy(buddyVal + 1);
        node2.setBuddy(buddyVal + 1);
    }

    /**
     * Merges two nodes into one full node (for buddy system)
     * 
     * @param fNode
     *            first of the two split identical nodes
     * @param sNode
     *            second of the two split nodes
     */
    public static void mergeNodes(Node fNode, Node sNode) {
        if (fNode.getBuddy() == sNode.getBuddy()) {
            fNode.setSize(fNode.getSize() + sNode.getSize());
            fNode.setNext(sNode.getNext());
            fNode.setBuddy(fNode.getBuddy() - 1);
        }
    }
}