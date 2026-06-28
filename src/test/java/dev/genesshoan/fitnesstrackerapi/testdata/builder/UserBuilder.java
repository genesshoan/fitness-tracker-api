package dev.genesshoan.fitnesstrackerapi.testdata.builder;

import dev.genesshoan.fitnesstrackerapi.user.domain.Role;
import dev.genesshoan.fitnesstrackerapi.user.domain.User;
import java.util.UUID;
import net.datafaker.Faker;

public class UserBuilder {

    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private Role role = Role.USER;

    public UserBuilder(Faker faker) {
        this.username =
            faker.name().firstName().toLowerCase() +
            "_" +
            UUID.randomUUID().toString().substring(0, 8);
        this.email =
            faker.internet().emailAddress() +
            "." +
            UUID.randomUUID().toString().substring(0, 8);
        this.passwordHash = faker.internet().password();
    }

    public static UserBuilder aUser(Faker faker) {
        return new UserBuilder(faker);
    }

    public UserBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public UserBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public User build() {
        return User.builder()
            .id(id)
            .username(username)
            .email(email)
            .passwordHash(passwordHash)
            .role(role)
            .build();
    }
}
