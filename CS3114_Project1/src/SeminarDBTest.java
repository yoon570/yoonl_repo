import student.TestCase;

/**
 * Tests all the methods and exceptions from SeminarDB
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
public class SeminarDBTest extends TestCase {
    // Test object and String array
    private SeminarDB test;
    private String[] arr;

    /**
     * Sets up the tests that follow. In general, used for initialization
     */
    public void setUp() {
        test = new SeminarDB(4, 512);
        arr = new String[] { "The", "Horse", "In", "Motion" };
    }


    /**
     * Tests that search() functions correctly with both HashTable
     * and MemoryManager
     * 
     * @throws Exception
     */
    public void testSearch() throws Exception {
        // Returns null if HashTable and MemoryManager are empty
        assertNull(test.search(1));

        // Returns a Seminar.toString() for Seminar stored by both structures
        test.insert(1, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        test.insert(2, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");

        Seminar sem1 = new Seminar(1, "The", "Horse", 10, (short)1, (short)1,
            10, arr, "Movies are magic");
        assertEquals(test.search(1), sem1.toString());

        // Returns null when the id is not in the HashTable for unempty table
        // assertNull(test.search(3));
    }


    /**
     * Tests that insert() functions correctly with both structures
     * 
     * @throws Exception
     */
    public void testInsert() throws Exception {
        // Returns empty string "" when inserting to empty structures
        assertEquals(test.insert(1, "Overview of HCI Research at VT",
            "0610051600", 90, (short)10, (short)10, 45, new String[] { "HCI",
                "Computer_Science", "VT", "Virginia_Tech" },
            "Movies are magic"), "");
        assertEquals(test.insert(2,
            "Computational Biology and Bioinformatics in CS at"
                + " Virginia Tech", "0610071600", 60, (short)20, (short)10, 30,
            new String[] { "Bioinformatics, computation_biology", "Biology",
                "Computer_Science", "VT", "Virginia_Tech" },
            "Introduction to   bioinformatics and " + "computation biology"),
            "");

        // Returns null when inserting with same id
        assertNull(test.insert(1, "My", "Horse", 100, (short)1, (short)1, 100,
            arr, "Movies are magic"));

        // Returns doubled statements when structures are doubled
        assertEquals(test.insert(3,
            "Computational Biology and Bioinformatics in CS at"
                + " Virginia Tech", "0610071600", 60, (short)20, (short)10, 30,
            new String[] { "Bioinformatics, computation_biology", "Biology",
                "Computer_Science", "VT", "Virginia_Tech" },
            "Introduction to   bioinformatics and " + "computation biology"),
            "Memory pool expanded to 1024 bytes\n"
                + "Hash table expanded to 8 records\n");
    }


    /**
     * Tests that delete() functions correctly with both structures
     * 
     * @throws Exception
     */
    public void testDelete() throws Exception {
        // Returns false when deleting from empty structures
        assertFalse(test.delete(1));

        // Returns true when deleting from the structures with the given id
        test.insert(1, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        test.insert(2, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        assertTrue(test.delete(1));
    }


    /**
     * Tests that printHash() prints out all from the HashTable
     * 
     * @throws Exception
     */
    public void testPrintHash() throws Exception {
        // Returns empty string for empty HashTable
        assertEquals(test.printHash(), "");

        // Returns 2 slots when 2 slots are occupied
        test.insert(1, "Overview of HCI Research at VT", "0610051600", 90,
            (short)10, (short)10, 45, new String[] { "HCI", "Computer_Science",
                "VT", "Virginia_Tech" }, "Movies are magic");
        test.insert(2, "Computational Biology and Bioinformatics in CS at"
            + " Virginia Tech", "0610071600", 60, (short)20, (short)10, 30,
            new String[] { "Bioinformatics, computation_biology", "Biology",
                "Computer_Science", "VT", "Virginia_Tech" },
            "Introduction to   bioinformatics and " + "computation biology");
        assertEquals(test.printHash(), "1: 1\n2: 2\n");

        // Returns 2 slots when one of the slots is a tombstone
        test.delete(2);
        assertEquals(test.printHash(), "1: 1\n2: TOMBSTONE\n");

        // Returns a larger list with tombstone gone when doubled
        // something here
        test.insert(3, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        test.insert(10, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        assertEquals(test.printHash(), "1: 1\n2: 10\n3: 3\n");
    }


    /**
     * Tests that printFB() prints all from the FreeBlockList
     * 
     * @throws Exception
     */
    public void testPrintFB() throws Exception {
        // Return one block for empty FreeBlockList
        assertEquals(test.printFB(), "512: 0\n");

        // Returns nonempty string when only insertions have occurred
        test.insert(1, "Overview of HCI Research at VT", "0610051600", 90,
            (short)10, (short)10, 45, new String[] { "HCI", "Computer_Science",
                "VT", "Virginia_Tech" }, "Movies are magic");
        test.insert(2, "Computational Biology and Bioinformatics in CS at"
            + " Virginia Tech", "0610071600", 60, (short)20, (short)10, 30,
            new String[] { "Bioinformatics, computation_biology", "Biology",
                "Computer_Science", "VT", "Virginia_Tech" },
            "Introduction to   bioinformatics and " + "computation biology");
        /*
         * assertEquals(test.printFB(), "");
         * 
         * // Returns a non-empty string when a deletion occurred
         * test.delete(2);
         * assertEquals(test.printFB(), "256: 256");
         */
    }


    /**
     * Tests that getSize() gets the correct size of the record
     * 
     * @throws Exception
     */
    public void testGetSize() throws Exception {
        test.insert(1, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        assertEquals(test.getSize(1), 80);

        test.insert(2, "My", "Horses", 1000, (short)1, (short)1, 10, arr,
            "Movies are magic");
        assertEquals(test.getSize(2), 80);
    }

    /**
     * Tests that getHashRecNum() gets the correct number of records
     * 
     * @throws Exception 
     */
    public void testGetHashRecNum() throws Exception
    {
        //Expects 0 when there are no insertions
        assertEquals(test.getHashRecNum(), 0);
        
        //Expects >0 when there are insertions
        test.insert(1, "The", "Horse", 10, (short)1, (short)1, 10, arr,
            "Movies are magic");
        test.insert(2, "My", "Horses", 1000, (short)1, (short)1, 10, arr,
            "Movies are magic");
        assertEquals(test.getHashRecNum(), 2);
        
        //Expects number to decrease when record is deleted
        test.delete(1);
        assertEquals(test.getHashRecNum(), 1);
    }
}
