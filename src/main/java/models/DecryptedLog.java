package models;

public record DecryptedLog(byte[] iv,
        byte[] ciphertext,
        byte[] logMac,
        byte[] dbMac,
        String plaintext) {
}
