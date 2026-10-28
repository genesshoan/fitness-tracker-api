package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IpNormalizer {

    private static final int DEFAULT_IPV6_PREFIX = 64;
    private static final String UNKNOWN_IP = "unknown";

    private final RateLimitProperties properties;

    public String normalize(String ip) {
        if (ip == null || ip.isBlank()) return UNKNOWN_IP;

        try {
            IPAddress address = new IPAddressString(ip.trim()).toAddress();

            if (address.isIPv4Convertible()) {
                address = address.toIPv4();
            }

            if (address.isIPv6()) {
                int prefix = properties.ipv6Prefix() > 0 ? properties.ipv6Prefix() : DEFAULT_IPV6_PREFIX;
                return address.toPrefixBlock(prefix).toCanonicalString();
            }

            return address.toCanonicalString();
        } catch (AddressStringException | IncompatibleAddressException e) {
            return UNKNOWN_IP;
        }
    }
}
