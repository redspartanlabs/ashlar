package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Accordion chevron in a page that has Ashlar's stylesheet and NO Tailwind. The parity fixtures all load Tailwind utilities (a host app has them), so they cannot see
 * an Accordion that quietly depends on a host utility such as {@code h-4 w-4}. This page loads only {@code ashlar.css}.
 *
 * <p>The chevron's classes are read from the shipped {@code accordion-item.jte} (the argument it passes to the Icon component), and the Icon component writes
 * {@code ash-icon} before the caller's classes, so the svg built here has exactly the class value the template produces. It must be {@code ash-icon ash-icon--sm} and
 * compute to 16 by 16px. If the template went back to {@code h-4 w-4} the svg would be {@code ash-icon h-4 w-4}, which is 20 by 20px without Tailwind: the control
 * svg on the same page shows that, and so proves the page really has no Tailwind.
 */
class AccordionTailwindFreeTest {

    private static final Pattern CHEVRON_CLASSES = Pattern.compile("icon\\(\\s*icon\\s*=\\s*Icon\\.CHEVRON_RIGHT\\s*,\\s*classes\\s*=\\s*\"([^\"]*)\"");

    private static final Map<String, String[]> RESULTS = new LinkedHashMap<>();
    private static String templateClasses;

    private static String svg(String id, String classValue) {
        return "<svg id=\"" + id + "\" class=\"" + classValue + "\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" aria-hidden=\"true\"><path d=\"M9 5l7 7-7 7\"/></svg>";
    }

    private static String chevron(String id, String classValue) {
        return "<div class=\"ash-accordion\"><div class=\"ash-accordion__item\"><h3 class=\"ash-accordion__heading\"><button type=\"button\" aria-expanded=\"false\" class=\"ash-accordion__button\">"
                + "<span class=\"ash-accordion__title\">T</span><span data-accordion-chevron class=\"ash-accordion__chevron\">" + svg(id, classValue) + "</span></button></h3></div></div>";
    }

    private static final String SCRIPT = """
            (function () {
              var out = [];
              function size(id) { var e = document.getElementById(id); var s = getComputedStyle(e); var r = e.getBoundingClientRect();
                out.push([id, s.width, s.height, r.width, r.height].join('\\t')); }
              try {
                size('template'); size('control'); size('probe');
                out.push('sheets\\t' + [].map.call(document.styleSheets, function (s) { return s.href || 'inline'; }).join(','));
              } catch (e) { out.push('SCRIPT ERROR\\t' + e); }
              var pre = document.createElement('pre'); pre.id = 'ash-accordion-tailwind-free'; pre.textContent = out.join('\\n'); document.body.appendChild(pre);
            })();
            """;

    @BeforeAll
    static void run() throws Exception {
        String template;
        try (InputStream in = AccordionTailwindFreeTest.class.getResourceAsStream("/ashlar/components/accordion-item.jte")) {
            if (in == null) {
                throw new IOException("accordion-item.jte is not on the test classpath");
            }
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = CHEVRON_CLASSES.matcher(template);
        if (!m.find()) {
            throw new IllegalStateException("accordion-item.jte no longer passes classes to the chevron Icon in the form this test reads");
        }
        templateClasses = m.group(1);

        ParitySession session = ParitySession.open("accordion-tailwind-free");
        String page = "<!doctype html><html data-theme=\"light\"><head><link rel=\"stylesheet\" href=\"" + ParityFrames.ASHLAR + "\"><style>*,*::before,*::after{transition:none !important}</style></head>"
                + "<body style=\"margin:8px\">" + chevron("template", "ash-icon " + templateClasses) + chevron("control", "ash-icon h-4 w-4")
                + "<div id=\"probe\" class=\"h-4 w-4\"></div><script>" + SCRIPT.replace("</script", "<\\/script") + "</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String dom = session.chrome().dumpDom(server.registerRaw(page));
            for (String line : ParityProbe.lines(dom, "ash-accordion-tailwind-free")) {
                String[] f = line.split("\t", -1);
                RESULTS.put(f[0], f);
            }
        }
    }

    @Test
    void theTemplatePassesTheSmallIconModifierAndNoTailwindSize() {
        assertEquals(List.of("ash-icon--sm"), List.of(templateClasses.trim().split("\\s+")), "the chevron's classes in accordion-item.jte");
    }

    @Test
    void thePageHasAshlarsStylesheetAndNoTailwind() {
        assertTrue(!RESULTS.containsKey("SCRIPT ERROR"), () -> String.join(" | ", RESULTS.get("SCRIPT ERROR")));
        String sheets = RESULTS.get("sheets")[1];
        // The page's own inline style (transitions off) is the only other sheet; the one linked sheet is Ashlar's.
        assertEquals(List.of("inline"), java.util.Arrays.stream(sheets.split(",")).filter(s -> !s.endsWith("/ashlar-real/ashlar.css")).toList(), "only Ashlar's stylesheet is linked: " + sheets);
        assertTrue(sheets.contains("/ashlar-real/ashlar.css"), sheets);
        // h-4 w-4 on a plain div does nothing here: the width fills the line and the height is empty content, not 16px.
        assertTrue(!RESULTS.get("probe")[2].equals("16px") && !RESULTS.get("probe")[1].equals("16px"), "no Tailwind utility reached the page: " + String.join(" | ", RESULTS.get("probe")));
    }

    @Test
    void withoutTailwindTheChevronIsStill16By16() {
        String[] r = RESULTS.get("template");
        assertEquals("16px", r[1], "computed width " + String.join(" | ", r));
        assertEquals("16px", r[2], "computed height " + String.join(" | ", r));
        assertEquals("16", r[3], "rendered width " + String.join(" | ", r));
        assertEquals("16", r[4], "rendered height " + String.join(" | ", r));
    }

    @Test
    void theControlWithTheOldTailwindSizeIsTwentyWithoutTailwind() {
        String[] r = RESULTS.get("control");
        assertEquals("20px", r[1], "ash-icon h-4 w-4 falls back to the Icon default when nothing supplies h-4 w-4: " + String.join(" | ", r));
        assertEquals("20px", r[2], String.join(" | ", r));
    }
}
