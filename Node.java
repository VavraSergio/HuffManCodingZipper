/**
 * Node class that I created because I couldn't find any others.
 * 
 * <p>Node helper class
 *
 * @author Sergio Vavra
 * @version 4/2024
 *
 */
public class Node {
  byte data;
  int frequency;
  Node left;
  Node right;

  /**
   * Constructor to create node.
   *
   * @param data Data file
   * @param frequency Frequency of byte
   */
  public Node(byte data, int frequency) {
    this.data = data;
    this.frequency = frequency;
    this.left = null;
    this.right = null;
  }
  
  public boolean isLeaf() {
    return left == null && right == null;
  }
  
}
