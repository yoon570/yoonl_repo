
/**
 * This project utilizes the Quicksort algorithm to sort a binary file into
 * ascending file. This process uses a BufferPool that acts as the main memory
 * area used for indexing and caching data.
 */

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The class containing the main method. This class implements the Quicksort
 * sorting method to sort byte arrays.
 *
 * @author Yoon Lee (yoonl18)
 * @author Danny Yang (dannyy8)
 * @version 10.23.23
 */

// On my honor:
//
// - I have not used source code obtained from another student,
// or any other unauthorized source, either modified or
// unmodified.
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

public class Quicksort {

    /**
     * This method is used to generate a file of a certain size, containing a
     * specified number of records.
     *
     * @param filename
     *            the name of the file to create/write to
     * @param blockSize
     *            the size of the file to generate
     * @param format
     *            the format of file to create
     * @throws IOException
     *             throw if the file is not open and proper
     */
    public static void generateFile(
        String filename,
        String blockSize,
        char format)
        throws IOException {
        FileGenerator generator = new FileGenerator();
        String[] inputs = new String[3];
        inputs[0] = "-" + format;
        inputs[1] = filename;
        inputs[2] = blockSize;
        generator.generateFile(inputs);
    }


    /**
     * Main method for QuickSort
     * 
     * @param args
     *            Command line parameters.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args == null) {
            throw new IOException();
        }
        if (args != null && args.length == 3) {
            long start = System.currentTimeMillis();
            // Create Random Access File object for argument file
            RandomAccessFile raf = new RandomAccessFile(args[0], "rw");

            QuickSorting qs = new QuickSorting(raf, Integer.parseInt(args[1]),
                ((int)raf.length() / BufferPool.RECSIZE) - 1);
            raf.close();

            long end = System.currentTimeMillis() - start;
            //TODO remove print for time
            System.out.println(end + " TIMETOSORT");
            FileWriter output = new FileWriter(args[2], true);
            output.write("Name of file: " + args[2] + "\n");
            output.write("Quicksort Time: " + end + "\n\n");

            output.close();
        }
        else {
            throw new IOException("Error: args is wrong");
        }
    }
}
