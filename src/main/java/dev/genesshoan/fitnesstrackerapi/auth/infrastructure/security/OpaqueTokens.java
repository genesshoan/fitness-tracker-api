package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for generating opaque refresh tokens and computing SHA-256 hashes.
 *
 * <p>This class provides cryptographically secure random token generation for
 * refresh tokens and secure hashing functions for token storage. All tokens
 * are generated using SecureRandom and encoded with URL-safe Base64 encoding.
 *
 * <p>The class is non-instantiable by design, serving only as a static utility
 * for token-related operations.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519#section-5.1.1">JWT ID (jti)</a>
 * @see java.security.SecureRandom
 */
public class OpaqueTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OpaqueTokens() {}

    /**
     * Generates a cryptographically secure random opaque token.
     *
     * <p>Creates a 256-bit random token using {@link SecureRandom}, encodes it
     * with URL-safe Base64 (without padding), and returns it as a string.
     * The resulting token is suitable for use as a refresh token or
     * verification token.
     *
     * @return a 43-character URL-safe Base64-encoded random token
     * @see SecureRandom
     * @see Base64.Encoder
     */
    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the SHA-256 hash of a token string for secure storage.
     *
     * <p>The hash is computed using UTF-8 encoding and returned as a
     * 32-byte array suitable for database storage. This prevents storing
     * plain tokens and enables secure lookup by hashing the incoming token
     * and comparing it against stored hashes.
     *
     * @param token the token string to hash
     * @return a 32-byte SHA-256 hash of the token
     * @throws IllegalStateException if SHA-256 algorithm is not available
     * @see MessageDigest
     */
    public static byte[] sha256(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
