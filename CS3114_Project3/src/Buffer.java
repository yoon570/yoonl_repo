import java.nio.ByteBuffer;

/**
 * This class creates the Buffer object, which stores a byte
 * array and takes up the space within a BufferPool.
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

public class Buffer {
    // Data it holds
    private byte[] data;
    // Position index of the Buffer
    private int pos;
    // Whether the Buffer is dirty or not
    private boolean isDirty;

    /**
     * Constructor for Buffer object
     * 
     * @param d
     *            data (byte array) that the Buffer will hold
     * @param p
     *            position of the data in the disk
     */
    public Buffer(byte[] d, int p) {
        this.data = new byte[d.length];
        this.setData(d);
        this.pos = p;
        this.isDirty = false;
    }
    
    /**
     * Getter method for data
     * 
     * @return this.data
     */
    public byte[] getData()
    {
        return this.data;
    }
    
    /**
     * Getter method for position index
     * 
     * @return this.pos
     */
    public int getPosition()
    {
        return this.pos;
    }
    
    /**
     * Getter method for isDirty()
     * 
     * @return this.isDirty
     */
    public boolean getDirty()
    {
        return this.isDirty;
    }

    /**
     * Setter method for data
     * 
     * @param newData
     *              new given data
     */
    public void setData(byte[] newData)
    {
        System.arraycopy(newData, 0, this.data, 0, newData.length);
    }
    
    /**
     * Setter method for isDirty
     * 
     * @param state
     *              whether Buffer is dirty (true) or clean (false)
     */
    public void setIsDirty(boolean state)
    {
        this.isDirty = state;
    }
    
    /**
     * @param dat
     * @param pos
     */
    public void updateRecord(byte[] dat, int pos) {
        System.arraycopy(dat, 0, this.data, pos, dat.length);
    }
}
