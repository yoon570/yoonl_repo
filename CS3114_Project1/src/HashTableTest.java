import student.TestCase;

/**
 * This class tests the HashTable functionalities.
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

public class HashTableTest extends TestCase {
    // testTable is the hashTable being tested on, which is filled
    // with records r1 - r5 which contain handles h1 - h5
    private HashTable testTable;
    private Handle h0;
    private Handle h1;
    private Handle h2;
    private Handle h3;
    private Handle h4;
    private Handle h5;
    private Record r0;
    private Record r1;
    private Record r2;
    private Record r3;
    private Record r4;
    private Record r5;

    /**
     * Sets up any basic foundations used for the HashTable testing
     */
    public void setUp() {
        h0 = new Handle(0, 1);
        h1 = new Handle(1, 2);
        h2 = new Handle(3, 4);
        h3 = new Handle(7, 8);
        h4 = new Handle(15, 16);
        h5 = new Handle(31, 32);
        r0 = new Record(0, h0);
        r1 = new Record(1, h1);
        r2 = new Record(2, h2);
        r3 = new Record(3, h3);
        r4 = new Record(4, h4);
        r5 = new Record(5, h5);

        testTable = new HashTable(4);
    }


    /**
     * Tests that search() works for unique ids, not given ids, and
     * nulls
     */
    public void testSearch() {
        testTable.insert(r1, 1);
        testTable.insert(r2, 2);

        // Searching unique ids
        assertEquals(testTable.search(1), r1);
        assertEquals(testTable.search(2), r2);

        // Searching not given ids
        assertNull(testTable.search(259));

        // Searching after doubling and rehashing
        testTable.insert(r3, 3);
        testTable.insert(r5, 5);
        assertEquals(testTable.search(3), r3);
        assertEquals(testTable.search(5), r5);

        // Searching a tombstone after delete yields null
        testTable.delete(1);
        assertNull(testTable.search(1));
    }


    /**
     * Tests that insert() works for unique ids and hashes, duplicate ids,
     * and duplicate hashes
     * 
     * @throws HashTableSizeException
     */
    public void testInsert() {
        // Inserting unique ids with non-null objects
        assertTrue(testTable.insert(r1, 1));
        assertTrue(testTable.insert(r2, 2));

        // Inserting null objects
        assertFalse(testTable.insert(null, 1));
        assertFalse(testTable.insert(null, 2));
        assertFalse(testTable.insert(null, 0));
        assertFalse(testTable.insert(null, -1));
        assertFalse(testTable.insert(null, 100));

        // Inserting duplicate ids
        assertFalse(testTable.insert(r1, 1));
        assertFalse(testTable.insert(r2, 2));

        // Inserting duplicate hashes which triggers doubling and rehashing
        assertTrue(testTable.insert(r3, 3));
        assertEquals(testTable.getSize(), 8);
        assertEquals(testTable.getCapacity(), 3);
        assertEquals(testTable.getRecordNum(), 3);
        assertEquals(testTable.search(3), r3);

        // Inserting works with 2nd hash function
        Record r6 = new Record(10, h1);
        Record r7 = new Record(11, h2);
        assertTrue(testTable.insert(r6, 10));
        assertEquals(testTable.search(10), r6);
        assertTrue(testTable.insert(r7, 11));

        // Inserting works with tombstones via 2nd hash function
        testTable.delete(3);
        Record r8 = new Record(19, h4);
        assertTrue(testTable.insert(r8, 19));
        assertEquals(testTable.getCapacity(), 6);
        assertEquals(testTable.getRecordNum(), 5);
    }


    /**
     * Tests that delete() works for unique ids, not given ids, and nulls
     */
    public void testDelete() {
        // Deleting an empty slot
        assertFalse(testTable.delete(256));

        // Deleting an non empty slot and replacing it with a tombstone
        testTable.insert(r1, 1);
        testTable.insert(r2, 2);
        assertTrue(testTable.delete(1));

        // Deleting a tombstone
        assertFalse(testTable.delete(1));
        assertNull(testTable.search(1));
        
        // Deleting after a doubling and rehashing
        testTable.insert(r3, 3);
        testTable.insert(r4, 4);
        assertTrue(testTable.delete(4));
        assertEquals(testTable.getCapacity(), 3);
        assertEquals(testTable.getRecordNum(), 2);
    }


    /**
     * Tests that print() works for empty, non-empty, and tombstone included
     * hash tables
     */
    public void testPrint() {
        // Printing out nothing
        assertEquals(testTable.print(), "");

        // Printing out an non-empty
        testTable.insert(r1, 1);
        testTable.insert(r2, 2);
        assertEquals(testTable.print(), "1: 1\n" + "2: 2\n");

        // Printing out an non-empty with tombstone
        testTable.delete(1);
        assertEquals(testTable.print(), "1: TOMBSTONE\n" + "2: 2\n");

        // Printing out an non-empty with tombstone after doubling and rehashing
        testTable.insert(r3, 3);
        testTable.insert(r4, 4);
        assertEquals(testTable.print(), "2: 2\n" + "3: 3\n" + "4: 4\n");
        assertEquals(testTable.getCapacity(), 3);
        assertEquals(testTable.getRecordNum(), 3);

    }


    /**
     * Tests that the hash function works
     */
    public void testHash() {
        // Returns positive hashed number for empty table
        assertEquals(testTable.hash(0), 1);
        assertEquals(testTable.hash(1), 2);
        assertEquals(testTable.hash(2), 3);
        assertEquals(testTable.hash(3), 4);
        assertEquals(testTable.hash(4), 1);
        
        // hash() returns negative number if table has id already
        testTable.insert(r0, 0);
        testTable.insert(r1, 1);
        assertEquals(testTable.hash(0), -1);
        assertEquals(testTable.hash(1), -2);
        
        // hash() returns positive number for open space
        assertEquals(testTable.hash(2), 3);
        assertEquals(testTable.hash(4), 4);
        
        // Collision testing
        testTable.insert(r4, 4);
        testTable.insert(new Record(8, new Handle(1, 1)), 8);
        assertEquals(testTable.getSize(), 8);
        assertEquals(testTable.getCapacity(), 4);
        testTable.insert(new Record(16, new Handle(2, 2)), 16);
        assertEquals(testTable.getSize(), 16);
        assertEquals(testTable.hash(16), -4);
        assertEquals(testTable.hash(32), 6);
        assertEquals(testTable.hash(17), 8);
    }
    
    /**
     * Tests tombstones 
     */
    public void testTombstones()
    {
        testTable.insert(r0, 0);
        testTable.delete(0);
        testTable.insert(r4, 4);
        assertEquals(testTable.print(), "0: TOMBSTONE\n3: 4\n");
        
        testTable.insert(r1, 1);
        testTable.delete(1);
        testTable.delete(4);
        assertEquals(testTable.print(), "1: TOMBSTONE\n4: TOMBSTONE\n");
    }
}
