package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.config.VerificationProperties;

/**
 * Utility class for securely hashing verification codes and tokens using HMAC.
 *
 * <p>
 * Provides cryptographic signing of verification codes to ensure integrity
 * and authenticity. Uses HMAC-SHA256 with a secret key to generate hashes
 * that are stored in the database instead of plain text codes.
 *
 * <p>
 * The hashing scheme incorporates both the code/token content and the
 * verification purpose to ensure each hash is unique to its intended use case.
 *
 * <p>
 * Hash functions:
 * <ul>
 * <li>{@link #hashCode(UUID, VerificationPurpose, String)} for verification codes</li>
 * <li>{@link #hashToken(VerificationPurpose, String)} for reset tokens</li>
 * </ul>
 */
@Component
public class CodeHasher {

    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    public CodeHasher(VerificationProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("verification secret must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(secret, ALGORITHM);
    }

    /**
     * Securely hashes a verification code using HMAC-SHA256.
     *
     * <p>
     * The hash incorporates the subject ID, verification purpose, and code content
     * to ensure uniqueness per purpose and user. Hashes are stored in the database
     * instead of plain text codes to prevent tampering.
     *
     * @param subjectId the user identifier associated with the code
     * @param purpose   the verification purpose (e.g. email verification)
     * @param code      the raw verification code to hash
     * @return the HMAC-SHA256 hash bytes
     */
    public byte[] hashCode(UUID subjectId, VerificationPurpose purpose, String code) {
        return hmac("code|" + subjectId + "|" + purpose.name() + "|" + code);
    }

    /**
     * Securely hashes a verification token using HMAC-SHA256.
     *
     * <p>
     * The hash incorporates the verification purpose and token content.
     * Used for tokens that are not tied to a specific user (e.g. reset tokens).
     *
     * @param purpose the verification purpose (e.g. email verification)
     * @param token   the raw token string to hash
     * @return the HMAC-SHA256 hash bytes
     */
    public byte[] hashToken(VerificationPurpose purpose, String token) {
        return hmac("token|" + purpose.name() + "|" + token);
    }

    /**
     * Compares two hash values in constant time to prevent timing attacks.
     *
     * @param expected the expected hash bytes
     * @param actual the actual hash bytes to compare against
     * @return true if the hashes are equal, false otherwise
     */
    public boolean matches(byte[] expected, byte[] actual) {
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] hmac(String input) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
