package crypto;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import models.DecryptedLog;

public class Encryption {

    public static String EncryptLog(String valuePlain, String token, byte[] prevMac) {
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

            byte[] res = new byte[c.length + macValue.length];

            // Juntar ciphertext + MAC
            System.arraycopy(c, 0, res, 0, c.length);
            System.arraycopy(macValue, 0, res, c.length, macValue.length);

            return Base64.getEncoder().encodeToString(res);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    public static boolean CheckIntegrity(String logPath, String token) {
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

                    byte[] storedMac = Arrays.copyOfRange(row, row.length - 32, row.length);
                    byte[] c = Arrays.copyOfRange(row, 0, row.length - storedMac.length);

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
    
    public static DecryptedLog DecryptLog(String rowBase64, String token) {
        try {
            // Derivar as chaves exatamente como no EncryptLog
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            byte[] ke = Arrays.copyOfRange(hash, 0, 16);

            // Descodificar de Base64
            byte[] rowBytes = Base64.getDecoder().decode(rowBase64);
            
            // Extrair o MAC, o IV e o Ciphertext
            byte[] iv = Arrays.copyOfRange(rowBytes, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(rowBytes, 16, rowBytes.length - 32);
            byte[] mac = Arrays.copyOfRange(rowBytes, rowBytes.length - 32, rowBytes.length);

            // Desencriptar o Ciphertext
            SecretKeySpec keySpec = new SecretKeySpec(ke, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            byte[] plainTextBytes = cipher.doFinal(ciphertext);
            return new DecryptedLog(iv, ciphertext, mac, new String(plainTextBytes, StandardCharsets.UTF_8));

        } catch (Exception e) {
            // Se falhar a desencriptação ou o Base64 estiver corrompido
            return null; 
        }
    }

}
