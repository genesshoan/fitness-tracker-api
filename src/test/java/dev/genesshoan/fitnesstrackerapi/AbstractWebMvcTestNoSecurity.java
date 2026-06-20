package dev.genesshoan.fitnesstrackerapi;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc(addFilters = false)
public abstract class AbstractWebMvcTestNoSecurity extends AbstractWebMvcTest {}
