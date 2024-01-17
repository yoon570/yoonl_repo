import java.io.*;
import java.util.*;

/**
 * Generate a test data file. The size is a multiple of 4096 bytes. Depending on
 * the options, you can generate two types of output. With option "-a", the
 * output will be set so that when interpreted as ASCII characters, it will look
 * like a series of: [space][letter][space][space]. With option "-b", the
 * records are short ints, with each record having a value less than 30,000.
 *
 * @author Modified from original command-line version by Cliff Shaffer
 * @version Sep 28, 2014
 */
public class FileGenerator {
    private static final int NUM_RECS = 2048; // Because they are short

    /** Initialize the random variable */
    static private Random value = new Random(); // Hold the Random class

    /**
     * This function generates a random number.
     *
     * @param n
     *            the ceiling
     * @return a random number
     */
    public int random(int n) {
        return Math.abs(value.nextInt()) % n;
    }


    /**
     * This method generates a file.
     *
     * @param args
     *            an array of arguments
     * @throws IOException
     *             an exception
     */
    public void generateFile(String[] args) throws IOException {
        short val;
        int filesize = Integer.parseInt(args[2]); // Size of file in blocks
        DataOutputStream file = new DataOutputStream(new BufferedOutputStream(
            new FileOutputStream(args[1])));

        if (args[0].charAt(1) == 'b') { // Write out random numbers
            for (int i = 0; i < filesize; i++) {
                for (int j = 0; j < NUM_RECS; j++) {
                    val = (short)(random(29999) + 1);
                    file.writeShort(val);
                }
            }
        }
        else if (args[0].charAt(1) == 'a') { // Write out ASCII-readable values
            for (int i = 0; i < filesize; i++) {
                for (int j = 0; j < NUM_RECS; j++) {
                    if ((j % 2) == 1) {
                        val = (short)(8224);
                    }
                    else {
                        val = (short)(random(26) + 0x2041);
                    }
                    file.writeShort(val);
                }
            }
        }
        file.flush();
        file.close();
    }
}
