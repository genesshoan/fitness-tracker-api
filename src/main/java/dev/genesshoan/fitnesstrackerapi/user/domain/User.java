package dev.genesshoan.fitnesstrackerapi.user.domain;

import jakarta.persistence.*;

import dev.genesshoan.fitnesstrackerapi.common.domain.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Core user entity representing a registered application user.
 *
 * <p>
 * Stores essential user account information including authentication
 * credentials, profile details, and verification status. Users are
 * associated with roles that control their access to application features.
 */
@Entity
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_user_email", columnNames = "email"),
        })
public class User extends BaseEntity {

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "profile_picture_key")
    private String profilePictureKey;
}
