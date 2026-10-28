package dev.genesshoan.fitnesstrackerapi.ratelimit.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.genesshoan.fitnesstrackerapi.ratelimit.application.exception.RateLimitExceededException;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.RateLimitExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Unit Tests - Rate Limit Exception Handler")
class RateLimitExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LimitedController())
            .setControllerAdvice(new RateLimitExceptionHandler())
            .build();

    @Test
    @DisplayName("maps the exception to a 429 problem detail with Retry-After")
    void mapsToTooManyRequests() throws Exception {
        mockMvc.perform(get("/limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "42"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.detail").value("Too many requests, retry later"))
                .andExpect(jsonPath("$.path").value("/limited"));
    }

    @RestController
    static class LimitedController {

        @GetMapping("/limited")
        void limited() {
            throw new RateLimitExceededException(42);
        }
    }
}
