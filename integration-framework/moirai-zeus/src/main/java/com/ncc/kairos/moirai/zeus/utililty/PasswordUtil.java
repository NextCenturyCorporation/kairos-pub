package com.ncc.kairos.moirai.zeus.utililty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {

    @Value("${jwt.password.encoding.strength}")
    private static int strength;

    // Private null constructor
    private PasswordUtil() {

    }

    public static String generateAccessCode() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()_+?";
        StringBuilder salt = new StringBuilder();
        SecureRandom rnd = new SecureRandom();
        while (salt.length() < 18) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    public static String getSecurePassword(String passwordToHash) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12, new SecureRandom());
        return encoder.encode(passwordToHash);
    }

    public static String encode(String passwordToHash) {
        return Base64.getEncoder().encodeToString(passwordToHash.getBytes());
    }

    public static String decode(String passwordToHash) {
        return new String(Base64.getDecoder().decode(passwordToHash));
    }

    public static void passwordValidation(String password) {
        int passwordMax = 30;
        int passwordMin = 5;

        if (password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Password cannot be null");
        } else if (password.length() < passwordMin || password.length() > passwordMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Password must be between " + passwordMin + " and " + passwordMax + " characters in length.");
        }
    }

    public static byte[] createPasswordSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }
}
