package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Text Entry family in a page that has Ashlar's stylesheet and NO Tailwind, and so no Tailwind Preflight. The parity fixtures load Tailwind utilities (a host app has them), so on
 * their own they cannot see a field that quietly depends on a host utility or a Preflight reset (the control's border and radius, an inherited font, a placeholder at full opacity,
 * a textarea that resizes only vertically, a button's native appearance). This page loads only {@code ashlar.css}.
 *
 * <p>Three kinds of evidence. (1) Absolute, read from the migrated rendering alone: a plain {@code h-4 w-4} box is not 16px (Tailwind is absent), a control is 38px tall with a 1px
 * border and a 6px radius, a textarea resizes vertically unless it is fixed or disabled, the placeholder is at full opacity, the clear button is a transparent borderless native button
 * that is {@code display: none} while hidden, and the icon is 16px. (2) The scoped legacy rendering (only the utilities and the few preflight rules the fields used, see
 * {@code text-entry-legacy-scoped-entry.css}) must equal the FULL legacy rendering, so it is a faithful stand-in for "the legacy fields without Tailwind's page-wide reset". (3) The
 * migrated rendering with Ashlar alone must equal that scoped legacy rendering, field by field, in six configurations that cover both themes and the skins that change a radius, a
 * fill or a shadow.
 *
 * <p>What is not compared, and why: {@code font-family} is not measured at all (both pages pin the same font on a wrapper, the page-wide font is the host's), the style of a zero-width
 * border is not compared (Preflight's blanket {@code border: 0 solid} gives every element an invisible {@code solid} style that the bare page lacks; widths and colours are
 * compared), and the drawing elements inside an {@code svg} are not measured (the {@code svg} itself is). Fully transparent zero-size shadow layers are normalised as in
 * {@link TextEntryParityTest}. Nothing else is excluded.
 *
 * <p>Firefox and Safari are not covered: only Chrome is driven, so Preflight rules that matter only there (the {@code :-moz-ui-invalid} shadow, Safari's placeholder colour guard)
 * are reasoned about in {@code field.css}, not measured.
 */
class TextEntryTailwindFreeTest {

    private static final List<String> CONFIGS = List.of("light/none", "dark/none", "light/ledger", "light/foundry", "dark/axiom", "dark/harbor");

    private static final List<String> FINDINGS_SCOPED_VS_FULL = new ArrayList<>();
    private static final List<String> FINDINGS_FREE_VS_SCOPED = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> FREE = new LinkedHashMap<>();
    private static String controlProbe;
    private static int configurations;

    private static String insertEnvironment(String page, String environment) {
        return page.replace("</head>", "<style>" + environment + "</style></head>");
    }

    private static String stripBorderStyle(String measurement) {
        return measurement.lines().filter(l -> !l.contains("\tborder-top-style\t")).collect(Collectors.joining("\n"));
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("text-entry-tailwind-free");
        List<TextEntryParityTest.Field> fields = TextEntryParityTest.fields().stream().filter(f -> f.pseudo().isEmpty()).toList();
        TextEntryLegacyMarkup textEntry = new TextEntryLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        StringBuilder migratedBody = new StringBuilder();
        fields.forEach(f -> migratedBody.append(TextEntryBrowser.marked(f.markup())));
        String probe = "<div id=\"probe\" class=\"h-4 w-4\"></div>";
        String legacyBody = icons.rewrite(textEntry.rewrite(migratedBody.toString()));
        String environment = ParityFrames.environment(session.generatedDir());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (String id : CONFIGS) {
                ParityConfigs.Config config = ParityConfigs.get(id);
                String full = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("text-entry-oracle.css")), config, "", legacyBody + probe, ""));
                String scoped = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.generated("text-entry-legacy-scoped.css")), config, "", legacyBody + probe, ""), environment));
                String free = server.registerRaw(insertEnvironment(TextEntryBrowser.page(List.of(ParityFrames.ASHLAR), config, "", migratedBody + probe, ""), environment));
                String fullM;
                String scopedM;
                String freeM;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    fullM = TextEntryParityTest.measure(cdp, full, fields, true);
                    scopedM = TextEntryParityTest.measure(cdp, scoped, fields, true);
                    freeM = TextEntryParityTest.measure(cdp, free, fields, true);
                    if (id.equals("light/none")) {
                        controlProbe = cdp.evaluate("(function(){var p=document.getElementById('probe').getBoundingClientRect();return p.width+'x'+p.height})()");
                    }
                }
                configurations++;
                for (TextEntryParityTest.Field f : fields) {
                    String prefix = f.id() + ":";
                    String where = id + " " + f.id() + " (" + f.kind() + " " + f.stateKey() + ")";
                    FINDINGS_SCOPED_VS_FULL.addAll(TextEntryBrowser.differences(where + " scoped-vs-full", stripBorderStyle(slice(fullM, prefix)), stripBorderStyle(slice(scopedM, prefix)), APPLIED));
                    FINDINGS_FREE_VS_SCOPED.addAll(TextEntryBrowser.differences(where + " free-vs-scoped", stripBorderStyle(slice(scopedM, prefix)), stripBorderStyle(slice(freeM, prefix)), APPLIED));
                }
                if (id.equals("light/none")) {
                    for (String line : freeM.split("\n")) {
                        String[] p = line.split("\t", -1);
                        if (p.length == 3) {
                            FREE.put(p[0] + "|" + p[1], p[2]);
                        }
                    }
                }
            }
        }
    }

    private static String slice(String all, String prefix) {
        StringBuilder sb = new StringBuilder();
        for (String line : all.split("\n")) {
            if (line.startsWith(prefix)) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString().stripTrailing();
    }

    private static String free(String field, String tag, String property) {
        return FREE.entrySet().stream().filter(e -> e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property))
                .map(Map.Entry::getValue).findFirst().orElseThrow(() -> new AssertionError("no " + property + " of " + tag + " in " + field));
    }

    @Test
    void tailwindIsReallyAbsentFromThePage() {
        assertNotEquals("16x16", controlProbe, "a plain h-4 w-4 box must NOT be 16px here: " + controlProbe);
    }

    @Test
    void theMigratedFieldsStandOnTheirOwnWithoutTailwind() {
        assertEquals("38px", free("text-1", "input", "height"));
        assertEquals("1px", free("text-1", "input", "border-top-width"));
        assertEquals("6px", free("text-1", "input", "border-top-left-radius"));
        assertEquals("14px", free("text-1", "input", "font-size"));
        assertEquals("20px", free("text-1", "input", "line-height"));
        assertEquals("block", free("text-1", "input", "display"));
        assertEquals("border-box", free("text-1", "input", "box-sizing"));
        assertEquals("0px", free("text-1", "input", "margin-top"));
        assertEquals("0px", free("text-1", "label", "margin-top"));
        assertEquals("8px", free("text-1", "label", "margin-bottom"));
        assertEquals("1", free("text-1", "input", "::placeholder opacity"));
        assertEquals("vertical", free("textarea-1", "textarea", "resize"));
        assertEquals("none", free("x-fixed", "textarea", "resize"));
        assertEquals("none", free("x-fixed-d", "textarea", "resize"));
        assertEquals("none", free("search-1", "button", "display"), "an empty Search Input's clear button is hidden by the attribute, with display: flex declared");
        assertEquals("flex", free("x-sval", "button", "display"));
        assertEquals("button", free("x-sval", "button", "appearance"));
        assertEquals("0px", free("x-sval", "button", "border-top-width"));
        assertEquals("rgba(0, 0, 0, 0)", free("x-sval", "button", "background-color"));
        assertEquals("16px", free("x-lead", "svg", "width"));
        assertEquals("16px", free("x-lead", "svg", "height"));
        assertTrue(FREE.entrySet().stream().anyMatch(e -> e.getKey().startsWith("x-lead:") && e.getKey().contains(":div|max-width") && e.getValue().equals("384px")), "the field box is capped at 24rem");
    }

    @Test
    void theScopedLegacyRenderingEqualsTheFullLegacyRendering() {
        assertEquals(6, configurations);
        assertEquals(List.of(), FINDINGS_SCOPED_VS_FULL.stream().limit(40).toList(), FINDINGS_SCOPED_VS_FULL.size() + " differences");
    }

    @Test
    void theMigratedFieldsWithoutTailwindEqualTheLegacyFieldsWithoutPageWideTailwind() {
        assertEquals(List.of(), FINDINGS_FREE_VS_SCOPED.stream().limit(40).toList(), FINDINGS_FREE_VS_SCOPED.size() + " differences");
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalised() {
        assertEquals(List.of("invisible-shadow-layers"), List.copyOf(APPLIED.keySet()), APPLIED.toString());
        assertTrue(APPLIED.get("invisible-shadow-layers") > 100, APPLIED.toString());
    }
}
