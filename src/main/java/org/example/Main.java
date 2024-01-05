package org.example;

import java.io.*;
import java.util.*;

public class Main {

    private static final int BUFFER_SIZE = 4096;
    private static final int BYTE_SIZE = 8;
    private static final int LEFTMOST_BIT_MASK = 0x80;

    public static class ByteArrayWrapper implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private final byte[] value;

        ByteArrayWrapper(byte[] value) {
            this.value = value;
        }

        byte[] getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ByteArrayWrapper that)) return false;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    static class HuffmanNode implements Serializable, Comparable<HuffmanNode> {
        @Serial
        private static final long serialVersionUID = 1L;
        ByteArrayWrapper value;
        int frequency;
        HuffmanNode left, right;

        HuffmanNode(ByteArrayWrapper value, int frequency) {
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

        @Serial
        private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
            objectOutputStream.defaultWriteObject();
            objectOutputStream.writeObject(left);
            objectOutputStream.writeObject(right);
        }

        @Serial
        private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
            objectInputStream.defaultReadObject();
            left = (HuffmanNode) objectInputStream.readObject();
            right = (HuffmanNode) objectInputStream.readObject();
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

        Map<ByteArrayWrapper, Integer> frequencies = calculateFrequencies(filePath, n);

        long time2 = System.currentTimeMillis();
        System.out.println("Reading and calculating frequencies time " + (time2 - time1) / 1000.0 + " seconds");

        HuffmanNode huffmanRoot = buildHuffmanTree(frequencies);
        Map<ByteArrayWrapper, String> huffmanCodes = new HashMap<>();
        long totalBitsEncoded = generateHuffmanCodes(huffmanRoot, huffmanCodes, "");

        long time3 = System.currentTimeMillis();
        System.out.println("Tree time " + (time3 - time2) / 1000.0 + " seconds");

        writeCompressedFile(filePath, n, totalBitsEncoded, huffmanRoot, huffmanCodes);

        long time4 = System.currentTimeMillis();
        System.out.println("Writing time " + (time4 - time3) / 1000.0 + " seconds");

        System.out.println("Compression completed in " + (time4 - time1) / 1000.0 + " seconds");
    }

    private static Map<ByteArrayWrapper, Integer> calculateFrequencies(String filePath, int n) {
        Map<ByteArrayWrapper, Integer> frequencies = new HashMap<>();
        try (FileInputStream fileInputStream = new FileInputStream(filePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            byte[] inputBuffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(inputBuffer)) != -1) {
                for (int i = 0; i < bytesRead; i += n) {
                    byte[] key = Arrays.copyOfRange(inputBuffer, i, Math.min(i + n, bytesRead));
                    frequencies.merge(new ByteArrayWrapper(key), 1, Integer::sum);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
            System.exit(1);
        }

        if (frequencies.isEmpty()) {
            System.err.println("File is empty or could not be read");
            System.exit(1);
        }
        return frequencies;
    }

    private static void writeCompressedFile(
            String inputFilePath,
            int n,
            long totalBitsEncoded,
            HuffmanNode root,
            Map<ByteArrayWrapper, String> huffmanCodes) {
        String outputFilePath = getCompressedFilePath(inputFilePath, n);
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
            objectOutputStream.writeLong(totalBitsEncoded);
            objectOutputStream.writeObject(root);

            writeEncodedData(inputFilePath, n, huffmanCodes, bufferedOutputStream);

            long inputFileSize = calculateFileSize(inputFilePath);
            long outputFileSize = calculateFileSize(outputFilePath);

            float compressionRatio = (float) (outputFileSize * 100) / (float) inputFileSize;
            System.out.println("Compression ratio: " + compressionRatio + "%");
        } catch (IOException e) {
            System.err.println("Error writing the compressed file: " + e.getMessage());
        }
    }

    private static String getCompressedFilePath(String filePath, int n) {
        File file = new File(filePath);
        String fileName = file.getName();
        String parentDir = file.getParent();
        String outputFileName = String.format("%s.%d.%s.hc", 20010382, n, fileName);
        if (parentDir != null)
            outputFileName = parentDir + File.separator + outputFileName;
        return outputFileName;
    }

    private static void writeEncodedData(
            String inputFilePath,
            int n,
            Map<ByteArrayWrapper, String> huffmanCodes,
            BufferedOutputStream bufferedOutputStream) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(inputFilePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            byte[] inputBuffer = new byte[BUFFER_SIZE];
            byte[] outputBuffer = new byte[BUFFER_SIZE];
            int bytesRead, outputIndex = 0;
            int bitsBuffer = 0, bitsBufferLength = 0;

            while ((bytesRead = bufferedInputStream.read(inputBuffer)) != -1) {
                for (int i = 0; i < bytesRead; i += n) {
                    byte[] key = Arrays.copyOfRange(inputBuffer, i, Math.min(i + n, bytesRead));
                    String bitCode = huffmanCodes.get(new ByteArrayWrapper(key));

                    for (int k = 0; k < bitCode.length(); k++) {
                        bitsBuffer <<= 1;
                        if (bitCode.charAt(k) == '1')
                            bitsBuffer |= 1;

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

            if (bitsBufferLength > 0) {
                bitsBuffer <<= (8 - bitsBufferLength);
                outputBuffer[outputIndex++] = (byte) bitsBuffer;
            }

            if (outputIndex > 0)
                bufferedOutputStream.write(outputBuffer, 0, outputIndex);
        }
    }

    private static long calculateFileSize(String filePath) {
        File file = new File(filePath);
        return file.length();
    }

    private static void decompress(String compressedFilePath) {
        long startTime = System.currentTimeMillis();

        String outputFilePath = getDecompressedFilePath(compressedFilePath);

        try (FileInputStream fileInputStream = new FileInputStream(compressedFilePath);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
             ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
             FileOutputStream fileOutputStream = new FileOutputStream(outputFilePath);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

            decompressData(bufferedInputStream, objectInputStream, bufferedOutputStream);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error during decompression: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Decompression completed in " + (endTime - startTime) / 1000.0 + " seconds");
    }

    private static String getDecompressedFilePath(String compressedFilePath) {
        File file = new File(compressedFilePath);
        String fileName = file.getName();
        String parentDir = file.getParent();
        if (fileName.endsWith(".hc"))
            fileName = fileName.substring(0, fileName.length() - 3);
        String decompressedFileName = String.format("%s.%s", "extracted", fileName);
        if (parentDir != null)
            decompressedFileName = parentDir + File.separator + decompressedFileName;
        return decompressedFileName;
    }

    private static void decompressData(
            BufferedInputStream bufferedInputStream,
            ObjectInputStream objectInputStream,
            BufferedOutputStream bufferedOutputStream) throws IOException, ClassNotFoundException {
        long totalBitsEncoded = objectInputStream.readLong();
        HuffmanNode huffmanRoot = (HuffmanNode) objectInputStream.readObject();

        int inputBufferIndex = 0;
        byte[] inputBuffer = new byte[BUFFER_SIZE];
        int bytesRead = bufferedInputStream.read(inputBuffer);

        HuffmanNode current = huffmanRoot;
        long totalBitsDecoded = 0;
        byte[] outputBuffer = new byte[BUFFER_SIZE];
        int outputBufferIndex = 0;

        while (totalBitsDecoded < totalBitsEncoded) {
            if (inputBufferIndex >= bytesRead) {
                bytesRead = bufferedInputStream.read(inputBuffer);
                inputBufferIndex = 0;
                if (bytesRead == -1)
                    break;
            }

            int byteBuffer = inputBuffer[inputBufferIndex++];
            int byteBufferLength = BYTE_SIZE;

            while (byteBufferLength > 0 && totalBitsDecoded < totalBitsEncoded) {
                current = ((byteBuffer & LEFTMOST_BIT_MASK) == 0) ? current.left : current.right;
                byteBuffer <<= 1;
                byteBufferLength--;

                if (current.isLeaf()) {
                    ByteArrayWrapper bytes = current.value;
                    for (byte aByte : bytes.getValue()) {
                        outputBuffer[outputBufferIndex++] = aByte;
                        if (outputBufferIndex == BUFFER_SIZE) {
                            bufferedOutputStream.write(outputBuffer, 0, BUFFER_SIZE);
                            outputBufferIndex = 0;
                        }
                    }
                    current = huffmanRoot;
                }
                totalBitsDecoded++;
            }
        }

        if (outputBufferIndex > 0)
            bufferedOutputStream.write(outputBuffer, 0, outputBufferIndex);
    }

    private static HuffmanNode buildHuffmanTree(Map<ByteArrayWrapper, Integer> frequencies) {
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();

        for (Map.Entry<ByteArrayWrapper, Integer> entry : frequencies.entrySet())
            pq.add(new HuffmanNode(entry.getKey(), entry.getValue()));

        while (pq.size() >= 2) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            assert right != null;
            HuffmanNode huffmanNode = new HuffmanNode(null, left.frequency + right.frequency);
            huffmanNode.left = left;
            huffmanNode.right = right;
            pq.add(huffmanNode);
        }

        return pq.poll();
    }

    private static long generateHuffmanCodes(
            HuffmanNode huffmanNode,
            Map<ByteArrayWrapper, String> huffmanCodes,
            String huffmanCode) {
        if (huffmanNode.isLeaf()) {
            huffmanCodes.put(huffmanNode.value, huffmanCode);
            return (long) huffmanCode.length() * huffmanNode.frequency;
        }

        long leftBitsCount = generateHuffmanCodes(huffmanNode.left, huffmanCodes, huffmanCode + "0");
        long rightBitsCount = generateHuffmanCodes(huffmanNode.right, huffmanCodes, huffmanCode + "1");
        return leftBitsCount + rightBitsCount;
    }

}
