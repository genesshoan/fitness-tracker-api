package dev.genesshoan.fitnesstrackerapi.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.genesshoan.fitnesstrackerapi.auth.dto.LoginRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.RegisterRequestDTO;
import dev.genesshoan.fitnesstrackerapi.auth.dto.TokenResponseDTO;
import dev.genesshoan.fitnesstrackerapi.auth.service.AuthService;
import dev.genesshoan.fitnesstrackerapi.auth.service.JwtService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserDetailsService userDetailsService;

  @Test
  void registerShouldReturn201AndTokens() throws Exception {

    RegisterRequestDTO request = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "shoan@mail.com");

    TokenResponseDTO response = new TokenResponseDTO(
            "access-token",
            "refresh-token");

    when(authService.register(any(RegisterRequestDTO.class)))
            .thenReturn(response);

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.access_token").value("access-token"))
            .andExpect(jsonPath("$.refresh_token").value("refresh-token"));

    verify(authService).register(any(RegisterRequestDTO.class));
  }

  @Test
  void loginShouldReturn200AndTokens() throws Exception {

    LoginRequestDTO request = new LoginRequestDTO(
            "shoan@mail.com",
            "StrongPass123!");

    TokenResponseDTO response = new TokenResponseDTO(
            "access-token",
            "refresh-token");

    when(authService.login(any(LoginRequestDTO.class)))
            .thenReturn(response);

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("access-token"))
            .andExpect(jsonPath("$.refresh_token").value("refresh-token"));

    verify(authService).login(any(LoginRequestDTO.class));
  }

  @Test
  void refreshShouldReturn200AndNewTokens() throws Exception {

    TokenResponseDTO response = new TokenResponseDTO(
            "new-access",
            "new-refresh");

    when(authService.refreshToken(anyString()))
            .thenReturn(response);

    mockMvc.perform(post("/api/v1/auth/refresh")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value("new-access"))
            .andExpect(jsonPath("$.refresh_token").value("new-refresh"));

    verify(authService).refreshToken("Bearer refresh-token");
  }

  @Test
  void logoutShouldReturn204() throws Exception {

    mockMvc.perform(delete("/api/v1/auth/logout")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
            .andExpect(status().isNoContent());

    verify(authService).logout("Bearer access-token");
  }

  @Test
  void registerShouldReturn400WhenEmailIsInvalid() throws Exception {

    RegisterRequestDTO request = new RegisterRequestDTO(
            "shoan",
            "StrongPass123!",
            "invalid-email");

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

    verifyNoInteractions(authService);
  }
}
