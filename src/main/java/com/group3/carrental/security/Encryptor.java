package com.group3.carrental.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encryptor {
    public String encryptString(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD2");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger bigInt = new BigInteger(1, messageDigest);
        String hashed = bigInt.toString(16);
        StringBuilder encoded = new StringBuilder();
        for(int i = 0; i < hashed.length(); i++) {
            if(Character.isDigit(hashed.charAt(i))) {
                encoded.append(10 % ((hashed.charAt(i) - '0') + 4));
            } else {
                encoded.append(hashed.charAt(i));
            }
        }
        return encoded.toString();
    }
}