/**
 * This class implements the Record class which stores the
 * Handle and its key
 * 
 * @author Danny Yang (dannyy8)
 * @author Yoon Lee (yoonl18)
 * @version September 10, 2023
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

public class Record {
    // Fields of record: Key-Value pair is id-handle
    //isTomb is the boolean that makes Record a tombstone or not
    private int id;
    private Handle handle;
    private boolean isTomb;

    /**
     * Constructor (Creates a Record object)
     * 
     * @param key
     *            id value associated with the Handle
     * @param handle
     *            Handle object associated with the id (key)
     */
    public Record(int key, Handle handle) {
        this.id = key;
        this.handle = handle;
        this.isTomb = false;
    }


    /**
     * Getter method for the id
     * 
     * @return this.id
     */
    public int getID() {
        return this.id;
    }


    /**
     * Getter method for the Handle
     * 
     * @return this.handle
     */
    public Handle getHandle() {
        return this.handle;
    }


    /**
     * Getter method for whether Record is tomb or not
     * 
     * @return true if it is, false if not
     */
    public boolean getTomb() {
        return this.isTomb;
    }


    /**
     * Sets Record to a tombstone
     */
    public void setTomb() {
        this.isTomb = true;
    }
}
