import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This project introduces a scheduling system that manages training sessions.
 * This system involves the usage of a hash table to store and maintain data
 * along with a memory manager that controls the memory associated with each
 * training session.
 */

/**
 * The class containing the main method.
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

public class SemManager {
    /**
     * @param args
     *            Command line parameters
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // Foundational fields for HashTable, MemoryManager, and input file
        // memSize is inital size of MemManager, hashSize is initial size
        // of the HashTable, and fileName is the name of input file
        int memSize;
        int hashSize;
        String fileName;

        // If args is null, throw IOException
        if (args == null) {
            throw new IOException("Error: Argument is null");
        }

        // Command line argument has to follow the convention with length 3:
        // java SemManager [memSize] [hashSize] [inputFileName]
        if (args.length == 3) {
            memSize = Integer.parseInt(args[0]);
            hashSize = Integer.parseInt(args[1]);
            fileName = args[2];

            // throws FileNotFoundException if fileName is null
            if (fileName == null) {
                throw new FileNotFoundException("Error: Missing input file");
            }

            // throws IOException if hashSize is not a power of 2
            if (!isPowerOf2(hashSize)) {
                throw new IOException(
                    "Error: Hash table size is not a power of 2 "
                        + "greater than 0");
            }
            
            // throws IOException if memSize is not a power of 2
            if (!isPowerOf2(memSize)) {
                throw new IOException("Error: Memory size is not a power of 2 "
                    + "greater than 0");
            }

            // Creates the CommandParser for reading the input file
            // Creates the SeminarDB for managing the HashTable and MemManager
            SeminarDB db = new SeminarDB(hashSize, memSize);
            CommandParser cp = new CommandParser(db, fileName);
        }
        else {
            throw new IOException("Error: Insert THREE arguments");
        }
    }

    /**
     * Checks if the given int is a power of 2
     * 
     * @param num
     *            int being check if it's a power of 2
     * @return true if so, false if not
     */
    private static boolean isPowerOf2(int num) {
        // 0 is the only power of 2 that is not allowed
        if (num == 0) {
            return false;
        }

        // Iterates through the number is see that it continues to be power of 2
        while (num != 1) {
            if (num % 2 != 0) {
                return false;
            }

            num /= 2;
        }

        return true;
    }
}
