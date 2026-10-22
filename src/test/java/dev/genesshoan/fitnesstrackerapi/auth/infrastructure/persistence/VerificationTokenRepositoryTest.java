package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationPurpose;
import dev.genesshoan.fitnesstrackerapi.auth.domain.VerificationToken;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.OpaqueTokens;
import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VerificationTokenRepositoryTest extends AbstractPostgresTest {

    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    TestEntityFactory testEntityFactory;

    @Autowired
    TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = testEntityFactory.createAndPersistUser();
    }

    @Test
    @DisplayName("Should find a verification token by hash and purpose")
    void findByHashAndPurpose_ShouldReturnTheToken() {
        // Given
        VerificationToken token = persistToken(
                VerificationPurpose.EMAIL_VERIFICATION, null, Instant.now().plusSeconds(3600));

        entityManager.flush();
        entityManager.clear();

        // When
        var result = verificationTokenRepository.findByHashAndPurpose(
                token.getHash(), VerificationPurpose.EMAIL_VERIFICATION);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(token.getId());
        assertThat(result.get().getSubjectId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should not find a verification token issued for another purpose")
    void findByHashAndPurpose_ShouldNotReturnTokenOfAnotherPurpose() {
        // Given
        VerificationToken token = persistToken(
                VerificationPurpose.EMAIL_VERIFICATION, null, Instant.now().plusSeconds(3600));

        entityManager.flush();
        entityManager.clear();

        // When / Then
        assertThat(verificationTokenRepository.findByHashAndPurpose(
                        token.getHash(), VerificationPurpose.PASSWORD_RESET))
                .isEmpty();
    }

    @Test
    @DisplayName("Should invalidate every active token of the subject and purpose")
    void invalidateActive_ShouldMarkActiveTokensAsUsed() {
        // Given
        Instant now = Instant.now();
        VerificationToken first = persistToken(VerificationPurpose.EMAIL_VERIFICATION, null, now.plusSeconds(3600));
        VerificationToken second = persistToken(VerificationPurpose.EMAIL_VERIFICATION, null, now.plusSeconds(3600));
        VerificationToken otherPurpose = persistToken(VerificationPurpose.PASSWORD_RESET, null, now.plusSeconds(3600));
        entityManager.flush();
        entityManager.clear();

        // When
        int invalidated =
                verificationTokenRepository.invalidateActive(user.getId(), VerificationPurpose.EMAIL_VERIFICATION, now);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(invalidated).isEqualTo(2);
        assertThat(verificationTokenRepository.findByHashAndPurpose(
                        first.getHash(), VerificationPurpose.EMAIL_VERIFICATION))
                .get()
                .extracting(VerificationToken::getUsedAt)
                .isNotNull();
        assertThat(verificationTokenRepository.findByHashAndPurpose(
                        second.getHash(), VerificationPurpose.EMAIL_VERIFICATION))
                .get()
                .extracting(VerificationToken::getUsedAt)
                .isNotNull();
        assertThat(verificationTokenRepository.findByHashAndPurpose(
                        otherPurpose.getHash(), VerificationPurpose.PASSWORD_RESET))
                .get()
                .extracting(VerificationToken::getUsedAt)
                .isNull();
    }

    @Test
    @DisplayName("Should mark a token as used only when it is active and not expired")
    void markUsed_ShouldOnlyMarkActiveTokens() {
        // Given
        Instant now = Instant.now();
        VerificationToken active = persistToken(VerificationPurpose.EMAIL_VERIFICATION, null, now.plusSeconds(3600));
        VerificationToken expired = persistToken(VerificationPurpose.EMAIL_VERIFICATION, null, now.minusSeconds(60));
        entityManager.flush();
        entityManager.clear();

        // When
        int markedActive = verificationTokenRepository.markUsed(active.getId(), now);
        int markedExpired = verificationTokenRepository.markUsed(expired.getId(), now);

        // Then
        assertThat(markedActive).isEqualTo(1);
        assertThat(markedExpired).isZero();
    }

    @Test
    @DisplayName("Should not mark an already used token as used again")
    void markUsed_ShouldNotMarkUsedTokensTwice() {
        // Given
        Instant now = Instant.now();
        VerificationToken token = persistToken(VerificationPurpose.EMAIL_VERIFICATION, now, now.plusSeconds(3600));
        entityManager.flush();
        entityManager.clear();

        // When
        int marked = verificationTokenRepository.markUsed(token.getId(), now);

        // Then
        assertThat(marked).isZero();
    }

    @Test
    @DisplayName("Should delete the expired tokens")
    void deleteExpiredBefore_ShouldDeleteOnlyExpiredTokens() {
        // Given
        Instant now = Instant.now();
        VerificationToken expired = persistToken(VerificationPurpose.EMAIL_VERIFICATION, null, now.minusSeconds(60));
        VerificationToken active = persistToken(VerificationPurpose.EMAIL_VERIFICATION, null, now.plusSeconds(3600));
        entityManager.flush();
        entityManager.clear();

        // When
        int deleted = verificationTokenRepository.deleteExpiredBefore(now);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(deleted).isEqualTo(1);
        assertThat(verificationTokenRepository.findByHashAndPurpose(
                        expired.getHash(), VerificationPurpose.EMAIL_VERIFICATION))
                .isEmpty();
        assertThat(verificationTokenRepository.findByHashAndPurpose(
                        active.getHash(), VerificationPurpose.EMAIL_VERIFICATION))
                .isPresent();
    }

    private VerificationToken persistToken(VerificationPurpose purpose, Instant usedAt, Instant expiresAt) {
        VerificationToken token = VerificationToken.builder()
                .subjectId(user.getId())
                .hash(OpaqueTokens.sha256("token-" + UUID.randomUUID()))
                .purpose(purpose)
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();

        return verificationTokenRepository.saveAndFlush(token);
    }
}
