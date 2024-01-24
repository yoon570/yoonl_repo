import student.TestCase;

/**
 * This class tests the Node functionalities.
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
public class NodeTest extends TestCase {
    /**
     * Sets up the tests that follow. In general, used for initialization
     */
    public void setUp() {
        // Nothing here
    }


    /**
     * Tests the getNext() method.
     */
    public void testGetNext() {
        Node testNode = new Node(4, false, null, null);
        Node testNode2 = new Node(4, false, testNode, null);

        assertEquals(testNode, testNode2.getNext());
    }


    /**
     * Tests the getSize method.
     */
    public void testGetSize() {
        Node testNode = new Node(4, false, null, null);
        assertEquals(4, testNode.getSize());
    }


    /**
     * Tests the getIsFull method.
     */
    public void testGetIsFull() {
        Node testNode = new Node(4, false, null, null);
        Node testNode2 = new Node(4, true, null, null);
        assertEquals(testNode.getIsFull(), false);
        assertEquals(testNode2.getIsFull(), true);
    }


    /**
     * Tests the getPrev method.
     */
    public void testGetPrev() {
        Node testNode = new Node(4, false, null, null);
        Node testNode2 = new Node(8, true, null, null);

        testNode2.setPrev(testNode);

        assertEquals(testNode2.getPrev(), testNode);
    }


    /**
     * Tests the setNext() method.
     */
    public void testSetNext() {
        Node testNode = new Node(4, false, null, null);
        Node testNode2 = new Node(4, false, null, null);

        testNode2.setNext(testNode);
        assertEquals(testNode, testNode2.getNext());
    }


    /**
     * Tests the setSize method.
     */
    public void testSetSize() {
        Node testNode = new Node(4, false, null, null);
        testNode.setSize(8);
        assertEquals(8, testNode.getSize());
    }


    /**
     * Tests the setIsFull method.
     */
    public void testSetIsFull() {
        Node testNode = new Node(4, false, null, null);
        testNode.setIsFull(true);
        assertEquals(testNode.getIsFull(), true);
    }


    /**
     * Tests the splitNode method.
     */
    public void testSplitNode() {

        Node testNode = new Node(32, false, null, null);
        Node.splitNode(testNode);

        assertEquals(testNode.getSize(), 16);
        assertEquals(testNode.getIsFull(), false);

        Node.splitNode(testNode);
        assertEquals(testNode.getSize(), 8);
        assertEquals(testNode.getNext().getSize(), 8);
        assertEquals(testNode.getNext().getNext().getSize(), 16);

        Node.splitNode(testNode);
        assertEquals(testNode.getSize(), 4);
        assertEquals(testNode.getNext().getSize(), 4);
        assertEquals(testNode.getNext().getNext().getSize(), 8);
        assertEquals(testNode.getNext().getNext().getNext().getSize(), 16);

    }


    /**
     * Tests mergeNodes()
     */
    public void testMergeNodes() {

        Node testNode = new Node(32, false, null, null);

        // First split
        Node.splitNode(testNode);
        assertEquals(testNode.getBuddy(), 1);
        assertEquals(testNode.getNext().getBuddy(), 1);
        assertEquals(testNode.getSize(), 16);
        assertEquals(testNode.getNext().getSize(), 16);

        // Second split
        Node.splitNode(testNode);
        assertEquals(testNode.getBuddy(), 2);
        assertEquals(testNode.getNext().getBuddy(), 2);
        assertEquals(testNode.getNext().getNext().getBuddy(), 1);
        assertEquals(testNode.getSize(), 8);
        assertEquals(testNode.getNext().getSize(), 8);
        assertEquals(testNode.getNext().getNext().getSize(), 16);

        // Shouldn't merge, since buddy values are different
        Node.mergeNodes(testNode.getNext(), testNode.getNext().getNext());
        assertEquals(testNode.getBuddy(), 2);
        assertEquals(testNode.getNext().getBuddy(), 2);
        assertEquals(testNode.getNext().getNext().getBuddy(), 1);

        // Successful merger
        Node.mergeNodes(testNode, testNode.getNext());
        assertEquals(testNode.getBuddy(), 1);
        assertEquals(testNode.getNext().getBuddy(), 1);
        assertEquals(testNode.getSize(), 16);
        assertEquals(testNode.getNext().getSize(), 16);

        // Ditto
        Node.mergeNodes(testNode, testNode.getNext());
        assertEquals(testNode.getBuddy(), 0);
        assertEquals(testNode.getSize(), 32);
    }
}
