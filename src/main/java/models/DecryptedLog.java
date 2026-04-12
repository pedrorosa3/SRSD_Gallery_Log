package models;

public record DecryptedLog(byte[] iv,
        byte[] ciphertext,
        byte[] mac,
        String plaintext) {
}
