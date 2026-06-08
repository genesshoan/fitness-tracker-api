package dev.genesshoan.fitnesstrackerapi.auth.domain;

import java.time.Instant;
import java.util.UUID;

import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent representation of JWT refresh tokens.
 *
 * Refresh tokens are used in the authentication flow to obtain new access
 * tokens
 * after the current access token expires, without requiring re-authentication.
 *
 * Each refresh token is uniquely identified by its JWT ID (jti) and is stored
 * to enable server-side control over session validity.
 *
 * Tokens are organized by a family identifier (familyId) which represents a
 * login session. This allows multiple refresh tokens to be related to the same
 * session and supports multi-device authentication.
 *
 * Refresh tokens are rotated on each use: a new token is issued and the
 * previous
 * token can be revoked to prevent reuse.
 *
 * Storing refresh tokens enables logout, session management, and detection of
 * compromised or reused tokens.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tokens")
public class Token {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID jti;

  @Column(nullable = false)
  private UUID familyId;

  @Builder.Default
  private boolean revoked = false;

  @Column(nullable = false)
  private Instant expiresAt;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
