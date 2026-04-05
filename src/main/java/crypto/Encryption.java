package crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {

    private static String encryptEntry(String valuePlain, String token, byte[] prevMac) {
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

}
