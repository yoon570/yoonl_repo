import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * This class implements the actual Quicksort algorithm to achieve
 * a sorted input file.
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

public class QuickSorting {
    // BufferPool used for sorting
    private BufferPool bufferPool;

    /**
     * Constructor that creates a QuickSorting object
     * 
     * @param file
     *            raf being used
     * @param numBuff
     *            number of buffers in pool
     * @param len
     *            length of file
     * @throws IOException
     */
    public QuickSorting(RandomAccessFile file, int numBuff, int len)
        throws IOException {
        this.bufferPool = new BufferPool(file, numBuff);
        quicksort(this.bufferPool, 0, len);
        this.bufferPool.flush();
    }


    /**
     * Function that does the Quicksorting method
     * 
     * @param arr
     *            array being Quicksorted
     * @param left
     *            left index
     * @param right
     *            right index
     * @throws IOException
     */
    private static void quicksort(BufferPool buff, int left, int right)
        throws IOException {
        if (right <= left) {
            return;
        }

        // Swapping occurring in left and right sub-arrays
        int pivotIndex = findPivot(buff, left, right);

        byte[] pivotRecord = getRecord(buff, pivotIndex);
        short pivotVal = getKey(pivotRecord);

        swap(buff, pivotIndex, right);

        int firstSub = partition(buff, left, right - 1, pivotVal);
        swap(buff, firstSub, right);

        // TODO duplicate checking, checking between indeces if they're identical
        
        // between firstsub and right, then return
        
        if (left == firstSub && checkDupe(buff, left + 1, right)) {
            return;
        }
        
        if ((firstSub - left) > 1) {
            quicksort(buff, left, firstSub - 1);
        }
        if ((right - firstSub) > 1) {
            quicksort(buff, firstSub + 1, right);
        }
    }


    /**
     * Finds the pivot needed for the Quicksort
     * 
     * @param buff
     *            array being Quicksorted
     * @param left
     *            left index
     * @param right
     *            right index
     * @return Pivot index
     */
    private static int findPivot(BufferPool buff, int left, int right) {
        return (left + right) / 2;
    }


    /**
     * Implements swapping between two indices
     * 
     * @param arr
     *            array being Quicksorted
     * @param a
     *            index of one element to be swapped
     * @param b
     *            index of another element to be swapped
     * @throws IOException
     */
    private static void swap(BufferPool buff, int a, int b) throws IOException {

        // this works with BufferPool, swap using insert and get

        byte[] recordA = new byte[4];
        byte[] recordB = new byte[4];

        buff.getbytes(recordA, BufferPool.RECSIZE, a);
        buff.getbytes(recordB, BufferPool.RECSIZE, b);

        buff.insert(recordA, BufferPool.RECSIZE, b);
        buff.insert(recordB, BufferPool.RECSIZE, a);
    }


    /**
     * Copies elements less than pivot to the low end of the array
     * and elements greater than pivot to the high end of the array
     * 
     * @param buff
     *            BufferPool being Quicksorted
     * @param left
     *            left index
     * @param right
     *            right index
     * @param pivot
     *            pivot value
     * @return first position in right partition
     * @throws IOException
     */
    private static int partition(
        BufferPool buff,
        int left,
        int right,
        short pivot)
        throws IOException {
        while (left <= right) {
            while (getKey(getRecord(buff, left)) < pivot) {
                left++;
            }

            while (right >= left && getKey(getRecord(buff, right)) >= pivot) {
                right--;
            }

            if (right > left) {
                swap(buff, left, right);
            }
        }

        return left;
    }


    /**
     * Gets the key from the disk using a given keyValue
     * 
     * @param keyValue
     *            target "buffer" with target key
     * @return key as a short
     */
    private static short getKey(byte[] keyValue) {

        short outShort = 0;

        outShort = ByteBuffer.wrap(keyValue, 0, 2).getShort();

        return outShort;
    }


    /**
     * Gets the record from the bufferpool with a given index
     * 
     * @param buff
     *            buff being used to find the record
     * @param index
     *            record has this specified target
     * @return target record associated with the index
     * @throws IOException
     */
    private static byte[] getRecord(BufferPool buff, int index)
        throws IOException {

        byte[] record = new byte[4];
        buff.getbytes(record, 4, index);

        return record;

    }
    
    private static boolean checkDupe(BufferPool buff, int start, int end) throws IOException {
        
        byte[] recA = new byte[4];
        byte[] recB = new byte[4];
        short shortA = getKey(recA);
        
        buff.getbytes(recA, BufferPool.RECSIZE, start);
        
        for (int i = start; i <= end; i++) {
            buff.getbytes(recB, BufferPool.RECSIZE, i);
            short shortB = getKey(recB);
            
            if (shortA != shortB) {
                return false;
            }
            
        }
        return true;
    }
}
