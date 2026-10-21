package dev.genesshoan.fitnesstrackerapi.notification.application;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import dev.genesshoan.fitnesstrackerapi.notification.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.thymeleaf.context.Context;

/**
 * Builds the Thymeleaf context shared by every notification email.
 *
 * <p>All notification templates rely on the same branding variables, so they are
 * resolved in a single place to keep the rendered emails consistent.
 */
@Component
@RequiredArgsConstructor
public class EmailContextFactory {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final AppProperties appProperties;

    /**
     * Creates a context pre-populated with the variables shared by every template.
     *
     * @return the base template context
     */
    public Context create() {
        Context ctx = new Context();
        ctx.setVariable("appName", appProperties.name());

        String logo = appProperties.mail().logoUrl();
        ctx.setVariable("logoUrl", (logo == null || logo.isBlank()) ? null : logo);

        return ctx;
    }

    /**
     * Builds an absolute URL pointing to the frontend application.
     *
     * @param path the frontend path, including any query string
     * @return the absolute frontend URL
     */
    public String frontendUrl(String path) {
        return appProperties.mail().frontendUrl() + path;
    }

    /**
     * Formats an instant for display in an email template.
     *
     * @param instant the instant to format
     * @return the timestamp rendered in UTC
     */
    public String formatTimestamp(Instant instant) {
        return TIMESTAMP_FORMAT.format(instant);
    }
}
