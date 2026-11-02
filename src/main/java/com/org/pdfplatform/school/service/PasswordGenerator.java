package com.org.pdfplatform.school.service;

import java.security.SecureRandom;

public final class PasswordGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";   // no 0/O/1/l/I confusion
    private static final SecureRandom RND = new SecureRandom();

    private PasswordGenerator() { }

    public static String generate(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
