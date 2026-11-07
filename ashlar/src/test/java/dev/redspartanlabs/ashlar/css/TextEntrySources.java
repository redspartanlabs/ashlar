package dev.redspartanlabs.ashlar.css;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** What the Text Entry contract tests read: the module's sources, a template reduced to its markup, and a stylesheet's rules. They read sources only; the rendered result is measured in the showcase. */
final class TextEntrySources {

    static final String DARK = ":where([data-theme=\"dark\"], [data-theme=\"dark\"] *)";

    private static Path module;

    private TextEntrySources() {
    }

    static Path module() {
        if (module == null) {
            String generated = System.getProperty("ashlar.css.generated");
            assertNotNull(generated, "ashlar.css.generated must be set by the Gradle test task");
            for (Path p = Path.of(generated).toAbsolutePath(); p != null; p = p.getParent()) {
                if (Files.isRegularFile(p.resolve("src/main/css/ashlar.css"))) {
                    module = p;
                    break;
                }
            }
            assertNotNull(module, "could not find the ashlar module above " + generated);
        }
        return module;
    }

    static String read(String relative) throws IOException {
        return Files.readString(module().resolve(relative), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    static String template(String name) throws IOException {
        return read("src/main/jte/ashlar/components/" + name + ".jte");
    }

    static String css(String name) throws IOException {
        return read("src/main/css/components/" + name + ".css");
    }

    static String script(String name) throws IOException {
        return read("src/main/resources/META-INF/resources/ashlar/js/" + name + ".js");
    }

    /** The template without its comments, parameter lines and local-variable lines, on one line. */
    static String markup(String name) throws IOException {
        return template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ").trim();
    }

    /** The template's local variables, comments removed. */
    static String locals(String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("(?m)^!\\{.*\\}$").matcher(template(name).replaceAll("(?s)<%--.*?--%>", ""));
        while (m.find()) {
            sb.append(m.group()).append('\n');
        }
        return sb.toString();
    }

    static List<String> declared(String name) throws IOException {
        List<String> declared = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^@(?:param|import) .*$").matcher(template(name));
        while (m.find()) {
            declared.add(m.group());
        }
        return declared;
    }

    static List<CssScanner.Rule> rules(String cssName) throws IOException {
        return CssScanner.flatRules(css(cssName).replaceAll("(?s)/\\*.*?\\*/", ""));
    }

    static Map<String, String> declarations(String cssName, String selector) throws IOException {
        return declarations(cssName, selector, "");
    }

    static Map<String, String> declarations(String cssName, String selector, String media) throws IOException {
        List<CssScanner.Rule> found = rules(cssName).stream().filter(r -> r.selector().equals(selector) && r.media().equals(media)).toList();
        assertEquals(1, found.size(), selector + " must be defined exactly once in " + cssName + (media.isEmpty() ? "" : " inside " + media) + "; rules: "
                + rules(cssName).stream().map(r -> r.media() + "|" + r.selector()).toList());
        return CssScanner.declarations(found.get(0).block());
    }

    private static final List<String> UTILITIES = List.of("flex", "grid", "gap-", "border", "px-", "py-", "pl-", "pr-", "text-", "font-", "bg-", "dark:", "hover:", "focus:", "ring", "rounded", "overflow",
            "truncate", "h-4", "w-4", "w-full", "max-w", "slate-", "blue-", "red-", "transition", "shadow", "cursor-", "items-", "justify-", "absolute", "relative", "inset-", "pointer-events", "resize",
            "shrink", "mb-", "mt-", "mr-", "ml-", "block", "disabled:", "read-only:", "user-invalid:", "aria-invalid:", "placeholder:", "appearance");

    /** No Tailwind utility class may remain in the template's markup or class expressions (comments, parameters and imports are ignored). */
    static void assertNoTailwindUtility(String name) throws IOException {
        String source = template(name).replaceAll("(?s)<%--.*?--%>", "").replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("AshlarAssets\\.js\\(\"[^\"]*\"\\)", "");
        for (String utility : UTILITIES) {
            assertFalse(Pattern.compile("class=\"[^\"]*(?<![\\w-])" + Pattern.quote(utility)).matcher(source).find() || Pattern.compile("\"[^\"]*(?<![\\w-])" + Pattern.quote(utility) + "[^\"]*\"\\s*[+;)]").matcher(source).find(),
                    name + ": a Tailwind utility is back: " + utility);
        }
    }

    /** No presentation survives in a class expression: only ash- classes may be named in the template's quoted class strings. */
    static void assertOnlyAshClasses(String name) throws IOException {
        String source = template(name).replaceAll("(?s)<%--.*?--%>", "");
        Matcher m = Pattern.compile("class=\"([^\"$]*)\"").matcher(source);
        while (m.find()) {
            for (String token : m.group(1).trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    assertEquals(true, token.startsWith("ash-"), name + ": class token that is not Ashlar's: " + token);
                }
            }
        }
        Matcher e = Pattern.compile("!\\{String \\w*[cC]lasses = ([^;]*);\\}").matcher(source);
        while (e.find()) {
            Matcher q = Pattern.compile("\"([^\"]*)\"").matcher(e.group(1));
            while (q.find()) {
                for (String token : q.group(1).trim().split("\\s+")) {
                    if (!token.isEmpty()) {
                        assertEquals(true, token.startsWith("ash-"), name + ": class token that is not Ashlar's in a class expression: " + token);
                    }
                }
            }
        }
    }
}
