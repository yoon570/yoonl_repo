/**
 * This class manages the data of bytes using a memory pool (array) and a
 * FreeBlockList. Data are stored as "used blocks". Free blocks are the chunks
 * of places where data can be stored if the free block is big enough. Free
 * blocks can merge and split using the buddy system.
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

public class MemManager {
    // memSize is initial of the mempool. memTable serves as the
    // mempool. headNode starts the BL (block list).
    // tailNode ends it.
    private int memSize;
    private byte[] memTable;
    private Node headNode;
    private Node tailNode;
    private Node firstNode;

    /**
     * Constructor (Creates a MemoryManager object)
     * 
     * @param size
     *            initial size of the memory pool
     */
    public MemManager(int size) {
        memTable = new byte[size];
        memSize = size;

        headNode = new Node(0, true, null, null);
        tailNode = new Node(0, true, null, null);
        firstNode = new Node(size, false, tailNode, headNode);
        headNode.setNext(firstNode);
        tailNode.setPrev(firstNode);
    }


    /**
     * Adds the bytes from the input into a suitable block in the memory pool
     * and
     * returns a Handle with the starting position and size of byte "block" in
     * the
     * pool
     * 
     * @param input
     *            byte array being inputed into the memory pool
     * @param size
     *            length of the byte array
     * @return Handle with the starting position and length of byte array
     */
    public Handle insert(byte[] input, int size) {
        // Find the size of the block
        // Find a freeBlock from the freeBlock list of a given size
        Node currNode = headNode.getNext();
        int handleStart = 0;
        Handle output = null;
        int power2 = powerOf2(size);

        // Find an appropriate block
        while (currNode != tailNode && currNode != null) {
            if (!currNode.getIsFull()) {
                if (currNode.getSize() >= power2) {
                    break;
                }
            }
            handleStart += currNode.getSize();
            currNode = currNode.getNext();
        }

        if (currNode == tailNode) {
            return null;
        }

        // Split the node to an appropriate size
        while (currNode.getSize() > power2) {
            Node.splitNode(currNode);
        }

        for (int i = 0; i < size; i++) {
            memTable[handleStart + i] = input[i];
        }
        currNode.setIsFull(true);
        output = new Handle(handleStart, size);

        return output;
    }


    /**
     * Returns the length of the record at the starting position of the given
     * Handle
     * 
     * @param handle
     *            Handle whose record's length is being targeted
     * @return the Handle's record's length
     */
    public int length(Handle handle) {
        return handle.getSize();
    }


    /**
     * Removes the block associated with the handle
     * 
     * @param handle
     *            handle's whose block is being removed
     */
    public void remove(Handle handle) {
        Node currNode = headNode.getNext();
        int position = 0;

        if (handle == null) {
            return;
        }

        while (currNode != tailNode) {
            if (position == handle.getStart()) {
                break;
            }
            position += currNode.getSize();
            currNode = currNode.getNext();
        }

        for (int i = handle.getStart(); i < handle.getStart() + handle
            .getSize(); i++) {
            memTable[i] = 0;
        }

        currNode.setIsFull(false);

        cleanup(currNode);
    }


    /**
     * Returns the byte array from pool given by its handle and length through
     * reference
     * 
     * @param arr
     *            array being inputed through reference
     * @param handle
     *            handle's whose record's byte array is being returned
     * @param length
     *            size of the handle's record's byte array
     * @return handle's record's byte array
     */
    public int get(byte[] arr, Handle handle, int length) {

        for (int i = 0; i < length; i++) {
            arr[i] = memTable[i + handle.getStart()];
        }

        return arr.length;
    }


    /**
     * Prints out all the free blocks from the FBL
     * 
     * @return string of all the free blocks
     */
    public String dump() {
        Node currNode = headNode.getNext();
        int position = 0;
        String str = "";

        while (currNode != tailNode && currNode != null) { // FLAGGED
            if (!currNode.getIsFull()) { // FLAGGED
                str += (currNode.getSize() + ": " + position + "\n");
            }

            position += currNode.getSize();
            currNode = currNode.getNext();
        }

        if (str.equals("")) {
            str += "There are no freeblocks in the memory pool\n";
        }

        return str;
    }


    /**
     * Checks to see if the pool has enough space for an array of the given
     * length.
     * If not, the pool will be doubled
     * 
     * @param length
     *            size of the byte array
     * @return true if there is not enough space and pool doubles, false if
     *         there is
     *         enough space and pool does not double
     */
    public boolean doubleSize(int length) {
        Node currNode = headNode.getNext();

        // If the memory pool is smaller than the expected length to be
        // inserted, then the pool will continually double until its large
        // enough
        doublePool(length);

        // Edge case for when there is one free block for whole thing
        if (currNode.getNext() == tailNode && !currNode.getIsFull()) {
            return false;
        }
        else {
            while (currNode.getNext() != tailNode) {
                currNode = currNode.getNext();
                if (!currNode.getIsFull() && currNode.getSize() >= length) {
                    return false;
                }
            }

            byte[] tempPool = new byte[2 * memSize];
            for (int i = 0; i < memSize; i++) {
                tempPool[i] = memTable[i];
            }
            memTable = tempPool;
            currNode.setNext(new Node(memSize, false, null, currNode));
            memSize *= 2;

            return true;
        }
    }


    /**
     * Getter method for the memory size of the pool
     * 
     * @return memSize
     */
    public int getSize() {
        return this.memSize;
    }


    /**
     * Prints all the nodes in order with their size
     */

    public void print() {
        Node curr = headNode.getNext();
        while (curr != tailNode && curr != null) {
            System.out.print(curr.getSize() + " " + curr.getIsFull() + "\n");
            curr = curr.getNext();
        }
    }


    /**
     * Finds the nearest power of 2 greater than the given int
     * 
     * @return nearest power of 2 greater than given int
     */
    private int powerOf2(int input) {
        int output = 1;
        while (output < input) {
            output *= 2;
        }
        return output;
    }


    /**
     * Doubles the pool when length > memSize
     * 
     * @param length
     *            length being compared
     */
    private void doublePool(int length) {
        // If the memory pool is smaller than the expected length to be
        // inserted, then the pool will continually double until its large
        // enough
        if (memSize < length) {
            while (memSize < length) {
                byte[] tempPool = new byte[2 * memSize];
                for (int i = 0; i < memSize; i++) {
                    tempPool[i] = memTable[i];
                }
                memTable = tempPool;
                memSize *= 2;
            }
        }
    }


    /**
     * Deals with all the merges after remove, thus cleaning up
     * 
     * @param cleanNode
     *            starting Node position in block list
     */
    private void cleanup(Node cleanNode) {
        Node currNode = cleanNode;

        if (cleanNode.getSize() == memSize) {
            return;
        }

        while (currNode.getPrev() != headNode && currNode.getPrev() != null) {
            if (!currNode.getPrev().getIsFull() && currNode.getPrev()
                .getSize() <= (currNode.getSize() / 2)) {
                currNode = currNode.getPrev();
            }
            else {
                break;
            }
        }

        while (currNode.getNext() != tailNode && currNode.getNext() != null) {
            if (currNode.getSize() == currNode.getNext().getSize() && !currNode
                .getIsFull() && !currNode.getNext().getIsFull()) {
                Node.mergeNodes(currNode, currNode.getNext());
            }
            else {
                break;
            }
        }
    }
}
