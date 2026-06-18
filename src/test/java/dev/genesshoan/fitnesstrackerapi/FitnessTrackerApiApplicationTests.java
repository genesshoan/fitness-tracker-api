package dev.genesshoan.fitnesstrackerapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.profiles.active=dev",
        "application.security.jwt.secret=J67jUDav4iCCnUYvBav7RJQ6fsgX8Kf4YL46Pwaym8o=",
        "application.security.jwt.expiration=86400000",
        "application.security.jwt.refresh-token.expiration=604800000"
})
class FitnessTrackerApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
