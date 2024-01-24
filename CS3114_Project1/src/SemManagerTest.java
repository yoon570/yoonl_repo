import java.io.FileNotFoundException;
import java.io.IOException;
import student.TestCase;

/**
 * Tests all the methods and exceptions in SemManager
 * 
 * @author Danny Yang (dannyy8)
 * @author Yoon Lee (yoonl18)
 * @version August 30, 2023
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

public class SemManagerTest extends TestCase {
    /**
     * Sets up the tests that follow. In general, used for initialization
     */
    public void setUp() {
        // Nothing here to set up
    }


    /**
     * Get code coverage of the class declaration.
     * 
     * @throws Exception
     */
    public void testMInitx() throws Exception {
        // Creates a non-null SemManager
        SemManager sem = new SemManager();
        assertNotNull(sem);

        // Expects thrown exception with one null argument
        Exception exception1 = null;
        try {
            SemManager.main(null);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception1 = e;
        }
        assertNotNull(exception1);
        assertTrue(exception1 instanceof IOException);

        // Expects thrown exception with argument length > 3
        Exception exception2 = null;
        try {
            String[] arr = new String[] { "9", "4", "file.txt", "hi" };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception2 = e;
        }
        assertNotNull(exception2);
        assertTrue(exception2 instanceof IOException);

        // Expects thrown exception with null file
        Exception exception3 = null;
        try {
            String[] arr = new String[] { "512", "4", null };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception3 = e;
        }
        assertNotNull(exception3);
        assertTrue(exception3 instanceof IOException);

        // Expects thrown exception with file that cannot be found
        Exception exception4 = null;
        try {
            String[] arr = new String[] { "512", "4", "file.txt" };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception4 = e;
        }
        assertNotNull(exception4);
        assertTrue(exception4 instanceof FileNotFoundException);

        // Expects thrown exception when memSize is not a power of 2
        Exception exception5 = null;
        try {
            String[] arr = new String[] { "9", "4", "P1Sample_input.txt" };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception5 = e;
        }
        assertNotNull(exception5);
        assertTrue(exception5 instanceof IOException);

        // Expects thrown exception when memSize = 0
        Exception exception6 = null;
        try {
            String[] arr = new String[] { "0", "4", "P1Sample_input.txt" };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception6 = e;
        }
        assertNotNull(exception6);
        assertTrue(exception6 instanceof IOException);

        // Expects thrown exception when hashSize is not a power of 2
        Exception exception7 = null;
        try {
            String[] arr = new String[] { "64", "99", "P1Sample_input.txt" };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception7 = e;
        }
        assertNotNull(exception7);
        assertTrue(exception7 instanceof IOException);

        // Expects main function to function without exceptions
        Exception exception8 = null;
        try {
            String[] arr = new String[] { "512", "4", "P1Sample_input.txt" };
            SemManager.main(arr);
        }
        catch (Exception e) {
            e.printStackTrace();
            exception8 = e;
        }
        assertNull(exception8);

        // Expects thrown exception with argument length < 3
        Exception exception9 = null;
        try {
            String[] arr = new String[] { "512", "4"};
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception9 = e;
        }
        assertNotNull(exception9);
        assertTrue(exception9 instanceof IOException);

        // Expects thrown exception with argument length = 0
        Exception exception10 = null;
        try {
            String[] arr = new String[] { "9", "4", "file.txt", "hi" };
            SemManager.main(arr);
        }
        catch (IOException e) {
            e.printStackTrace();
            exception10 = e;
        }
        assertNotNull(exception10);
        assertTrue(exception10 instanceof IOException);
    }
}
