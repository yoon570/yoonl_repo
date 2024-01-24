/**
 * This class manages the data read from the CommandParser with the
 * utilization of the HashTable and MemManager.
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

public class SeminarDB {
    // HashTable field for storage of keys and records
    // MemoryManager field for memory storage
    private HashTable hashTable;
    private MemManager memManager;

    /**
     * Constructor (Creates a SeminarDB)
     * 
     * @param hashSize
     *            size of the HashTable
     * @param memSize
     *            size of the MemoryManager
     */
    public SeminarDB(int hashSize, int memSize) {
        hashTable = new HashTable(hashSize);
        memManager = new MemManager(memSize);
    }


    /**
     * Executes search() for HashTable to get a corresponding Handle
     * which is used to find a Seminar object from the MemManager
     * 
     * @param id
     *            id being searched for in the HashTable
     * @return String of Seminar if found, else nothing
     * @throws Exception
     */
    public String search(int id) throws Exception {
        Record record = hashTable.search(id);

        // If record is null, then it is not found in HashTable
        if (record != null) {
            // Uses Handle and to get its corresponding byte array for Seminar
            byte[] seminarr = new byte[record.getHandle().getSize()];
            memManager.get(seminarr, record.getHandle(), record
                .getHandle().getSize());

            // Returns the string of the Seminar deserialized from byte[]
            Seminar sem = Seminar.deserialize(seminarr);

            return (sem.toString());
        }

        return null;
    }


    /**
     * Executes insert() for HashTable and MemManager()
     * 
     * @param title
     *            input title
     * @param date
     *            input date
     * @param length
     *            input length
     * @param keywords
     *            input keywords
     * @param x
     *            input x coord
     * @param y
     *            input y coord
     * @param desc
     *            input description
     * @param cost
     *            input cost
     * @param id
     *            input ID
     * @return null if insert fails, String if not
     * @throws Exception
     */
    public String insert(
        int id,
        String title,
        String date,
        int length,
        short x,
        short y,
        int cost,
        String[] keywords,
        String desc)
        throws Exception {
        String string = "";
        // Checks to see if id is unique in order for insertion to work
        if (hashTable.search(id) == null) {
            // Creates a new Seminar object to be added to MemManager
            Seminar sem = new Seminar(id, title, date, length, x, y, cost,
                keywords, desc);

            // Checks to see if the memManager needs to double its size
            // If it doubles, a String will be added that states its doubling
            String sizeUpdate = "";
            if (memManager.doubleSize(sem.serialize().length)) {
                sizeUpdate += "Memory pool expanded to " + memManager.getSize()
                    + " bytes\n";
            }

            // Gets the Handle created by Seminar's insertion into MemManager
            Handle idHandle = memManager.insert(sem.serialize(), sem
                .serialize().length);

            // Checks to see if the Hash Table needs to double its size
            // If it doubles, a String will be added that states its doubling
            if (hashTable.getCapacity() == hashTable.getSize() / 2) {
                sizeUpdate += "Hash table expanded to " + hashTable.getSize()
                    * 2 + " records\n";
            }

            // Inserts Record into HashTable
            Record record = new Record(id, idHandle);
            hashTable.insert(record, id);

            return (string + sizeUpdate);
        }

        return null;
    }


    /**
     * Executes delete() for HashTable and remove() for MemManager
     * 
     * @param id
     *            id of the Seminar being deleted
     * @return true if deleted, false if not
     */
    public boolean delete(int id) {
        Record record = hashTable.search(id);

        // If Handle is not null, delete commences and remains true
        if (record != null) {
            hashTable.delete(id);
            memManager.remove(record.getHandle());
            return true;
        }

        return false;
    }


    /**
     * Executes print() from HashTable
     * 
     * @return String created by print() of HashTable
     */
    public String printHash() {
        return hashTable.print();
    }


    /**
     * Executes dump() from MemManager
     * 
     * @return String created by dump() of MemManager
     */
    public String printFB() {
        return memManager.dump();
    }


    /**
     * Finds the size of the Seminar object from its bytes through
     * looking it up with its id
     * 
     * @param id
     *            id of the target Seminar object
     * @return int byte size of the Seminar object
     */
    public int getSize(int id) {
        // Gets the Handle from HashTable by its id
        Handle idHandle = hashTable.search(id).getHandle();

        // Returns the length of the Handle which is the size of Seminar
        return memManager.length(idHandle);
    }


    /**
     * Getter method for the Hash Table recordNum
     * 
     * @return hashTable.getSize()
     */
    public int getHashRecNum() {
        return this.hashTable.getRecordNum();
    }
}
