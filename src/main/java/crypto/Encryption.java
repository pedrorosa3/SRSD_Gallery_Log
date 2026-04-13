package crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import Utils.Utils;
import models.DecryptedData;
import models.LineSeekResult;

public class Encryption {

    public static String encryptLog(String valuePlain, String token, byte[] prevMac, byte[] dbContent) {
        try {
            // Gerar IV aleatório (16 bytes para AES/CBC)
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Derivar chaves de encriptacao e mac do token
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] ke = new byte[16];
            byte[] km = new byte[16];

            System.arraycopy(hash, 0, ke, 0, 16);
            System.arraycopy(hash, 16, km, 0, 16);

            // c' ← Enc_ke(m)
            SecretKeySpec keySpec = new SecretKeySpec(ke, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] ciphertext = cipher.doFinal(valuePlain.getBytes(StandardCharsets.UTF_8));

            // Combinar IV + ciphertext = c
            byte[] c = new byte[iv.length + ciphertext.length];

            System.arraycopy(iv, 0, c, 0, iv.length);
            System.arraycopy(ciphertext, 0, c, iv.length, ciphertext.length);

            // Gerar mac
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec macKey = new SecretKeySpec(km, "HmacSHA256");
            mac.init(macKey);
            if (prevMac == null) {
                prevMac = new byte[32];
            }
            byte[] macInput = new byte[c.length + prevMac.length];
            System.arraycopy(c, 0, macInput, 0, c.length);
            System.arraycopy(prevMac, 0, macInput, c.length, prevMac.length);
            byte[] macValue = mac.doFinal(macInput);

            byte[] dbMac;
            mac.init(macKey);
            if (dbContent != null) {
                byte[] dbMacInput = new byte[dbContent.length + macValue.length];
                System.arraycopy(dbContent, 0, dbMacInput, 0, dbContent.length);
                System.arraycopy(macValue, 0, dbMacInput, dbContent.length, macValue.length);
                dbMac = mac.doFinal(dbMacInput);
            } else {
                dbMac = mac.doFinal(new byte[0]);
            }

            // res = c || macValue || dbMac
            byte[] res = new byte[c.length + macValue.length + dbMac.length];

            // Juntar ciphertext + MAC
            System.arraycopy(c, 0, res, 0, c.length);
            System.arraycopy(macValue, 0, res, c.length, macValue.length);
            System.arraycopy(dbMac, 0, res, c.length + macValue.length, dbMac.length);

            return Base64.getEncoder().encodeToString(res);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    public static String encryptDBRow(String valuePlain, String token) {
        try {
            // Gerar IV aleatório (16 bytes para AES/CBC)
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Derivar chaves de encriptacao e mac do token
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] ke = new byte[16];

            System.arraycopy(hash, 0, ke, 0, 16);

            // c' ← Enc_ke(m)
            SecretKeySpec keySpec = new SecretKeySpec(ke, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] ciphertext = cipher.doFinal(valuePlain.getBytes(StandardCharsets.UTF_8));

            // Combinar IV + ciphertext = c
            byte[] c = new byte[iv.length + ciphertext.length];

            System.arraycopy(iv, 0, c, 0, iv.length);
            System.arraycopy(ciphertext, 0, c, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(c);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    public static boolean checkIntegrity(String logPath, String token) {
        try {
            byte[] prevMac = new byte[32];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] km = new byte[16];
            System.arraycopy(hash, 16, km, 0, 16);
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec macKey = new SecretKeySpec(km, "HmacSHA256");
            mac.init(macKey);

            try (Scanner sc = new Scanner(new File(logPath))) {
                while (sc.hasNextLine()) {
                    byte[] row = Base64.getDecoder().decode(sc.nextLine());

                    if (row.length < 16 + 32) {
                        return false;
                    }

                    byte[] storedMac = Arrays.copyOfRange(row, row.length - 64, row.length - 32);
                    byte[] c = Arrays.copyOfRange(row, 0, row.length - 64);

                    byte[] macInput = new byte[c.length + prevMac.length];
                    System.arraycopy(c, 0, macInput, 0, c.length);
                    System.arraycopy(prevMac, 0, macInput, c.length, prevMac.length);
                    byte[] expectedMac = mac.doFinal(macInput);

                    if (!MessageDigest.isEqual(storedMac, expectedMac)) {
                        return false;
                    }

                    prevMac = storedMac;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public static boolean checkDbIntegrity(String logPath, String dbPath, String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] km = Arrays.copyOfRange(hash, 16, 32);

            LineSeekResult result = Utils.readNthLastLine(logPath, 1);
            if (result == null || result.line() == null || result.line().isEmpty()) {
                return false;
            }

            String lastLine = result.line().trim();

            byte[] row = Base64.getDecoder().decode(lastLine);

            if (row.length < 64) {
                return false;
            }

            byte[] storedDbMac = Arrays.copyOfRange(row, row.length - 32, row.length);
            byte[] lastLogMac = Arrays.copyOfRange(row, row.length - 64, row.length - 32);
            byte[] dbContent = Files.readAllBytes(Paths.get(dbPath));

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec macKey = new SecretKeySpec(km, "HmacSHA256");
            mac.init(macKey);

            byte[] dbMacInput = new byte[dbContent.length + lastLogMac.length];
            System.arraycopy(dbContent, 0, dbMacInput, 0, dbContent.length);
            System.arraycopy(lastLogMac, 0, dbMacInput, dbContent.length, lastLogMac.length);

            byte[] expectedDbMac = mac.doFinal(dbMacInput);

            return MessageDigest.isEqual(storedDbMac, expectedDbMac);

        } catch (Exception e) {
            return false;
        }
    }

    public static DecryptedData decryptLog(String rowBase64, String token) {
        try {
            // Derivar as chaves exatamente como no EncryptLog
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] ke = Arrays.copyOfRange(hash, 0, 16);

            // Descodificar de Base64
            byte[] rowBytes = Base64.getDecoder().decode(rowBase64);

            // Extrair o MAC, o IV e o Ciphertext
            byte[] iv = Arrays.copyOfRange(rowBytes, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(rowBytes, 16, rowBytes.length - 64);
            byte[] logMac = Arrays.copyOfRange(rowBytes, rowBytes.length - 64, rowBytes.length - 32);
            byte[] dbMac = Arrays.copyOfRange(rowBytes, rowBytes.length - 32, rowBytes.length);

            // Desencriptar o Ciphertext
            SecretKeySpec keySpec = new SecretKeySpec(ke, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainTextBytes = cipher.doFinal(ciphertext);
            return new DecryptedData(iv, ciphertext, logMac, dbMac, new String(plainTextBytes, StandardCharsets.UTF_8));

        } catch (Exception e) {
            // Se falhar a desencriptação ou o Base64 estiver corrompido
            return null;
        }
    }

    public static DecryptedData decryptDBRow(String rowBase64, String token) {
        try {
            // Derivar as chaves exatamente como no EncryptLog
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] ke = Arrays.copyOfRange(hash, 0, 16);

            // Descodificar de Base64
            byte[] rowBytes = Base64.getDecoder().decode(rowBase64);

            // Extrair o MAC, o IV e o Ciphertext
            byte[] iv = Arrays.copyOfRange(rowBytes, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(rowBytes, 16, rowBytes.length);

            // Desencriptar o Ciphertext
            SecretKeySpec keySpec = new SecretKeySpec(ke, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainTextBytes = cipher.doFinal(ciphertext);
            return new DecryptedData(iv, ciphertext, null, null, new String(plainTextBytes, StandardCharsets.UTF_8));

        } catch (Exception e) {
            // Se falhar a desencriptação ou o Base64 estiver corrompido
            return null;
        }
    }

}
