package dev.genesshoan.fitnesstrackerapi.user.domain;

import dev.genesshoan.fitnesstrackerapi.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
    @UniqueConstraint(name = "uk_user_email", columnNames = "email")
}, indexes = {
    @Index(name = "idx_user_email", columnList = "email")
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
  private Role role;
}
