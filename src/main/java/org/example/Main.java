package org.example;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class Main {

    private static final int BUFFER_SIZE = 4096;
    private static final int BYTE_SIZE = 8;
    private static final int LEFTMOST_BIT_MASK = 0x80;

    static class HuffmanNode implements Comparable<HuffmanNode> {
        String value;
        int frequency;
        HuffmanNode left, right;

        HuffmanNode(String value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        public boolean isLeaf() {
            return this.left == null && this.right == null;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return this.frequency - other.frequency;
        }
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            if (Objects.equals(args[0], "c")) {
                if (args.length > 2) {
                    compress(args[1], Integer.parseInt(args[2]));
                }
                else {
                    System.out.println("No enough args provided!");
                    System.exit(1);
                }
            }
            else if (Objects.equals(args[0], "d")) {
                decompress(args[1]);
            }
            else {
                System.out.println("Args provided are not correct");
                System.exit(1);
            }
        } else {
            System.out.println("No enough args provided!");
            System.exit(1);
        }
    }

    private static void compress(String filePath, int n) {
        long time1 = System.currentTimeMillis();

        Map<String, Integer> frequencies = calculateFrequencies(filePath, n);

        long time2 = System.currentTimeMillis();
        System.out.println("Reading time " + (time2 - time1) / 1000.0 + " seconds");

        HuffmanNode root = buildHuffmanTree(frequencies);
        Map<String, String> huffmanCodes = new HashMap<>();
        long totalBitsEncoded = generateHuffmanCodes(root, huffmanCodes, "");

        long time3 = System.currentTimeMillis();
        System.out.println("Tree time " + (time3 - time2) / 1000.0 + " seconds");

        writeCompressedFile(filePath, n, frequencies, huffmanCodes, totalBitsEncoded);

        long time4 = System.currentTimeMillis();
        System.out.println("Writing time " + (time4 - time3) / 1000.0 + " seconds");

        System.out.println("Compression completed in " + (time4 - time1) / 1000.0 + " seconds");

        decompress(filePath + ".hc");
    }

    private static Map<String, Integer> calculateFrequencies(String filePath, int n) {
        Map<String, Integer> frequencies = new HashMap<>();
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i += n) {
                    String key = new String(buffer, i, Math.min(n, bytesRead - i), StandardCharsets.ISO_8859_1);
                    frequencies.merge(key, 1, Integer::sum);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }

        if (frequencies.isEmpty()) {
            System.err.println("File is empty or could not be read");
            System.exit(1);
        }
        return frequencies;
    }

    private static void writeCompressedFile(
            String filePath,
            int n,
            Map<String, Integer> frequencies,
            Map<String, String> huffmanCodes,
            long totalBitsEncoded) {
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filePath + ".hc"));
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
            objectOutputStream.writeLong(totalBitsEncoded);
            objectOutputStream.writeObject(frequencies);

            writeEncodedData(filePath, n, huffmanCodes, bufferedOutputStream);
        } catch (IOException e) {
            System.err.println("Error writing the compressed file: " + e.getMessage());
        }
    }

    private static void writeEncodedData(String filePath, int n, Map<String, String> huffmanCodes, BufferedOutputStream bufferedOutputStream) throws IOException {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            byte[] outputBuffer = new byte[BUFFER_SIZE];
            int bytesRead, outputIndex = 0;
            int bitsBuffer = 0, bitsBufferLength = 0;

            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i += n) {
                    int end = Math.min(i + n, bytesRead);
                    String key = new String(buffer, i, end - i, StandardCharsets.ISO_8859_1);
                    String bitCode = huffmanCodes.get(key);

                    for (int k = 0; k < bitCode.length(); k++) {
                        bitsBuffer <<= 1;
                        if (bitCode.charAt(k) == '1') {
                            bitsBuffer |= 1;
                        }

                        bitsBufferLength++;
                        if (bitsBufferLength == 8) {
                            outputBuffer[outputIndex++] = (byte) bitsBuffer;
                            if (outputIndex == outputBuffer.length) {
                                bufferedOutputStream.write(outputBuffer, 0, outputIndex);
                                outputIndex = 0;
                            }
                            bitsBuffer = 0;
                            bitsBufferLength = 0;
                        }
                    }
                }
            }

            // Handle any remaining bits
            if (bitsBufferLength > 0) {
                bitsBuffer <<= (8 - bitsBufferLength);
                outputBuffer[outputIndex++] = (byte) bitsBuffer;
            }

            // Write any remaining data in output buffer to the stream
            if (outputIndex > 0)
                bufferedOutputStream.write(outputBuffer, 0, outputIndex);
        }
    }

    private static void decompress(String compressedFilePath) {
        long startTime = System.currentTimeMillis();

        File inputFile = new File(compressedFilePath);
        String outputFilePath = getOutputFilePath(compressedFilePath);
        File outputFile = new File(outputFilePath);

        try (FileInputStream fileInputStream = new FileInputStream(inputFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

            decompressData(bufferedInputStream, objectInputStream, bufferedOutputStream);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error during decompression: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Decompression completed in " + (endTime - startTime) / 1000.0 + " seconds");
    }

    private static String getOutputFilePath(String compressedFilePath) {
        compressedFilePath = compressedFilePath.substring(0, compressedFilePath.length() - 3);
        int extensionIndex = compressedFilePath.lastIndexOf('.');
        return compressedFilePath.substring(0, extensionIndex) + "_uncompressed"
                + compressedFilePath.substring(extensionIndex);
    }

    private static void decompressData(
            BufferedInputStream bufferedInputStream,
            ObjectInputStream objectInputStream,
            BufferedOutputStream bufferedOutputStream) throws IOException, ClassNotFoundException {

        long totalBitsEncoded = objectInputStream.readLong();
        Map<String, Integer> frequencies = (Map<String, Integer>) objectInputStream.readObject();

        HuffmanNode root = buildHuffmanTree(frequencies);

        int inputBufferIndex = 0;
        byte[] inputBuffer = new byte[BUFFER_SIZE];
        int bytesRead = bufferedInputStream.read(inputBuffer);

        HuffmanNode current = root;
        long totalBitsDecoded = 0;
        int outputBufferIndex = 0;
        byte[] outputBuffer = new byte[BUFFER_SIZE];

        while (totalBitsDecoded < totalBitsEncoded) {
            if (inputBufferIndex >= bytesRead) {
                bytesRead = bufferedInputStream.read(inputBuffer);
                inputBufferIndex = 0;
                if (bytesRead == -1)
                    break;
            }

            int byteBuffer = inputBuffer[inputBufferIndex++] & 0xFF;
            int byteBufferLength = BYTE_SIZE;

            while (byteBufferLength > 0 && totalBitsDecoded < totalBitsEncoded) {
                current = ((byteBuffer & LEFTMOST_BIT_MASK) == 0) ? current.left : current.right;
                byteBuffer <<= 1;
                byteBufferLength--;

                if (current.isLeaf()) {
                    byte[] bytes = current.value.getBytes(StandardCharsets.ISO_8859_1);
                    for (byte aByte : bytes) {
                        outputBuffer[outputBufferIndex++] = aByte;
                        if (outputBufferIndex == BUFFER_SIZE) {
                            bufferedOutputStream.write(outputBuffer, 0, BUFFER_SIZE);
                            outputBufferIndex = 0;
                        }
                    }
                    current = root;
                }
                totalBitsDecoded++;
            }
        }

        if (outputBufferIndex > 0)
            bufferedOutputStream.write(outputBuffer, 0, outputBufferIndex);
    }

    private static HuffmanNode buildHuffmanTree(Map<String, Integer> frequencies) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();

        for (Map.Entry<String, Integer> entry : frequencies.entrySet())
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));

        while (pq.size() >= 2) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            assert right != null;
            HuffmanNode huffmanNode = new HuffmanNode("", left.frequency + right.frequency);
            huffmanNode.left = left;
            huffmanNode.right = right;
            pq.add(huffmanNode);
        }

        return pq.poll();
    }

    private static long generateHuffmanCodes(HuffmanNode huffmanNode, Map<String, String> huffmanCodes, String huffmanCode) {
        if (huffmanNode.isLeaf()) {
            huffmanCodes.put(huffmanNode.value, huffmanCode);
            return (long) huffmanCode.length() * huffmanNode.frequency;
        }

        long leftBitsCount = generateHuffmanCodes(huffmanNode.left, huffmanCodes, huffmanCode + "0");
        long rightBitsCount = generateHuffmanCodes(huffmanNode.right, huffmanCodes, huffmanCode + "1");
        return leftBitsCount + rightBitsCount;
    }

}
