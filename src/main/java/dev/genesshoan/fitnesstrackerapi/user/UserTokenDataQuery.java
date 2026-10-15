package dev.genesshoan.fitnesstrackerapi.user;

import java.util.Optional;

public interface UserTokenDataQuery {

    Optional<UserTokenData> findByEmail(String email);
}
