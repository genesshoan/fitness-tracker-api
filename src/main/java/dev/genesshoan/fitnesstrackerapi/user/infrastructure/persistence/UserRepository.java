package dev.genesshoan.fitnesstrackerapi.user.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData;
import dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.emailVerified = true
        WHERE u.id = :id
        """)
    int markEmailVerified(@Param("id") UUID id);

    @Query("""
            SELECT NEW dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials(
                u.id,
                u.email,
                u.username,
                u.passwordHash,
                u.emailVerified,
                u.role
            )
            FROM User u
            WHERE u.email = :email
        """)
    Optional<UserCredentials> findCredentialsByEmail(@Param("email") String email);

    @Query("""
            SELECT NEW dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials(
                u.id,
                u.email,
                u.username,
                u.passwordHash,
                u.emailVerified,
                u.role
            )
            FROM User u
            WHERE u.email = :identifier
                OR u.username = :identifier
        """)
    Optional<UserCredentials> findCredentialsByIdentifier(@Param("identifier") String identifier);

    @Query("""
            SELECT NEW dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.UserCredentials(
                u.id,
                u.email,
                u.username,
                u.passwordHash,
                u.emailVerified,
                u.role
            )
            FROM User u
            WHERE u.id = :id
        """)
    Optional<UserCredentials> findCredentialsById(@Param("id") UUID id);

    @Query("""
            SELECT NEW dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.RefreshUserData(
                u.role
            )
            FROM User u
            WHERE u.id = :id
        """)
    Optional<RefreshUserData> findRefreshUserDataById(@Param("id") UUID id);

    @Modifying
    @Query("""
        UPDATE User u
        SET u.passwordHash = :passwordHash
        WHERE u.id = :id
        """)
    int updatePasswordHash(@Param("id") UUID id, @Param("passwordHash") String passwordHash);

    @Query("""
            SELECT NEW dev.genesshoan.fitnesstrackerapi.user.application.port.in.result.PendingVerificationUser(
                u.id,
                u.username
            )
            FROM User u
            WHERE u.email = :email
                AND emailVerified = false
        """)
    Optional<PendingVerificationUser> findUnverifiedByEmail(@Param("email") String email);

    @Query("""
            SELECT u.timezone
            FROM User u
            WHERE u.id = :id
        """)
    Optional<String> findTimezoneById(@Param("id") UUID id);
}
