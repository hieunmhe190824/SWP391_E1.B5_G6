package com.group3.carrental.security;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EnvReader {
    private EnvReader() {}
    public static String load(String key) {
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String value = br.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1).trim())
                .findFirst()
                .orElse(null);
            
            if (value == null) {
                System.err.println("Error: Variable '" + key + "' not found in .env file");
            }
            return value;
        } catch (IOException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
            return null;
        }
    }
}