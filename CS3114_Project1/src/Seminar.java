import java.io.*;

/**
 * Seminar class with serialization/deserialization support. You should use this
 * class in your project without making any changes to this code, because the
 * Web-CAT reference tests rely on your serialized Seminar objects being of
 * exactly the right length.
 *
 * @author CS3114/CS5040 staff
 * @version July 2023, updated August 2023
 */

public class Seminar implements Serializable {
    private String title; // Semianar title
    private String date; // Seminar date
    private int length; // Seminar length
    private String[] keywords; // Seminar keywords
    private short x; // Seminar x coord
    private short y; // Seminar y coord
    private String desc; // Seminar description
    private int cost; // Seminar cost
    private int id; // Seminar ID

    // ----------------------------------------------------------
    /**
     * Dummy seminar constructor
     */
    public Seminar() {
        // Nothing here
    }

    /**
     * Create a new Seminar object from the field data
     *
     * @param tin    input title
     * @param datein input date
     * @param lin    input length
     * @param kin    input keywords
     * @param xin    input x coord
     * @param yin    input y coord
     * @param descin input description
     * @param cin    input cost
     * @param idin   input ID
     */
    public Seminar(int idin, String tin, String datein, int lin, short xin,
        short yin, int cin, String[] kin, String descin) {
        id = idin;
        title = tin;
        date = datein;
        length = lin;
        x = xin;
        y = yin;
        cost = cin;
        keywords = kin;
        desc = descin;
    }

    // ----------------------------------------------------------

    /**
     * Return a Seminar object made by deserializing a byte array
     *
     * @param inputbytes A serialized Seminar object stored in a byte array
     * @return the deserialized Seminar
     * @throws Exception from byte stream
     */

    public static Seminar deserialize(byte[] inputbytes) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(inputbytes);
        try (ObjectInputStream inputStream = new ObjectInputStream(bis)) {
            int id = inputStream.readInt();
            String title = inputStream.readUTF();
            String date = inputStream.readUTF();
            int length = inputStream.readInt();
            short x = inputStream.readShort();
            short y = inputStream.readShort();
            int cost = inputStream.readInt();

            int numKeywords = inputStream.readInt();
            String[] keywords = new String[numKeywords];
            for (int i = 0; i < numKeywords; i++) {
                keywords[i] = inputStream.readUTF();
            }

            String desc = inputStream.readUTF();

            return new Seminar(id, title, date, length, x, y, cost, keywords,
                desc);
        }
    }

    // ----------------------------------------------------------
    /**
     * Return the cannonical serialized form (as a byte array) for this seminar
     * object
     *
     * @return the byte array that is the serialization of this
     * @throws Exception from serialization
     */

    public byte[] serialize() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(out)) {
            outputStream.writeInt(id);
            outputStream.writeUTF(title);
            outputStream.writeUTF(date);
            outputStream.writeInt(length);
            outputStream.writeShort(x);
            outputStream.writeShort(y);
            outputStream.writeInt(cost);

            // Write the number of keywords and then each keyword
            outputStream.writeInt(keywords.length);
            for (String keyword : keywords) {
                outputStream.writeUTF(keyword);
            }

            outputStream.writeUTF(desc);
        }
        return out.toByteArray();
    }

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        int i;
        String mykeys = "";
        for (i = 0; i < keywords.length; i++) {
            mykeys += keywords[i];
            if (i != keywords.length - 1)
                mykeys += ", ";
        }
        return "ID: " + id + ", Title: " + title + "\nDate: " + date +
            ", Length: " + length + ", X: " + x + ", Y: " + y + ", Cost: " +
            cost + "\nDescription: " + desc + "\nKeywords: " + mykeys;
    }
}
