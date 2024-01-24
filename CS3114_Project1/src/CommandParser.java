import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * This class reads and parses the information set in the provided input files
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

public class CommandParser {
	// SeminarDB object that manages the objects read by CommandParser
	private SeminarDB db;

	/**
	 * Constructor (Creates a CommandParser object)
	 * 
	 * @param db       SeminarDB object that manages the objects read from fileName
	 * @param fileName file being read
	 * @throws Exception
	 */
	public CommandParser(SeminarDB db, String fileName) throws Exception {
		// throws Exception if file is null or unreadable
		if (fileName == null) { //
			throw new Exception("File is null or unreadable");
		} else {
			this.db = db;
			this.inputOutput(fileName);
		}
	}

	/**
	 * Reads through the file, executes the commands, and sets up the objects needed
	 * to fill HashTable and MemoryManager
	 * 
	 * @param fileName file being read
	 * @throws Exception
	 */
	private void inputOutput(String fileName) throws Exception {
		File file = new File(fileName);

		if (file.exists()) { //
			FileWriter output = new FileWriter("output.txt");

			// File is continues to be read as long as their are
			// still things to be read
			Scanner input = new Scanner(file);
			while (input.hasNext()) {
				String string = input.next();
				if (string.equals("insert")) {
					// Gets all the fields for the creation of a Seminar object
					int id = input.nextInt();
					String title = (input.next() + input.nextLine()).trim();
					String date = input.next();
					int length = input.nextInt();
					short x = input.nextShort();
					short y = input.nextShort();
					int cost = input.nextInt();
					String[] keywords = (input.next() + input.nextLine()).split("\\s+");
					String desc = (input.next() + input.nextLine()).trim();

					// For insert commmand, parser sends database the params
					// for a new Seminar object. If database can add it, returns
					// String, else null.
					String answer = db.insert(id, title, date, length, x, y, cost, keywords, desc);
					if (answer != null) {
						output.write(answer);
						System.out.print(answer);
						output.write("Successfully inserted record with ID " + id + "\n");
						System.out.print("Successfully inserted record with ID " + id + "\n");
						output.write(db.search(id) + "\n");
						System.out.print(db.search(id) + "\n");
						output.write("Size: " + db.getSize(id) + "\n");
						System.out.print("Size: " + db.getSize(id) + "\n");
					} else {
						output.write("Insert FAILED - There is already a record" + " with ID " + id + "\n");
						System.out.print("Insert FAILED - There is already a record" + " with ID " + id + "\n");
					}

				} else if (string.equals("search")) {
					// For search command, parser sends database an id to
					// look for a Seminar
					// If database cannot return a Seminar, it returns null
					// If it can, it returns the Seminar's string
					int id = input.nextInt();

					if (db.search(id) != null) {
						output.write("Found record with ID " + id + ":\n");
						System.out.print("Found record with ID " + id + ":\n");
						output.write(db.search(id) + "\n");
						System.out.print(db.search(id) + "\n");
					} else {
						output.write("Search FAILED -- There is no record " + "with ID " + id + "\n");
						System.out.print("Search FAILED -- There is no record " + "with ID " + id + "\n");
					}
				} else if (string.equals("delete")) {
					// For delete command, parser sends database an id to delete
					// its Seminar
					// If database returns true, record is deleted. False if
					// not.
					int id = input.nextInt();

					if (db.delete(id) == true) {
						output.write("Record with ID " + id + " successfully deleted from " + "the database\n");
						System.out.print("Record with ID " + id + " successfully deleted from " + "the database\n");
					} else {
						output.write("Delete FAILED -- There is no record with ID " + id + "\n");
						System.out.print("Delete FAILED -- There is no record with ID " + id + "\n");
					}
				} else if (string.equals("print")) {
					// For print command, parser either makes database use
					// HashTable's print()
					// or MemoryManager's dump() depending on which is asked in
					// input file.
					String next = input.next();
					if (next.equals("hashtable")) {
						output.write("Hashtable:\n");
						System.out.print("Hashtable:\n");
						output.write(db.printHash());
						System.out.print(db.printHash());
						output.write("total records: " + db.getHashRecNum() + "\n");
						System.out.print("total records: " + db.getHashRecNum() + "\n");
					}
					if (next.equals("blocks")) {
						output.write("Freeblock List:\n");
						System.out.print("Freeblock List:\n");
						output.write(db.printFB());
						System.out.print(db.printFB());
					}
				}
			}

			input.close();
			output.close();
		} else {
			// Exception is thrown with no file to be found
			throw new FileNotFoundException("There is no file to be read");
		}
	}

}
