package dev.genesshoan.fitnesstrackerapi.ratelimit.ip;

import java.util.Map;

import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.config.RateLimitProperties;
import dev.genesshoan.fitnesstrackerapi.ratelimit.infrastructure.web.ip.IpNormalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests - IP Normalizer")
class IpNormalizerTest {

    private final IpNormalizer normalizer = normalizerWithPrefix(64);

    @Test
    @DisplayName("keeps IPv4 addresses as a single host")
    void keepsIpv4() {
        assertThat(normalizer.normalize("203.0.113.7")).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("collapses IPv4-mapped IPv6 into the embedded IPv4 address")
    void collapsesIpv4Mapped() {
        assertThat(normalizer.normalize("::ffff:203.0.113.7")).isEqualTo("203.0.113.7");
        assertThat(normalizer.normalize("::FFFF:203.0.113.7")).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("groups IPv6 addresses by prefix so clients can rotate the host part")
    void groupsIpv6ByPrefix() {
        assertThat(normalizer.normalize("2001:db8:1:2:3:4:5:6")).isEqualTo("2001:db8:1:2::/64");
        assertThat(normalizer.normalize("2001:db8:1:2:ffff:ffff:ffff:ffff")).isEqualTo("2001:db8:1:2::/64");
    }

    @Test
    @DisplayName("honours the configured IPv6 prefix")
    void honoursConfiguredPrefix() {
        IpNormalizer normalizer48 = normalizerWithPrefix(48);

        assertThat(normalizer48.normalize("2001:db8:1:2::1")).isEqualTo("2001:db8:1::/48");
    }

    @Test
    @DisplayName("trims surrounding whitespace")
    void trimsWhitespace() {
        assertThat(normalizer.normalize("  203.0.113.7  ")).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("uses a shared fallback for malformed values instead of using them as a key")
    void usesFallbackForMalformedValues() {
        assertThat(normalizer.normalize("not-an-ip")).isEqualTo("unknown");
        assertThat(normalizer.normalize("203.0.113.7:8080")).isEqualTo("unknown");
        assertThat(normalizer.normalize("::ffff:203.0.113.7:8080")).isEqualTo("unknown");
    }

    @Test
    @DisplayName("uses the fallback for null and blank values")
    void usesFallbackForNullAndBlank() {
        assertThat(normalizer.normalize(null)).isEqualTo("unknown");
        assertThat(normalizer.normalize("   ")).isEqualTo("unknown");
    }

    private static IpNormalizer normalizerWithPrefix(int prefix) {
        return new IpNormalizer(new RateLimitProperties(Map.of(), prefix));
    }
}
