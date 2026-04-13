package models;

public record DecryptedData(byte[] iv,
        byte[] ciphertext,
        byte[] logMac,
        byte[] dbMac,
        String plaintext) {
}
