package dev.genesshoan.fitnesstrackerapi.base;

import dev.genesshoan.fitnesstrackerapi.auth.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class AbstractWebMvcTest {

    @MockitoBean
    protected JwtService jwtService;

    @MockitoBean
    protected UserDetailsService userDetailsService;
}
