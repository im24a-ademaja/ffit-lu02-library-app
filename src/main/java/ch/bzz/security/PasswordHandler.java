package ch.bzz.security;

import java.security.NoSuchAlgorithmException;

public final class PasswordHandler {

    private PasswordHandler() {
    }

    public static byte[] generateSalt() {
        return ch.bzz.PasswordHandler.generateSalt();
    }

    public static byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        return ch.bzz.PasswordHandler.hashPassword(password, salt);
    }
}