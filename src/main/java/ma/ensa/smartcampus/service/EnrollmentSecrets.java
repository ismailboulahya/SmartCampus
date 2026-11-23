package ma.ensa.smartcampus.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec encryptionKey;
    private final TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator();

    public EnrollmentSecrets(@Value("${app.registration.encryption-key}") String encodedKey) {
        byte[] key = Base64.getDecoder().decode(encodedKey);
        if (key.length != 32) {
            throw new IllegalArgumentException("REGISTRATION_ENCRYPTION_KEY doit contenir 32 octets encodés en Base64.");
        }
        encryptionKey = new SecretKeySpec(key, "AES");
    }

    public String token() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));
    }

    public String hash(String token) {
        return DigestUtils.sha256Hex(token);
    }

    public String newTotpKey() {
        return new Base32().encodeToString(randomBytes(20));
    }

    public boolean matches(String manualKey, String code, Instant now) {
        var key = new SecretKeySpec(new Base32().decode(manualKey), totp.getAlgorithm());
        try {
            for (int offset = -1; offset <= 1; offset++) {
                String expected = totp.generateOneTimePasswordString(key, now.plusSeconds(offset * 30L), Locale.ROOT);
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), code.getBytes(StandardCharsets.US_ASCII))) {
                    return true;
                }
            }
            return false;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Vérification TOTP impossible.", ex);
        }
    }

    public String encrypt(String value, String context) {
        byte[] nonce = randomBytes(12);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, nonce, context);
            byte[] data = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + data.length)
                    .put(nonce).put(data).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Chiffrement impossible.", ex);
        }
    }

    public String decrypt(String value, String context) {
        byte[] data = Base64.getDecoder().decode(value);
        try {
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, Arrays.copyOf(data, 12), context);
            return new String(cipher.doFinal(data, 12, data.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Donnée d’inscription illisible.", ex);
        }
    }

    private Cipher cipher(int mode, byte[] nonce, String context) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, encryptionKey, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
