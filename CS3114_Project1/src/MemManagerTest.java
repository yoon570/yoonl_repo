import student.TestCase;

/**
 * This class tests the MemManager's functionalities.
 * 
 * @author Danny Yang (dannyy8)
 * @author Yoon Lee (yoonl18)
 * @version September 11, 2023
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

public class MemManagerTest extends TestCase {
    // Old variables, needs fixing.
    private MemManager memMan;
    private Seminar mysem;
    private byte[] serial;
    private Handle handle;

    // New variable list
    private Seminar size64;
    private Seminar size128;
    private Seminar size256;

    /**
     * Sets up the tests that follow. In general, used for initialization
     */
    public void setUp() {
        memMan = new MemManager(512);
        String[] keywords = { "Good", "Bad", "Ugly" };
        mysem = new Seminar(1729, "Seminar Title", "2405231000", 75, (short)15,
            (short)33, 125, keywords, "T");

        String[] keywords64 = { "Good" };
        int givenSize = 0;
        String description = "a";
        givenSize = 64;

        size64 = new Seminar(1, "Seminar of size", "2405231000", 75, (short)15,
            (short)33, 125, keywords64, description);
        for (int i = 0; i < givenSize; i++) {
            description = description + "a";
        }
        size128 = new Seminar(1, "Seminar of size", "2405231000", 75, (short)15,
            (short)33, 125, keywords64, description);
        for (int i = 0; i < givenSize; i++) {
            description = description + "a";
        }
        for (int i = 0; i < givenSize; i++) {
            description = description + "a";
        }
        size256 = new Seminar(1, "Seminar of size", "2405231000", 75, (short)15,
            (short)33, 125, keywords64, description);
        for (int i = 0; i < givenSize; i++) {
            description = description + "a";
        }

    }


    /**
     * Tests that insert() works for all cases
     * 
     * @throws Exception
     */
    public void testInsert() throws Exception {

        assertEquals(size256.serialize().length, 256);
        // Insert and fetch handle
        handle = memMan.insert(size256.serialize(), size256.serialize().length);
        assertEquals(handle.getStart(), 0);

        handle = memMan.insert(size128.serialize(), size128.serialize().length);
        assertEquals(handle.getStart(), 256);

        handle = memMan.insert(size64.serialize(), size64.serialize().length);
        assertEquals(handle.getStart(), 384);

        handle = memMan.insert(size256.serialize(), size256.serialize().length);
        assertNull(handle);

    }


    /**
     * Tests that pullRecord() pulls the record from the pool
     * 
     * @throws Exception
     */
    public void testPullRecord() throws Exception {
        serial = mysem.serialize();
        // Insert and fetch handle
        handle = memMan.insert(serial, serial.length);

        // Pull record
        byte[] test = new byte[handle.getSize()];
        memMan.get(test, handle, handle.getSize());

        // Check if each element of the byte array is equal to the serialized
        // seminar
        for (int i = 0; i < test.length; i++) {
            assertEquals(test[i], serial[i]);
        }
    }


    /**
     * Tests that length() is equal to the actual length of the record
     */
    public void testLength() throws Exception {
        // Serialize seminar
        serial = mysem.serialize();
        // Insert and fetch handle
        handle = memMan.insert(serial, serial.length);

        // Check that the handle is of the same size as the record
        assertEquals(serial.length, memMan.length(handle));
    }


    /**
     * Tests that dump() prints out all the free blocks in the FBL
     * 
     * @throws Exception
     */
    public void testDump() throws Exception {
        // Serialize seminar
        handle = memMan.insert(size64.serialize(), size64.serialize().length);
        // Insert and fetch handle
        
        // Check if dump is correctly printing free blocks
        assertEquals(memMan.dump(), "64: 64\n128: 128\n256: 256\n");
        
        memMan.insert(size64.serialize(), size64.serialize().length);
        assertEquals(memMan.dump(), "128: 128\n256: 256\n");
        
        memMan.insert(size128.serialize(), size128.serialize().length);
        assertEquals(memMan.dump(), "256: 256\n");
        
        memMan.insert(size256.serialize(), size256.serialize().length);
        assertEquals(memMan.dump(), "There are no freeblocks in the memory pool\n");
    }


    /**
	 * Tests removes() works as expected
	 * 
	 * @throws Exception
	 */
	public void testRemove() throws Exception {
	    // Test null removal
	    MemManager smallMan = new MemManager(256);
	    smallMan.remove(null);
	
	    // Set up byte array for stored records.
	    handle = smallMan.insert(size256.serialize(), size256
	        .serialize().length);
	    byte[] storedRecord = new byte[handle.getSize()];
	    smallMan.get(storedRecord, handle, handle.getSize());
	
	    for (int i = 0; i < storedRecord.length; i++) {
	        assertEquals(storedRecord[i], size256.serialize()[i]);
	    }
	
	    smallMan.remove(handle);
	    storedRecord = new byte[handle.getSize()];
	    smallMan.get(storedRecord, handle, handle.getSize());
	
	    for (int i = 0; i < storedRecord.length; i++) {
	        assertEquals(storedRecord[i], 0);
	    }
	    
	    Handle bogusHandle = new Handle(128, 128);
	    smallMan.remove(bogusHandle);
	
	    Handle handle1 = smallMan.insert(size64.serialize(), size64
	        .serialize().length);
	    byte[] storedRecord1 = new byte[handle1.getSize()];
	    smallMan.get(storedRecord1, handle1, handle1.getSize());
	
	    Handle handle2 = smallMan.insert(size64.serialize(), size64
	        .serialize().length);
	    byte[] storedRecord2 = new byte[handle2.getSize()];
	    smallMan.get(storedRecord2, handle2, handle2.getSize());
	
	    smallMan.remove(handle2);
	    smallMan.get(storedRecord2, handle2, handle2.getSize());
	    for (int i = 0; i < storedRecord2.length; i++) {
	        assertEquals(storedRecord2[i], 0);
	    }
	
	    smallMan.remove(handle1);
	    smallMan.get(storedRecord1, handle1, handle1.getSize());
	    for (int i = 0; i < storedRecord1.length; i++) {
	        assertEquals(storedRecord1[i], 0);
	    }
	    
	    smallMan.insert(size256.serialize(), size256.serialize().length);
	    assertEquals(smallMan.get(size256.serialize(), new Handle(0,256), 256), 256);
	    
	    smallMan.remove(new Handle(0, 256));
	    
	    
	    // Cleanup mutation testing begins here
	    System.out.print("CLEANUPTEST LINE 222print\n");
	    MemManager cleanTest = new MemManager(256);
	    
	    // Simple insert of two records and removal of two records
	    Handle cleanH1 = cleanTest.insert(size64.serialize(), size64.serialize().length);
	    Handle cleanH2 = cleanTest.insert(size64.serialize(), size64.serialize().length);
	    
	    System.out.println("NEWPRINT");
	    System.out.print(cleanTest.dump()); //PRINT
	    cleanTest.print();
	    
	    cleanTest.remove(cleanH1);
	    cleanTest.remove(cleanH2);
	    
	    System.out.println("NEWPRINT");
	    System.out.print(cleanTest.dump()); //PRINT
	    cleanTest.print();
	    
	    cleanH1 = cleanTest.insert(size64.serialize(), size64.serialize().length);
	    cleanH2 = cleanTest.insert(size64.serialize(), size64.serialize().length);
	    Handle cleanH3 = cleanTest.insert(size64.serialize(), size64.serialize().length);
	    
	    System.out.println("NEWPRINT");
	    System.out.print(cleanTest.dump()); //PRINT
	    cleanTest.print();
	    
	    cleanTest.remove(cleanH1);
	    
	    System.out.println("NEWPRINT");
	    System.out.print(cleanTest.dump()); //PRINT
	    cleanTest.print();
	    
	    cleanTest.remove(cleanH3);
	
	    System.out.println("NEWPRINT");
	    System.out.print(cleanTest.dump()); //PRINT
	    cleanTest.print();
	    
	    cleanTest.remove(cleanH2);
	    
	    System.out.println("NEWPRINT");
	    System.out.print(cleanTest.dump()); //PRINT
	    cleanTest.print();
	}


	/**
     * Tests that doubleSize() tries to double the pool when appropriate
     * 
     * @throws Exception
     */
    /**
     * Tests that doubleSize() tries to double the pool when appropriate
     * 
     * @throws Exception
     */
    public void testDoubleSize() throws Exception {
        // Serialize seminar
        // Insert and fetch handle
    	
    	MemManager smallMan = new MemManager(256);
    	smallMan.doubleSize(256);
    	assertEquals(smallMan.getSize(), 256);
        handle = smallMan.insert(size256.serialize(), size256.serialize().length);
        smallMan.doubleSize(256);
        assertEquals(smallMan.getSize(), 512);
        smallMan.remove(handle);


        // doubleSize() doubles the memSize to match it when
        // length > memSize and then doubles it to fit if necessary
        MemManager memMan2 = new MemManager(4);
        memMan2.doubleSize(137);
        assertEquals(memMan2.getSize(), 256);
    }


    /**
     * Tests that getSize() returns the expected size
     * 
     * @throws Exception
     */
    public void testGetSize() throws Exception {
        // getSize() returns initial size
        assertEquals(memMan.getSize(), 512);

        // getSize() returns double the initial size when pool is doubled
        // Something here
        serial = mysem.serialize();
        handle = memMan.insert(serial, serial.length);
    }
}
