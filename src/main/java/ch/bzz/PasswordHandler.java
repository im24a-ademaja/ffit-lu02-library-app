package ch.bzz;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordHandler {

    private PasswordHandler() {
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hashPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(salt);
        return messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean verifyPassword(String inputPassword, byte[] storedHash, byte[] storedSalt)
            throws NoSuchAlgorithmException {
        byte[] hashedInput = hashPassword(inputPassword, storedSalt);
        return MessageDigest.isEqual(hashedInput, storedHash);
    }
}