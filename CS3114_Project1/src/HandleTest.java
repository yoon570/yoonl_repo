import student.TestCase;

/**
 * This class tests all the methods found in the Handle class
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

public class HandleTest extends TestCase {
    /**
     * Sets up any basic foundations used for the HashTable testing
     */
    public void setUp() {
        // Nothing to do here
    }


    /**
     * Tests all the getter and setter methods
     */
    public void testGetSet() {
        Handle handle1 = new Handle(1, 2);
        Handle handle2 = new Handle(3, 4);

        // Expects handle's starting position
        assertEquals(handle1.getStart(), 1);
        assertEquals(handle2.getStart(), 3);

        // Expects handle's size (length)
        assertEquals(handle1.getSize(), 2);
        assertEquals(handle2.getSize(), 4);
    }
}
