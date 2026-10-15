package dev.genesshoan.fitnesstrackerapi.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            SELECT NEW dev.genesshoan.fitnesstrackerapi.user.application.UserTokenData(
                u.id,
                u.username,
                u.role,
                u.emailVerified
            )
            FROM User u
            WHERE u.email = :email
        """)
    Optional<UserTokenData> findTokenDataByEmail(@Param("email") String email);
}
