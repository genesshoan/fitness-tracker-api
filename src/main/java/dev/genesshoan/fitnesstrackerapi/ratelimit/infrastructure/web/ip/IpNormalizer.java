package dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import lombok.RequiredArgsConstructor;

/**
 * Normalizes client addresses before they are used as rate limit bucket keys.
 *
 * <p>Normalization prevents trivial bypasses:
 * <ul>
 * <li>IPv4-mapped IPv6 values ({@code ::ffff:a.b.c.d}) are collapsed to their
 * IPv4 form, so the same client is not split across two keys.</li>
 * <li>IPv6 addresses are reduced to their {@code /prefix} network (default
 * {@code /64}), so rotating the host part of the address does not reset the
 * limit.</li>
 * <li>Missing, blank or malformed values map to a shared {@code unknown} key
 * instead of being used verbatim, so they cannot be used to create arbitrarily
 * many buckets.</li>
 * </ul>
 *
 * @see RateLimitProperties#ipv6Prefix()
 */
@Component
@RequiredArgsConstructor
public class IpNormalizer {

    private static final int DEFAULT_IPV6_PREFIX = 64;
    private static final String UNKNOWN_IP = "unknown";

    private final RateLimitProperties properties;

    /**
     * Normalizes an address into a stable bucket key.
     *
     * @param ip raw address, possibly {@code null}, blank or malformed
     * @return the canonical IPv4 address, the canonical IPv6 prefix block, or the
     *     shared {@code unknown} fallback
     */
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
