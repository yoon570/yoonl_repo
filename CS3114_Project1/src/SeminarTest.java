import student.TestCase;

// -------------------------------------------------------------------------
/**
 *  Test the Seminar class
 *
 *  @author CS3114/CS5040 staff
 *  @version July 2023, updated August 2023
 */
public class SeminarTest extends TestCase {
    /**
     * Sets up the tests that follow. In general, used for initialization
     */
    public void setUp() {
        // Nothing Here
    }


    /**
     * Check the toString method
     */
    public void testtoString()
    {
        String[] keywords = {"Good", "Bad", "Ugly"};
        String expected = "ID: 1729, Title: Seminar Title\n"
            + "Date: 2405231000, Length: 75, X: 15, Y: 33, Cost: 125\n"
            + "Description: This is a great seminar\n"
            + "Keywords: Good, Bad, Ugly";
        Seminar mysem = new Seminar(1729, "Seminar Title", "2405231000", 75,
            (short)15, (short)33, 125, keywords, "This is a great seminar");
        String semPrint = mysem.toString();
        System.out.println("testtoString");
        System.out.println(semPrint);
        assertTrue(semPrint.equals(expected));
    }


    /**
     * Check the serialization/deserialization process
     * @throws Exception
     */
    public void testSeminarDS()
        throws Exception
    {
        System.out.println("testSeminarDS");
        byte[] bytes;
        String[] keywords = {"Good", "Bad", "Ugly"};

        Seminar mysem = new Seminar(1729, "Seminar Title", "2405231000", 75,
            (short)15, (short)33, 125, keywords, "This is a great seminar");
        String semPrint = mysem.toString();
        bytes = mysem.serialize();
        Seminar mysem2 = Seminar.deserialize(bytes);
        System.out.println("Number of bytes in serialized object is: " +
            bytes.length);
        assertTrue(bytes.length == 95); // 274
        String sem2Print = mysem2.toString();
        System.out.println(semPrint);
        System.out.println(sem2Print);
        assertTrue(semPrint.equals(sem2Print));
    }
}
