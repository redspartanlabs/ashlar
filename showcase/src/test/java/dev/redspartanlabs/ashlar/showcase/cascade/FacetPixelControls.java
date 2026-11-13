package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.List;
import java.util.Map;

/**
 * Negative controls for {@link FacetPixelTest}: renderings that are broken in one specific, realistic way, which the exact comparison must tell apart from the real one. Each control takes
 * a field, takes its picture with the real application stylesheet, takes it again with one rule added that breaks one thing, and the two pictures must differ. A comparison that could not
 * see a thinner ring, a missing hover fill or a label that no longer wraps would pass every real field and prove nothing.
 *
 * <p>The broken rules address the markup by its structure and its {@code data-*} hooks (never by a Facets class), so the same controls work whatever the classes are called. Every one
 * carries {@code !important} so it wins over any layer.
 */
final class FacetPixelControls {

    private FacetPixelControls() {
    }

    private static final String RED = "rgb(220, 38, 38)";

    /** A control: its name, the fixture it is applied to, the configuration, and the rule that breaks the rendering. */
    record Control(String key, String fixture, String config, String css) {
    }

    static final List<Control> CONTROLS = List.of(
            new Control("checkbox accent colour", "counts and a checked option", "light/none", "[data-facet] input{accent-color:" + RED + " !important}"),
            new Control("label colour", "single facet", "light/none", "[data-facet] label > span:first-of-type{color:" + RED + " !important}"),
            new Control("label colour in dark", "single facet", "dark/none", "[data-facet] label > span:first-of-type{color:" + RED + " !important}"),
            new Control("label size", "single facet", "light/none", "[data-facet] label{font-size:15px !important}"),
            new Control("legend weight", "single facet", "light/none", "[data-facet] legend{font-weight:400 !important}"),
            new Control("legend colour in dark", "single facet", "dark/none", "[data-facet] legend{color:" + RED + " !important}"),
            new Control("legend margin", "single facet", "light/none", "[data-facet] legend{margin-bottom:7px !important}"),
            new Control("row padding", "single facet", "light/none", "[data-facet] label{padding-left:9px !important}"),
            new Control("gap inside a row", "counts and a checked option", "light/none", "[data-facet] label{column-gap:11px !important}"),
            new Control("count size", "counts and a checked option", "light/none", "[data-facet] label > span[aria-hidden]{font-size:13px !important}"),
            new Control("count colour", "counts and a checked option", "light/none", "[data-facet] label > span[aria-hidden]{color:" + RED + " !important}"),
            new Control("gap between facets", "several facets", "light/none", "[data-faceted-search]{row-gap:21px !important}"),
            new Control("disabled dimming", "disabled", "light/none", "[data-facet] label:has(> input:disabled){opacity:0.7 !important}"),
            new Control("disabled row not dimmed at all", "disabled", "dark/none", "[data-facet] label:has(> input:disabled){opacity:1 !important}"),
            new Control("checkbox size", "single facet", "light/none", "[data-facet] input{width:17px !important;height:17px !important}"),
            new Control("hover fill missing", "hover on an option", "light/none", "[data-facet] label:hover{background-color:transparent !important}"),
            new Control("hover fill a shade off", "hover on an option", "light/none", "[data-facet] label:hover{background-color:rgb(235, 240, 246) !important}"),
            new Control("hover fill in dark", "hover on an option", "dark/none", "[data-facet] label:hover{background-color:rgb(51, 65, 86) !important}"),
            new Control("row corners square", "hover on an option", "light/none", "[data-facet] label{border-radius:0 !important}"),
            new Control("row corners rounder", "hover on an option", "light/none", "[data-facet] label{border-radius:12px !important}"),
            new Control("hover fill on a disabled row", "hover on a disabled option", "light/none", "[data-facet] label:hover{background-color:rgb(241, 245, 249) !important}"),
            new Control("focus ring missing", "focus-visible on an option", "light/none", "[data-facet] input:focus-visible{box-shadow:none !important}"),
            new Control("focus ring thinner", "focus-visible on an option", "light/none", "[data-facet] input:focus-visible{box-shadow:0 0 0 2px #fff, 0 0 0 3px rgb(59, 130, 246) !important}"),
            new Control("focus ring colour", "focus-visible on an option", "light/none", "[data-facet] input:focus-visible{box-shadow:0 0 0 2px #fff, 0 0 0 4px " + RED + " !important}"),
            new Control("focus ring without the white offset", "focus-visible on a checked option", "light/none", "[data-facet] input:focus-visible{box-shadow:0 0 0 4px rgb(59, 130, 246) !important}"),
            new Control("long label no longer breaks", "long label without a break", "light/none", "[data-facet] label > span:first-of-type{overflow-wrap:normal !important}"),
            new Control("long label no longer shrinks", "long label that wraps", "light/none", "[data-facet] label > span:first-of-type{min-width:auto !important;flex:none !important}"));

    /** Runs every control; the result of each is {@code rejected} (the pictures differ, as they must) or {@code SAME PICTURE}. */
    static void run(ParitySession session, CascadeFixtureServer server, Map<String, String> results) throws Exception {
        List<FacetParityTest.Field> fields = FacetParityTest.fields();
        for (Control c : CONTROLS) {
            FacetParityTest.Field field = fields.stream().filter(f -> f.spec().key().equals(c.fixture())).findFirst().orElseThrow(() -> new IllegalStateException("no fixture " + c.fixture()));
            ParityConfigs.Config config = ParityConfigs.get(c.config());
            String page = TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", FacetBrowser.marked(field.markup()), "");
            String real = server.registerRaw(page);
            String broken = server.registerRaw(page.replace("</head>", "<style>" + c.css() + "</style></head>"));
            String[] pictures = new String[2];
            try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                FacetPixelTest.capture(cdp, real, List.of(field), (f, png) -> pictures[0] = png);
                FacetPixelTest.capture(cdp, broken, List.of(field), (f, png) -> pictures[1] = png);
            }
            results.put(c.key(), pictures[0].equals(pictures[1]) ? "SAME PICTURE" : "rejected");
        }
    }
}
