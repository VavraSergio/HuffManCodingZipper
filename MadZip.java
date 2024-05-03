import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.PriorityQueue;

/**
 * MadZip class that compresses and unzips files.
 * 
 * <p>Zips and unzips files.
 *
 * @author Sergio Vavra
 * @version 4/2024
 *
 */
public class MadZip {

  /**
   * Zipper method that zips the file given.
   *
   * @param destinationFile Destination file
   * @param sourceFile Source file
   */
  public static void zip(File sourceFile, File destinationFile) throws IOException {

    if (sourceFile.length() == 1) {
      byte singleByte = singleByteRead(sourceFile);
      
      HashMap<Byte, Integer> singleFrequency = new HashMap<>();
      singleFrequency.put(singleByte, 1);
      
      HashMap<Byte, String> singleEncoding = new HashMap<>();
      singleEncoding.put(singleByte, "0");
      
      HuffmanSave huffmanSave = new HuffmanSave(new BitSequence(), singleFrequency);
      
      saveToFile(huffmanSave, destinationFile);
      
      return;
    }

    HashMap<Byte, Integer> frequency = calculateFrequency(sourceFile);

    PriorityQueue<Node> minHeap = convertToMinHeap(frequency);

    Node root = buildHuffmanTree(minHeap);

    HashMap<Byte, String> encodingMap = buildEncodingMap(root);

    BitSequence encodedData = encodeData(sourceFile, encodingMap);

    HuffmanSave huffmanSave = new HuffmanSave(encodedData, frequency);

    saveToFile(huffmanSave, destinationFile);

  }

  /**
   * Zipper method that unzips the file given.
   *
   * @param destinationFile Destination file
   * @param sourceFile Source file
   * @throws ClassNotFoundException throws if class not found
   */
  public static void unzip(File sourceFile, File destinationFile)
      throws IOException, ClassNotFoundException {

    HuffmanSave save = read(sourceFile);

    if (save.getFrequencies().size() == 1) {
      byte singleByte = save.getFrequencies().keySet().iterator().next();
      singleByteSave(destinationFile, singleByte);
      return;
    }

    BitSequence encodeData = save.getEncoding();
    HashMap<Byte, Integer> frequency = save.getFrequencies();

    PriorityQueue<Node> minHeap = convertToMinHeap(frequency);

    Node root = buildHuffmanTree(minHeap);

    HashMap<Byte, String> encodingMap = buildEncodingMap(root);

    decodeWriteData(encodeData, encodingMap, destinationFile);

  }

  private static HashMap<Byte, Integer> calculateFrequency(File sourceFile) throws IOException {
    HashMap<Byte, Integer> frequency = new HashMap<>();

    try (FileInputStream input = new FileInputStream(sourceFile)) {
      int read;
      while ((read = input.read()) != -1) {
        byte value = (byte) read;
        if (frequency.containsKey(value)) {
          frequency.put(value, frequency.get(value) + 1);
        } else {
          frequency.put(value, 1);
        }
      }
    } catch (Exception e) {
      throw new IOException("Invalid File.");
    }
    return frequency;
  }

  private static void decodeWriteData(BitSequence encodedData, HashMap<Byte, String> encodingMap,
      File destinationFile) throws ClassNotFoundException {
    try (FileOutputStream outputStream = new FileOutputStream(destinationFile)) {
      StringBuilder currentCode = new StringBuilder();
      for (int i = 0; i < encodedData.length(); i++) {
        int bit = encodedData.getBit(i);
        currentCode.append(bit);
        for (Byte key : encodingMap.keySet()) {
          String code = encodingMap.get(key);
          if (code.equals(currentCode.toString())) {
            outputStream.write(key);
            currentCode.setLength(0);
            break;
          }
        }
      }
    } catch (Exception e) {
      throw new ClassNotFoundException("Invalid writing");

    }
  }

  private static PriorityQueue<Node> convertToMinHeap(HashMap<Byte, Integer> frequency) {
    PriorityQueue<Node> minHeap =
        new PriorityQueue<>((a, b) -> Integer.compare(a.frequency, b.frequency));

    for (Byte b : frequency.keySet()) {
      minHeap.offer(new Node(b, frequency.get(b)));
    }

    return minHeap;
  }

  private static HuffmanSave read(File sourceFile) throws IOException {
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      return (HuffmanSave) objectInputStream.readObject();
    } catch (Exception e) {
      throw new IOException("Invalid file.");
    }
  }

  private static void saveToFile(HuffmanSave huffmanSave, File destinationFile) throws IOException {
    try (OutputStream outputStream =
        new BufferedOutputStream(new FileOutputStream(destinationFile))) {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
      objectOutputStream.writeObject(huffmanSave);
      objectOutputStream.flush();
    } catch (Exception e) {
      throw new IOException();
    }
  }

  private static BitSequence encodeData(File sourceFile, HashMap<Byte, String> encodingMap)
      throws IOException {
    BitSequence data = new BitSequence();
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
      int bytes;
      while ((bytes = inputStream.read()) != -1) {
        byte bytesInput = (byte) bytes;
        String encoding = encodingMap.get(bytesInput);
        data.appendBits(encoding);
      }
    }
    return data;
  }

  private static HashMap<Byte, String> buildEncodingMap(Node root) {
    HashMap<Byte, String> encodingMap = new HashMap<>();
    StringBuilder currentEncoding = new StringBuilder();
    traverseTree(root, currentEncoding, encodingMap);
    return encodingMap;
  }

  private static void traverseTree(Node node, StringBuilder currentEncoding,
      HashMap<Byte, String> encodingMap) {
    if (node != null) {
      if (node.isLeaf()) {
        encodingMap.put(node.data, currentEncoding.toString());
      } else {
        currentEncoding.append('0');
        traverseTree(node.left, currentEncoding, encodingMap);
        currentEncoding.deleteCharAt(currentEncoding.length() - 1);

        currentEncoding.append('1');
        traverseTree(node.right, currentEncoding, encodingMap);
        currentEncoding.deleteCharAt(currentEncoding.length() - 1);
      }
    }
  }

  private static Node buildHuffmanTree(PriorityQueue<Node> minHeap) {
    while (minHeap.size() > 1) {
      Node left = minHeap.poll();
      Node right = minHeap.poll();

      Node root = new Node((byte) 0, left.frequency + right.frequency);
      root.left = left;
      root.right = right;

      minHeap.offer(root);
    }

    return minHeap.poll();
  }

  private static byte singleByteRead(File sourceFile) throws IOException {
    try (InputStream inputStream = new BufferedInputStream(new FileInputStream(sourceFile))) {
      return (byte) inputStream.read();
    }
  }

  private static void singleByteSave(File destinationFile, byte single) throws IOException {
    try (OutputStream outputStream =
        new BufferedOutputStream(new FileOutputStream(destinationFile))) {
      outputStream.write(single);
    }
  }


}
