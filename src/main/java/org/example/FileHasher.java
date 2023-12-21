package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHasher {

    public static void main(String[] args) {
        String filePathGenerated = "D:\\Study\\Semester 5\\Algorithms\\Labs\\Submitted\\Lab 2\\Files\\lecture_uncompressed.pdf";
        String filePathOutOriginal = "D:\\Study\\Semester 5\\Algorithms\\Labs\\Submitted\\Lab 2\\Files\\lecture.pdf";

        try {
            byte[] fileBytesIn = readFileToByteArray(filePathGenerated);
            byte[] fileBytesOut = readFileToByteArray(filePathOutOriginal);
            String sha256HexIn = getSHA256(fileBytesIn);
            String sha256HexOut = getSHA256(fileBytesOut);
            System.out.println("SHA-256 Hash Generated: " + sha256HexIn);
            System.out.println("SHA-256 Hash Original: " + sha256HexOut);
            System.out.println(sha256HexIn.equals(sha256HexOut));
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
    }

    private static byte[] readFileToByteArray(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        byte[] data = new byte[fis.available()];
        fis.read(data);
        fis.close();
        return data;
    }

    private static String getSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(data);

        // Convert byte array to a string representation in hexadecimal
        StringBuilder hexString = new StringBuilder();
        for (byte hashByte : hashBytes) {
            String hex = Integer.toHexString(0xff & hashByte);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}