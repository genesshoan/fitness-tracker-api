package dev.genesshoan.fitnesstrackerapi.auth.domain;

/**
 * Enumeration of verification purposes for security codes.
 *
 * <p>
 * Each verification purpose represents a distinct use case for which
 * verification codes are generated and validated. This separation ensures
 * that codes issued for one purpose (e.g., email verification) cannot
 * be used for another purpose (e.g., password reset).
 */
public enum VerificationPurpose {
    EMAIL_VERIFICATION
}
