import java.io.FileNotFoundException;
import student.TestCase;

/**
 * Tests all the methods and exceptions from CommandParser
 * 
 * @author Danny Yang (dannyy8)
 * @author Yoon Lee (yoonl18)
 * @version September 5, 2023
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

public class CommandParserTest extends TestCase {
	/**
	 * Sets up the tests that follow. In general, used for initialization
	 */
	public void setUp() {
		// Nothing here to set up
	}

	/**
	 * Tests the Constructor to see if exception is thrown
	 */
	public void testCommandParser() {
		SeminarDB db = new SeminarDB(4, 512);

		// Expects thrown exception with null as file
		String test = null;
		Exception exception1 = null;
		try {
			CommandParser cp1 = new CommandParser(db, test);
		} catch (Exception e) {
			e.printStackTrace();
			exception1 = e;
		}
		assertNotNull(exception1);
		assertTrue(exception1 instanceof Exception);

		// Expects thrown exception with file that is not there
		Exception exception2 = null;
		try {
			CommandParser cp2 = new CommandParser(db, "file.txt");
		} catch (Exception e) {
			e.printStackTrace();
			exception2 = e;
		}
		assertNotNull(exception2);
		assertTrue(exception2 instanceof FileNotFoundException);

		// Expects no exceptions when passed with correct params
		Exception exception3 = null;
		try {
			CommandParser cp2 = new CommandParser(db, "P1Sample_input.txt");
		} catch (Exception e) {
			e.printStackTrace();
			exception3 = e;
		}
		assertNull(exception3);

		// Expects thrown exception when both params are null
		Exception exception4 = null;
		try {
			CommandParser cp4 = new CommandParser(null, null);
		} catch (Exception e) {
			e.printStackTrace();
			exception4 = e;
		}
		assertNotNull(exception4);
		assertTrue(exception4 instanceof Exception);

		// Expects no exceptions when passed with correct params
		// for different file
		Exception exception5 = null;
		SeminarDB db2 = new SeminarDB(256, 16);
		try {
			CommandParser cp5 = new CommandParser(db2, "emptySearch.txt");
		} catch (Exception e) {
			e.printStackTrace();
			exception5 = e;
		}
		assertNull(exception5);

		// Expects no exceptions when passed with correct params
		// for a whole different file
		Exception exception6 = null;
		SeminarDB db3 = new SeminarDB(8, 512);
		try {
			CommandParser cp6 = new CommandParser(db3, "oneInsert.txt");
		} catch (Exception e) {
			e.printStackTrace();
			exception6 = e;
		}
		assertNull(exception6);

		// Expects no exceptions but empty output file
		Exception exception7 = null;
		SeminarDB db4 = new SeminarDB(8, 512);
		try {
			CommandParser cp7 = new CommandParser(db4, "jibberish.txt");
		} catch (Exception e) {
			e.printStackTrace();
			exception7 = e;
		}
		assertNull(exception7);
	}

	/**
	 * Tests inputOutput() and makes sure exceptions are met when expected
	 */
	public void testInputOutput() {
		SeminarDB db = new SeminarDB(256, 4);
		Exception exception1 = null;
		try {
			CommandParser cp = new CommandParser(db, "file.txt");
		} catch (Exception e) {
			e.printStackTrace();
			exception1 = e;
		}
		assertNotNull(exception1);
		assertTrue(exception1 instanceof FileNotFoundException);
		
		String nullStr = null;
		try {
			CommandParser cp = new CommandParser(db, nullStr);
		} catch (Exception e) {
			e.printStackTrace();
			exception1 = e;
		}
		assertNotNull(exception1);
		assertTrue(exception1 instanceof Exception);
	}

}
