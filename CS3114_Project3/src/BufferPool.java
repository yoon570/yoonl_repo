import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class creates an interface for the BufferPool data type.
 * This BufferPool ADT uses the message-passing method.
 * 
 * @author Yoon Lee (yoonl18)
 * @author Danny Yang (dannyy8)
 * @version 10.23.2023
 */

// On my honor:
//
// - I have not used source code obtained from another student,
// or any other unauthorized source, either modified or
// unmodified.
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

public class BufferPool {
    /**
     * Number of records
     */
    public static final int NUMREC = 1024;
    /**
     * Size of records
     */
    public static final int RECSIZE = 4;
    /**
     * Size of the Buffer
     */
    public static final int BUFFERSIZE = 4096;
    // Initial area of main memory that indexes data read from a disk
    private Buffer[] bufferPool;
    // Capacity of the BufferPool
    private RandomAccessFile file;
    // Stats array; stats[0] = # cache hits, [1] = # disk reads, [2] =
    // # disk writes
    private long[] stats;
    // Size of the BufferPool
    private int size;

    /**
     * Constructor for BufferPool
     * 
     * @param file
     *            file that BufferPool reads from
     * @param bufferNum
     *            number of buffers allocated for the pool
     * @throws IOException
     */
    public BufferPool(RandomAccessFile file, int bufferNum) throws IOException {
        // Creates an array of Buffers
        bufferPool = new Buffer[bufferNum];
        this.file = file;
        this.stats = new long[3];
        this.size = bufferNum;
    }


    /**
     * Copy "sz" bytes from "space" to position "pos" in the buffered storage
     * 
     * @param space
     *            initial place where data is contained
     * @param sz
     *            size of the bytes
     * @param pos
     *            position
     * @throws IOException
     */
    public void insert(byte[] space, int sz, int pos) throws IOException {
        this.update(pos);

        // Copies the elements of Buffer's byte array from space
//        byte[] copy = this.bufferPool[0].getData();
//        
//        int index = (pos * BufferPool.RECSIZE) % BufferPool.BUFFERSIZE;
//        
//        System.arraycopy(space, 0, copy, pos, pos);
//        
//        this.bufferPool[0].setData(copy);
        
        int index = (pos * BufferPool.RECSIZE) % BufferPool.BUFFERSIZE;
        
        this.bufferPool[0].updateRecord(space, index);
        this.bufferPool[0].setIsDirty(true);
    }


    /**
     * Copy "sz" bytes from position "pos" of the buffered storage to "space"
     * 
     * @param space
     *            initial place where data is contained
     * @param sz
     *            size of the bytes
     * @param pos
     *            position
     * @throws IOException
     */
    public void getbytes(byte[] space, int sz, int pos) throws IOException {
        this.update(pos);

        // Copies the elements of Buffer's byte array to space
        int index = (pos * BufferPool.RECSIZE) % BufferPool.BUFFERSIZE;
        System.arraycopy(bufferPool[0].getData(), index, space, 0, sz);
    }


    /**
     * Getter method for the stats array
     * 
     * @return this.stats
     */
    public long[] getStats() {
        return this.stats;
    }


    /**
     * Checks whether the BufferPool contains a Buffer by a specified
     * position
     * 
     * @param pos
     *            position
     * @return index of Buffer if it is in the pool
     */
    private int containsBuffer(int pos) {
        for (int i = 0; i < bufferPool.length; i++) {
            if (bufferPool[i] != null) {
                if (bufferPool[i].getPosition() == pos) {
                    return i;
                }
            }
        }

        return -1;
    }


    /**
     * Insert helper method by finding Buffer and setting it to the front
     * 
     * @param pos
     *            buffer's position
     * @throws IOException
     */
    private void update(int pos) throws IOException {
        // If specified buffer is not in the pool, reads one from RAF
        // and adds to the top of the BufferPool
        Buffer newBuffer = null;
        int index = containsBuffer(pos / BufferPool.NUMREC);
        if (index < 0) {
            file.seek((pos / BufferPool.NUMREC) * BufferPool.BUFFERSIZE);
            byte[] arr = new byte[BufferPool.BUFFERSIZE];
            file.read(arr, 0, BufferPool.BUFFERSIZE);
            newBuffer = new Buffer(arr, pos / BufferPool.NUMREC);
            this.addFront(newBuffer); // shift forward
        }
        else if (index != 0) {
            this.placeFirst(index);
        }
    }


    /**
     * Adds a Buffer object to the front and pushes the element
     * down the BufferPool
     * 
     * @param buffer
     *            Buffer object being added to the front of the BufferPool
     * @throws IOException
     */
    private void addFront(Buffer buffer) throws IOException {
        // Adds the new Buffer to the front and saves the last/removed element
        Buffer last = bufferPool[this.size - 1];
        
        // Writes the last, element to the file if it is not null and isDirty
        if (last != null) {
            if (last.getDirty()) {
                file.seek(last.getPosition() * BufferPool.BUFFERSIZE);
                file.write(last.getData());
            }
        }
        
        System.arraycopy(this.bufferPool, 0, this.bufferPool, 1, this.size - 1);
        // shifted[0] = buffer;


        bufferPool[0] = buffer;
    }


    /**
     * Places a specified Buffer object to the front and pushes the elements
     * before it down one
     * 
     * @param index
     *            index of the target Buffer
     * @throws IOException 
     */
    private void placeFirst(int index) throws IOException {
        // Sets specified Buffer as temp and shifts the elements before it
        // right by one
        // Adds the new Buffer to the front and saves the last/removed element
        Buffer last = bufferPool[index];
        
        
        System.arraycopy(this.bufferPool, 0, this.bufferPool, 1, index);
        // shifted[0] = buffer;


        bufferPool[0] = last;
    }


    /**
     * Flushes the BufferPool and closes the file
     * 
     * @throws IOException
     */
    public void flush() throws IOException {
        // Writes any dirty Buffer to the file
        for (int i = this.size - 1; i > -1; i--) {
            if (this.bufferPool[i] != null) {
                if (this.bufferPool[i].getDirty()) {
                    file.seek(this.bufferPool[i].getPosition()
                        * BufferPool.BUFFERSIZE);
                    file.write(this.bufferPool[i].getData());
                }
            }
        }
    }
}
