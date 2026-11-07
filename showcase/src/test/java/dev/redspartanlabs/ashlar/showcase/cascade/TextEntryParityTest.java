package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visual parity of the Text Entry family with Tailwind PRESENT, in all 22 configurations (two themes, no skin and the ten skins): the legacy rendering (the migrated markup rewritten
 * to the Tailwind classes the templates used to write, with the cumulative Tailwind oracle) against the migrated rendering (the real application stylesheet, Tailwind and Ashlar
 * together). Every element of every field is measured for {@link TextEntryBrowser#PROPERTIES}, its box, and (for an input or textarea) its placeholder, in every state the family has:
 * resting, hover, focus, both, disabled, read-only, invalid, their overlaps, placeholder, filled, required, help and error text, icons, the character count below, at and without a
 * limit, a fixed textarea, the clear button with and without a value, and the Form Field.
 *
 * <p>Hover and focus are forced on the real control with the browser's own pseudo-state API, on a browser that is stated to be able to hover. The comparison is EXACT except for one
 * documented normalisation (decision D11, {@link TextEntryBrowser#visibleShadow}): a box-shadow's fully transparent, zero-size layers are not compared, because Tailwind composes a shadow
 * from unused layers and the migrated component writes only what it draws. Every visible layer is compared exactly, and the suite pins how often that was needed. The legacy quirks
 * (a hovered disabled field showing the read-only hover border, dark hover winning over dark focus) are in the legacy rendering, so they are compared like everything else.
 *
 * <p>The input modality of a browser changes what {@code :focus-visible} matches, but nothing here uses it: the field uses {@code :focus}. Behaviour that depends on trusted input is
 * {@link TextEntryBehaviourTest}, which gives each rendering its own browser.
 */
class TextEntryParityTest {

    private static final List<String> FINDINGS = new ArrayList<>();
    private static final Map<String, Integer> APPLIED = new LinkedHashMap<>();
    private static final Map<String, String> LEGACY_LIGHT_NONE = new LinkedHashMap<>();
    private static int configurations;
    private static int measuredFields;
    private static int measuredValues;

    /** One field and, if any, the pseudo-classes forced while it is measured: on its control, or on {@code target} (a selector) when the state belongs to another element, such as the clear button. */
    record Field(String id, String kind, String stateKey, List<String> pseudo, String markup, String target) {
        Field(String id, String kind, String stateKey, List<String> pseudo, String markup) {
            this(id, kind, stateKey, pseudo, markup, "#" + id);
        }
    }

    static List<Field> fields() {
        List<Field> fields = new ArrayList<>();
        for (String kind : TextEntryBrowser.KINDS) {
            int i = 0;
            for (TextEntryBrowser.State state : TextEntryBrowser.STATES) {
                i++;
                if (kind.equals("search") && (state.options().containsKey("readonly"))) {
                    continue; // the Search Input has no read-only parameter
                }
                Map<String, Object> options = new LinkedHashMap<>(state.options());
                options.put("id", kind + "-" + i);
                fields.add(new Field(kind + "-" + i, kind, state.key(), state.pseudo(), TextEntryBrowser.build(kind, options)));
            }
        }
        for (Map.Entry<String, String> extra : TextEntryBrowser.EXTRAS) {
            String id = extra.getValue().replaceFirst("(?s).*?data-field=\"([^\"]+)\".*", "$1");
            fields.add(new Field(id, "extra", extra.getKey(), List.of(), extra.getValue()));
        }
        // The clear button's own states: it is a separate element from the control, with its own hover colour and focus ring.
        for (String pseudo : List.of("hover", "focus", "focus-visible")) {
            {
                String id = "x-clear-" + pseudo;
                fields.add(new Field(id, "extra", "search clear button " + pseudo, List.of(pseudo), TextEntryBrowser.search(TextEntryBrowser.o("id", id, "value", "query")), "[data-field=\"" + id + "\"] [data-search-input-clear]"));
            }
        }
        return fields;
    }

    static String measure(DevToolsSession cdp, String url, List<Field> fields) throws Exception {
        return measure(cdp, url, fields, false);
    }

    /** {@code skipRaw}: leave out the caller's own content (marked raw), which a Tailwind-free comparison must not hold to either side's page-wide reset. */
    static String measure(DevToolsSession cdp, String url, List<Field> fields, boolean skipRaw) throws Exception {
        cdp.navigate(url);
        String sheets = cdp.evaluate("(function(){var ok=[].every.call(document.styleSheets,function(s){try{return s.cssRules.length>0}catch(e){return false}});"
                + "return document.styleSheets.length+':'+ok})()");
        assertTrue(sheets.endsWith(":true") && !sheets.startsWith("0:"), "every stylesheet must have loaded - infrastructure failure, not a parity result: " + sheets);
        StringBuilder all = new StringBuilder();
        for (Field f : fields) {
            List<Integer> nodes = f.pseudo().isEmpty() ? List.of() : cdp.nodes(f.target());
            if (!f.pseudo().isEmpty()) {
                cdp.force(nodes, f.pseudo());
            }
            all.append(cdp.evaluate(TextEntryBrowser.measureScript(f.id(), skipRaw))).append('\n');
            if (!f.pseudo().isEmpty()) {
                cdp.force(nodes, List.of());
            }
        }
        return all.toString();
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("text-entry-parity");
        List<Field> fields = fields();
        TextEntryLegacyMarkup textEntry = new TextEntryLegacyMarkup();
        IconLegacyMarkup icons = new IconLegacyMarkup();
        StringBuilder migratedBody = new StringBuilder();
        fields.forEach(f -> migratedBody.append(TextEntryBrowser.marked(f.markup())));
        String legacyBody = icons.rewrite(textEntry.rewrite(migratedBody.toString()));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("text-entry-oracle.css")), config, "", legacyBody, ""));
                String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", migratedBody.toString(), ""));
                String legacy;
                String migrated;
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    legacy = measure(cdp, legacyUrl, fields);
                    migrated = measure(cdp, migratedUrl, fields);
                }
                configurations++;
                measuredFields += fields.size();
                measuredValues += legacy.split("\n").length;
                if (config.id().equals("light/none")) {
                    for (String line : legacy.split("\n")) {
                        String[] p = line.split("\t", -1);
                        if (p.length == 3) {
                            LEGACY_LIGHT_NONE.put(p[0] + "|" + p[1], p[2]);
                        }
                    }
                }
                // Compare field by field, so a failure names the field and its state.
                for (Field f : fields) {
                    String prefix = f.id() + ":";
                    FINDINGS.addAll(TextEntryBrowser.differences(config.id() + " " + f.id() + " (" + f.kind() + " " + f.stateKey() + ")", slice(legacy, prefix), slice(migrated, prefix), APPLIED));
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

    private static String legacy(String field, String tag, String property) {
        return LEGACY_LIGHT_NONE.entrySet().stream().filter(e -> e.getKey().startsWith(field + ":") && e.getKey().contains(":" + tag + "|") && e.getKey().endsWith("|" + property))
                .map(Map.Entry::getValue).findFirst().orElseThrow(() -> new AssertionError("no legacy " + property + " of " + tag + " in " + field));
    }

    @Test
    void theMigratedFieldsAreIdenticalToTheLegacyOnesInAll22Configurations() {
        assertEquals(22, configurations);
        assertEquals(List.of(), FINDINGS.stream().limit(60).toList(), FINDINGS.size() + " differences");
    }

    @Test
    void theMeasurementCoversEveryFieldStateAndIsLargeEnoughToMeanSomething() {
        assertTrue(measuredFields >= 22 * 90, "fields measured: " + measuredFields);
        assertTrue(measuredValues >= 22 * 4000, "values measured per rendering: " + measuredValues);
    }

    @Test
    void theLegacyOracleShowsTheStatesTheComparisonClaimsToCover() {
        // text-1 default .. see STATES order: default(1) hover(2) focus(3) focus+hover(4) disabled(5) disabled+hover(6) readonly(7) ... invalid(12)
        String rest = legacy("text-1", "input", "border-top-color");
        assertNotEquals(rest, legacy("text-2", "input", "border-top-color"), "hover changes the border");
        assertNotEquals(rest, legacy("text-3", "input", "border-top-color"), "focus changes the border");
        assertTrue(legacy("text-3", "input", "box-shadow").contains("2px"), "focus draws a 2px ring: " + legacy("text-3", "input", "box-shadow"));
        assertEquals("not-allowed", legacy("text-5", "input", "cursor"));
        assertEquals("default", legacy("text-7", "input", "cursor"));
        assertNotEquals(rest, legacy("text-12", "input", "border-top-color"), "aria-invalid changes the border");
        assertEquals("vertical", legacy("textarea-1", "textarea", "resize"));
        assertEquals("none", legacy("x-fixed", "textarea", "resize"));
        assertEquals("flex", legacy("x-sval", "button", "display"), "the clear button shows with a value");
        assertEquals("none", legacy("search-1", "button", "display"), "and hides without one");
        assertNotEquals(legacy("x-count", "span", "color"), legacy("x-limit", "span", "color"), "the count turns red at its limit");
        assertTrue(legacy("text-1", "input", "border-top-width").equals("1px"));
        assertNotEquals(legacy("x-sval", "button", "color"), legacy("x-clear-hover", "button", "color"), "hover darkens the clear button");
        assertTrue(legacy("x-clear-focus-visible", "button", "box-shadow").contains("2px"), "keyboard focus draws the clear button's ring: " + legacy("x-clear-focus-visible", "button", "box-shadow"));
        assertFalse(legacy("x-clear-focus", "button", "box-shadow").contains("0px 0px 0px 2px"), "a plain :focus (a mouse click) does not draw it");
    }

    @Test
    void theLegacyQuirksAreInTheOracleSoTheyAreComparedNotCorrected() {
        // A hovered disabled field shows the READ-ONLY hover border (slate-300), because :read-only also matches a disabled control; plain hover is slate-400.
        assertEquals(legacy("text-1", "input", "border-top-color"), legacy("text-6", "input", "border-top-color"), "disabled+hover shows the resting border colour");
        assertNotEquals(legacy("text-5", "input", "border-top-color"), legacy("text-6", "input", "border-top-color"), "and not the disabled one");
        // A read-only field does not darken on hover.
        assertEquals(legacy("text-7", "input", "border-top-color"), legacy("text-8", "input", "border-top-color"));
    }

    @Test
    void onlyInvisibleShadowLayersWereNormalisedAndTheNormalisationWasActuallyNeeded() {
        assertEquals(List.of("invisible-shadow-layers"), List.copyOf(APPLIED.keySet()), "no other normalisation exists: " + APPLIED);
        assertTrue(APPLIED.get("invisible-shadow-layers") > 1000, "Tailwind's unused transparent layers are what the migrated rule leaves out: " + APPLIED);
    }

    @Test
    void theShadowNormalisationRemovesOnlyTransparentZeroLayers() {
        assertEquals("none", TextEntryBrowser.visibleShadow("rgba(0, 0, 0, 0) 0px 0px 0px 0px, rgba(0, 0, 0, 0) 0px 0px 0px 0px"));
        String visible = "oklch(0.623 0.214 259.815) 0px 0px 0px 2px, rgba(0, 0, 0, 0.1) 0px 1px 3px 0px";
        assertEquals(visible, TextEntryBrowser.visibleShadow("rgba(0, 0, 0, 0) 0px 0px 0px 0px, " + visible));
        assertNotEquals(TextEntryBrowser.visibleShadow("rgba(0, 0, 0, 0.1) 0px 1px 3px 0px"), TextEntryBrowser.visibleShadow("rgba(0, 0, 0, 0.1) 0px 1px 2px 0px"));
        assertNotEquals("none", TextEntryBrowser.visibleShadow("rgba(0, 0, 0, 0) 0px 1px 0px 0px"), "a transparent layer with an offset is not provably invisible");
        assertFalse(TextEntryBrowser.visibleShadow("rgba(0, 0, 0, 0.3) 0px 0px 0px 0px").equals("none"), "a visible colour with zero size is kept");
    }
}
