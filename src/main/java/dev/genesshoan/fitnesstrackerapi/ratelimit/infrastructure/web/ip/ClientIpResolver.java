package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import jakarta.servlet.http.HttpServletRequest;

public interface ClientIpResolver {

    String resolve(HttpServletRequest request);
}
