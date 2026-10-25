package dev.genesshoan.fitnesstrackerapi.security;

import dev.genesshoan.fitnesstrackerapi.base.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration Tests - CORS")
class CorsIT extends AbstractIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    private static final String UNKNOWN_ORIGIN = "http://unknown.example.com";

    @Test
    @DisplayName("Should answer the preflight request of an allowed origin")
    void should_answerPreflightRequest_when_originIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS"));
    }

    @Test
    @DisplayName("Should reject the preflight request of an unknown origin")
    void should_rejectPreflightRequest_when_originIsNotAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header("Origin", UNKNOWN_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should add the cors headers to an actual request of an allowed origin")
    void should_addCorsHeaders_when_originIsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/sessions").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("Should reject an actual request of an unknown origin")
    void should_rejectActualRequest_when_originIsNotAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/sessions").header("Origin", UNKNOWN_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
