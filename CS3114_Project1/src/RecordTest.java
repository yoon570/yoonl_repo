import student.TestCase;

/**
 * This class tests all the methods found in the Record class
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
public class RecordTest extends TestCase {
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
        Record record1 = new Record(1, handle1);
        Record record2 = new Record(2, handle2);

        // Expects record's id
        assertEquals(record1.getID(), 1);
        assertEquals(record2.getID(), 2);

        // Expects record's handle
        assertEquals(record1.getHandle(), handle1);
        assertEquals(record2.getHandle(), handle2);

        // Expects handle to start off not as a tomb
        assertFalse(record1.getTomb());
        assertFalse(record2.getTomb());

        // Expects true when handle is set to a tomb
        record2.setTomb();
        assertTrue(record2.getTomb());
    }
}
