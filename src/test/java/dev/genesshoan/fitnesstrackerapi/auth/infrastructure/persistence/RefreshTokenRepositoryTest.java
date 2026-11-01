package dev.genesshoan.fitnesstrackerapi.auth.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import dev.genesshoan.fitnesstrackerapi.auth.domain.RefreshToken;
import dev.genesshoan.fitnesstrackerapi.auth.infrastructure.security.OpaqueTokens;
import dev.genesshoan.fitnesstrackerapi.base.AbstractPostgresTest;
import dev.genesshoan.fitnesstrackerapi.testdata.TestEntityFactory;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RefreshTokenRepositoryTest extends AbstractPostgresTest {

    private static final String TOKEN = "raw-refresh-token";

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

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
    @DisplayName("Should find a refresh token by its hash")
    void findByHash_ShouldReturnToken() {
        // Given
        RefreshToken token = persistToken(UUID.randomUUID(), false);

        entityManager.flush();
        entityManager.clear();

        // When
        var result = refreshTokenRepository.findByHash(token.getHash());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(token.getId());
        assertThat(result.get().getUserId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("Should return empty when the hash is unknown")
    void findByHash_ShouldReturnEmptyWhenUnknown() {
        // When / Then
        assertThat(refreshTokenRepository.findByHash(OpaqueTokens.sha256("unknown")))
                .isEmpty();
    }

    @Test
    @DisplayName("Should report whether a token exists and is not revoked")
    void existsByIdAndRevokedFalse_ShouldReflectRevocation() {
        // Given
        RefreshToken token = persistToken(UUID.randomUUID(), false);
        entityManager.flush();

        // Then
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(token.getId()))
                .isTrue();

        // When
        refreshTokenRepository.revokeById(token.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(token.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Should revoke a single token only once")
    void revokeById_ShouldRevokeOnlyActiveTokens() {
        // Given
        RefreshToken token = persistToken(UUID.randomUUID(), false);
        entityManager.flush();

        // When
        int first = refreshTokenRepository.revokeById(token.getId());
        int second = refreshTokenRepository.revokeById(token.getId());

        // Then
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
    }

    @Test
    @DisplayName("Should revoke every token of a family")
    void revokeByFamily_ShouldRevokeTheWholeFamily() {
        // Given
        UUID familyId = UUID.randomUUID();
        RefreshToken first = persistToken(familyId, false);
        RefreshToken second = persistToken(familyId, false);
        RefreshToken other = persistToken(UUID.randomUUID(), false);
        entityManager.flush();

        // When
        refreshTokenRepository.revokeByFamily(familyId);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(first.getId()))
                .isFalse();
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(second.getId()))
                .isFalse();
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(other.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("Should revoke the family of a token given its hash")
    void revokeByHash_ShouldRevokeTheFamilyOfTheGivenToken() {
        // Given
        UUID familyId = UUID.randomUUID();
        RefreshToken token = persistToken(familyId, false);
        RefreshToken sibling = persistToken(familyId, false);
        entityManager.flush();

        // When
        int revoked = refreshTokenRepository.revokeByHash(token.getHash());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(revoked).isEqualTo(2);
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(token.getId()))
                .isFalse();
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(sibling.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Should revoke every token of a user")
    void revokeByUserId_ShouldRevokeEveryTokenOfTheUser() {
        // Given
        RefreshToken token = persistToken(UUID.randomUUID(), false);
        entityManager.flush();

        // When
        refreshTokenRepository.revokeByUserId(user.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(refreshTokenRepository.existsByIdAndRevokedFalse(token.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Should keep revoked tokens while their family has an active token")
    void deleteExpiredAndInactiveFamilies_ShouldKeepFamiliesWithAnActiveToken() {
        // Given
        Instant now = Instant.now();
        UUID activeFamily = UUID.randomUUID();
        RefreshToken revokedSibling = persistToken(activeFamily, true, now.plusSeconds(3600));
        RefreshToken activeSibling = persistToken(activeFamily, false, now.plusSeconds(3600));
        RefreshToken inactiveFamilyToken = persistToken(UUID.randomUUID(), true, now.plusSeconds(3600));
        RefreshToken expiredToken = persistToken(UUID.randomUUID(), false, now.minusSeconds(60));
        entityManager.flush();
        entityManager.clear();

        // When
        int deleted = refreshTokenRepository.deleteExpiredAndInactiveFamilies(now);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(deleted).isEqualTo(2);
        assertThat(refreshTokenRepository.findByHash(revokedSibling.getHash())).isPresent();
        assertThat(refreshTokenRepository.findByHash(activeSibling.getHash())).isPresent();
        assertThat(refreshTokenRepository.findByHash(inactiveFamilyToken.getHash()))
                .isEmpty();
        assertThat(refreshTokenRepository.findByHash(expiredToken.getHash())).isEmpty();
    }

    private RefreshToken persistToken(UUID familyId, boolean revoked) {
        return persistToken(familyId, revoked, Instant.now().plusSeconds(3600));
    }

    private RefreshToken persistToken(UUID familyId, boolean revoked, Instant expiresAt) {
        RefreshToken token = RefreshToken.builder()
                .familyId(familyId)
                .userId(user.getId())
                .hash(OpaqueTokens.sha256(TOKEN + "-" + UUID.randomUUID()))
                .revoked(revoked)
                .expiresAt(expiresAt)
                .build();

        return refreshTokenRepository.saveAndFlush(token);
    }
}
