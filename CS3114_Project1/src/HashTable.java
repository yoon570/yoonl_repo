/**
 * This class implements the HashTable data structure.
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

public class HashTable {
    // Fields include an array to store Records, size of table,
    // capacity of table (including tombstones), and number of records
    // (excluding tombstones)
    private Record[] values;
    private int size;
    private int capacity;
    private int recordNum;

    /**
     * Constructor for HashTable objects
     * 
     * @param size
     *            int size, given in power of 2
     */
    public HashTable(int size) {
        this.values = new Record[size];
        this.size = size;
        this.capacity = 0;
        this.recordNum = 0;
    }


    /**
     * Searches the HashTable for a specific id
     * 
     * @param id
     *            id being searched
     * @return Object if it is in the table, null if not
     */
    public Record search(int id) {
        // Checks that the idSet keys contains the id and id is not null
        int index = hash(id);
        if (index < 0) {
            // Returns null when Record is associated with a Tombstone
            if (values[(-1) * (index + 1)].getTomb()) {
                return null;
            }

            return values[(-1) * (index + 1)];
        }

        return null;
    }


    /**
     * Inserts an Object to a slot that corresponds to its hashed id
     * 
     * @param obj
     *            Object being added
     * @param id
     *            id of the object
     * @return true if it adds the Seminar to the table, false if not
     */
    public boolean insert(Record obj, int id) {
        // Checks that the id has not been inserted before and the inserted
        // object is not null
        if (obj == null) {
            return false;
        }
        // If capacity is at 50% of the size of the HashTable, the table
        // will double in size and rehash the key with the new size
        if (capacity == size / 2) {
            doubleAndRehash();
        }

        int index = hash(id);
        if (index > 0) {
            values[(index - 1)] = obj;
            capacity++;
            recordNum++;

            return true;
        }
        else {
            return false;
        }
    }


    /**
     * Deletes an Object from a slot that corresponds to the given id
     * when hashed
     * 
     * @param id
     *            slot being deleted
     * @return true if deletes the object from the slot, false if not
     */
    public boolean delete(int id) {
        // Checks to see if id has been inserted already and that id is not
        // null
        int index = hash(id);
        if (index < 0) {
            // Cannot delete a tombstone
            if (values[(-1) * (index + 1)].getTomb()) {
                return false;
            }
            else {
                // Delete a Handle slot by setting it as a tombstone
                values[(-1) * (index + 1)].setTomb();
                recordNum--;

                return true;
            }
        }

        return false;
    }


    /**
     * Prints out all the filled slots of the HashTable
     * 
     * @return string of all the things in
     */
    public String print() {
        String string = "";

        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                string += (i + ": ");

                if (values[i].getTomb()) {
                    string += "TOMBSTONE\n";
                }
                else {
                    string += (values[i].getID() + "\n");
                }
            }
        }

        return string;
    }


    /**
     * Returns the size of the hash table
     * 
     * @return this.size
     */
    public int getSize() {
        return this.size;
    }


    /**
     * Returns the capacity of the hash table
     * 
     * @return this.capacity
     */
    public int getCapacity() {
        return this.capacity;
    }


    /**
     * Returns the number of records in the hash table
     * 
     * @return this.recordNum
     */
    public int getRecordNum() {
        return this.recordNum;
    }


    /**
     * Utilizes linear probing with the 2 hash functions
     * h1: k mod M and
     * h2: (((k/M) mod (M/2)) * 2) + 1.
     * 
     * @param id
     *            number being hashed with the functions
     * @return hashed number
     */
    public int hash(int id) {
        // Creates hash values from id from both h1 & h2
        int h1 = id % size;
        int h2 = (((id / size) % (size / 2)) * 2) + 1;

        // Checks if first hash index is empty
        if (values[h1] == null) {
            return h1 + 1;
        }
        else {
            // If first hash index is full, checks if it shares id with given id
            if (values[h1].getID() == id) {
                return -(h1 + 1);
            }

            // If a collision is met at a position, the id will be continually
            // hashed with h2 until it reaches a open spot
            int newHash = (h1 + h2) % size;

            while (values[newHash] != null) {
                // Checks for filled indexes, whether they share id with given
                if (values[newHash].getID() == id) {
                    return -(newHash + 1);
                }

                newHash = (newHash + h2) % size;
            }

            return newHash + 1;
        }
    }


    /**
     * Doubles the size of HashTable and rehashes all the keys to match
     * the new size
     */
    private void doubleAndRehash() {
        // Creates larger array with double the size of the original
        Record[] valuesTemp = new Record[2 * values.length];
        size *= 2;

        for (int i = 0; i < values.length; i++) {
            valuesTemp[i] = values[i];
        }

        values = valuesTemp;

        // Rehashes the filled, non-tombstone slots into the larger array
        // using the same h1 and h2 hash function but with double the size
        Record[] valuesTemp2 = new Record[size];

        for (int i = 0; i < values.length / 2; i++) {
            // If an entry is a tombstone, that tombstone is erased and its
            // slot is made and considered empty.
            if (values[i] != null && values[i].getTomb()) {
                values[i] = null;
                this.capacity--;
            }

            // Similar rehashing as insert()
            if (values[i] != null) {
                int index = hash(values[i].getID());

                if (index < 0) {
                    index = -(index + 1);
                }
                else {
                    index--;
                }

                valuesTemp2[index] = values[i];
            }
        }

        // Setting the old to the new array
        values = valuesTemp2;
    }
}
