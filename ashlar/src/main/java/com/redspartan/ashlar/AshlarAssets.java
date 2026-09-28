package com.redspartan.ashlar;

/**
 * Builds the URLs for Ashlar's own static assets - the per-component scripts
 * and stylesheets that ship inside the Ashlar jar under
 * {@code META-INF/resources/ashlar/}.
 *
 * <p>Ashlar's components emit their own {@code <script>} tag (see
 * {@code ashlar/components/button.jte}), which means the template has to name
 * a URL. Hardcoding a root-relative {@code /ashlar/js/button.js} would be
 * wrong for any application deployed under a servlet context path: a consumer
 * mounted at {@code /myapp} needs {@code /myapp/ashlar/js/button.js}, and a
 * template has no way to discover that on its own.
 *
 * <p>A context path is fixed for the life of a deployment, so the smallest
 * mechanism that solves this correctly is a single configured prefix set once
 * at startup, rather than threading a request-scoped value through every
 * template that might contain a component. Consumers running at the root need
 * to do nothing; the default already matches where the jar's resources are
 * served from.
 *
 * <p>Deliberately not a Spring bean and deliberately not an auto-configuration:
 * Ashlar's only hard dependency is JTE, and a component library should not
 * require a particular application framework in order to emit a URL. A Spring
 * consumer sets this from {@code server.servlet.context-path} in one line of
 * its own startup code; see the showcase module for that.
 */
public final class AshlarAssets {

    /** Matches the jar layout: {@code META-INF/resources/ashlar/**}. */
    private static final String DEFAULT_BASE_PATH = "/ashlar";

    private static volatile String basePath = DEFAULT_BASE_PATH;

    private AshlarAssets() {
    }

    /**
     * Sets the prefix every Ashlar asset URL is built from. Pass the
     * application's context path joined to Ashlar's own resource root - for an
     * application at {@code /myapp} that is {@code /myapp/ashlar}.
     *
     * <p>A null or blank value restores the default. A trailing slash is
     * removed and a leading slash is added if missing, so callers can pass
     * whatever shape their framework hands them without pre-cleaning it.
     */
    public static void basePath(String path) {
        if (path == null || path.isBlank()) {
            basePath = DEFAULT_BASE_PATH;
            return;
        }
        String normalized = path.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        basePath = normalized;
    }

    /** The prefix currently in use. Defaults to {@code /ashlar}. */
    public static String basePath() {
        return basePath;
    }

    /** URL for a script shipped at {@code META-INF/resources/ashlar/js/}. */
    public static String js(String fileName) {
        return basePath + "/js/" + fileName;
    }

    /** URL for a stylesheet shipped at {@code META-INF/resources/ashlar/css/}. */
    public static String css(String fileName) {
        return basePath + "/css/" + fileName;
    }
}
