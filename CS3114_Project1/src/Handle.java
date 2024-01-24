/**
 * This class implements the Handle object
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

public class Handle
{
    //Start is the starting position of the Seminar object in 
    //memory pool. Size is the length of the Seminar object in 
    //the mempool.
    private int start;
    private int size;

    /**
     * Constructor for the Handle object
     * 
     * @param recordStart
     *            starting position indicated by Handle
     * @param recordSize
     *            length of the block indicated by Handle
     */
    public Handle(int recordStart, int recordSize) {
        this.start = recordStart;
        this.size = recordSize;
    }

    /**
     * Getter method for the position of the Handle
     * 
     * @return position of the Handle
     */
    public int getStart() {
        return this.start;
    }

    /**
     * Getter method for the length of the Handle
     * 
     * @return length of the Handle
     */
    public int getSize() {
        return this.size;
    }
}
