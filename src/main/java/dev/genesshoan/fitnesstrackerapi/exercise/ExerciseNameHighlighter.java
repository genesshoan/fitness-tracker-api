package dev.genesshoan.fitnesstrackerapi.exercise;

import java.util.Locale;

/**
 * Escapes exercise names and highlights literal, case-insensitive query matches.
 */
public final class ExerciseNameHighlighter {

    private ExerciseNameHighlighter() {}

    /**
     * Wraps every literal occurrence of {@code query} in bold tags.
     *
     * @param name exercise name
     * @param query search query
     * @return escaped name with matching fragments highlighted
     */
    public static String highlight(String name, String query) {
        if (name == null) {
            return null;
        }
        if (query == null || query.isBlank()) {
            return escapeHtml(name);
        }

        String lowerName = name.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        StringBuilder highlighted = new StringBuilder();
        int from = 0;
        int match;

        while ((match = lowerName.indexOf(lowerQuery, from)) >= 0) {
            highlighted.append(escapeHtml(name.substring(from, match)));
            highlighted
                    .append("<b>")
                    .append(escapeHtml(name.substring(match, match + query.length())))
                    .append("</b>");
            from = match + query.length();
        }

        return from == 0
                ? escapeHtml(name)
                : highlighted.append(escapeHtml(name.substring(from))).toString();
    }

    private static String escapeHtml(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            escaped.append(
                    switch (text.charAt(i)) {
                        case '&' -> "&amp;";
                        case '<' -> "&lt;";
                        case '>' -> "&gt;";
                        case '"' -> "&quot;";
                        case '\'' -> "&#39;";
                        default -> text.charAt(i);
                    });
        }
        return escaped.toString();
    }
}
