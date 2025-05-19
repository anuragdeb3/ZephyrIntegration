package com.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class QshCalculator {
    public static String calculateQsh(String method, String uri) {
        String canonicalRequest = method.toUpperCase() + "&" + uri + "&";
        return sha256Hex(canonicalRequest);
    }

    private static String sha256Hex(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}