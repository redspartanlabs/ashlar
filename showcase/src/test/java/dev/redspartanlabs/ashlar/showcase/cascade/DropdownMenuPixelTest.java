package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.icon.Icon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pixel parity: every shot below, in all 22 configurations, painted by the legacy markup on the Dropdown Menu oracle and by the migrated markup on the application
 * stylesheet, must be identical: the comparison is {@link PixelPolicy#EXACT}, unchanged, with no tolerance and no retry. Each page is captured twice and the two captures
 * must be identical before they are compared (a rendering that differs from itself cannot be compared). The trigger at rest, under the pointer and with keyboard focus; the
 * open menu; an entry under the pointer, focused, and both; the disabled entry; the icon trigger; a wrapping entry; an entry with aria-current; and the trigger inside a
 * {@code <header>} directly in the body, where civic's masthead rule reaches it.
 */
class DropdownMenuPixelTest {

    private static final Pattern DATA = Pattern.compile("\"data\":\"([^\"]+)\"");
    private static final List<String> MISMATCHES = new ArrayList<>();
    private static final List<String> UNSTABLE = new ArrayList<>();
    private static final Map<String, String> SAMPLE = new LinkedHashMap<>();
    private static int compared;

    private record Shot(String key, String markup, String force, List<String> pseudo, boolean inHeader) {
    }

    private static List<Shot> shots() {
        String actions = OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("Actions", null, DropdownMenuBrowser.ACTIONS));
        String open = DropdownMenuBrowser.inState(actions, true, true);
        String icon = DropdownMenuBrowser.inState(OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("Settings", Icon.SETTINGS, "General|#g", "Notifications|#n")), true, true);
        String wrapping = DropdownMenuBrowser.inState(OverlayBrowser.withoutScripts(DropdownMenuBrowser.menu("More", null, "Short|#s", DropdownMenuBrowser.LONG_LABEL + "|#l")), true, true);
        String trigger = "[data-dropdown-menu-trigger]";
        String first = "[data-dropdown-menu-item]:first-child";
        List<Shot> shots = new ArrayList<>();
        shots.add(new Shot("closed", actions, null, List.of(), false));
        shots.add(new Shot("closed, trigger hover", actions, trigger, List.of("hover"), false));
        shots.add(new Shot("closed, trigger focus-visible", actions, trigger, List.of("focus", "focus-visible"), false));
        shots.add(new Shot("open", open, null, List.of(), false));
        shots.add(new Shot("open, trigger hover", open, trigger, List.of("hover"), false));
        shots.add(new Shot("open, entry hover", open, first, List.of("hover"), false));
        shots.add(new Shot("open, entry focus", open, first, List.of("focus", "focus-visible"), false));
        shots.add(new Shot("open, entry focus and hover", open, first, List.of("focus", "focus-visible", "hover"), false));
        shots.add(new Shot("open, disabled entry hover and focus", open, "[data-dropdown-menu-item][aria-disabled]", List.of("hover", "focus", "focus-visible"), false));
        shots.add(new Shot("open, icon trigger", icon, null, List.of(), false));
        shots.add(new Shot("open, wrapping entry", wrapping, null, List.of(), false));
        shots.add(new Shot("open, entry with aria-current", open.replaceFirst("data-dropdown-menu-item", "data-dropdown-menu-item aria-current=\"page\""), null, List.of(), false));
        shots.add(new Shot("closed, in body > header", actions, null, List.of(), true));
        shots.add(new Shot("closed, in body > header, trigger hover", actions, trigger, List.of("hover"), true));
        return shots;
    }

    private static String capture(DevToolsSession cdp) throws Exception {
        Matcher m = DATA.matcher(cdp.call("Page.captureScreenshot", "{\"format\":\"png\"}"));
        if (!m.find()) {
            throw new IllegalStateException("no screenshot data");
        }
        return m.group(1);
    }

    /** The page's picture with the shot's state forced, captured twice; null (and the case recorded as unstable) when the two captures differ. */
    private static String picture(DevToolsSession cdp, String url, Shot s, String where) throws Exception {
        cdp.navigate(url);
        Thread.sleep(50);
        if (s.force() != null) {
            List<Integer> nodes = cdp.nodes("[data-shot] " + s.force());
            if (nodes.size() != 1) {
                throw new IllegalStateException(where + ": exactly one element to force the state on, found " + nodes.size());
            }
            cdp.force(nodes, s.pseudo());
            Thread.sleep(30);
        }
        String first = capture(cdp);
        String second = capture(cdp);
        if (!first.equals(second)) {
            UNSTABLE.add(where + ": " + PixelDiagnostics.measure(first, second).describe());
            return null;
        }
        return first;
    }

    private static String placed(String html) {
        return "<div data-shot style=\"padding:20px 60px 300px 60px\">" + html + "</div>";
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("dropdown-menu-pixels");
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            for (ParityConfigs.Config config : ParityConfigs.all()) {
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    OverlayBrowser.viewport(cdp, 900, 600);
                    for (Shot s : shots()) {
                        String legacyMarkup = DropdownMenuBrowser.legacy(s.markup());
                        if (legacyMarkup.contains("ash-dropdown-menu")) {
                            throw new IllegalStateException("the legacy rendering must carry no migrated class: " + s.key());
                        }
                        String legacyUrl = server.registerRaw(DropdownMenuBrowser.page(List.of(ParityFrames.generated("dropdown-menu-oracle.css")), config,
                                s.inHeader() ? placed(legacyMarkup) : "", s.inHeader() ? "" : placed(legacyMarkup)));
                        String migratedUrl = server.registerRaw(DropdownMenuBrowser.page(List.of(ParityFrames.APP), config,
                                s.inHeader() ? placed(s.markup()) : "", s.inHeader() ? "" : placed(s.markup())));
                        String where = config.id() + " " + s.key();
                        String legacy = picture(cdp, legacyUrl, s, where + " (legacy)");
                        String migrated = picture(cdp, migratedUrl, s, where + " (migrated)");
                        if (legacy == null || migrated == null) {
                            continue;
                        }
                        compared++;
                        PixelPolicy.Verdict verdict = PixelPolicy.compare(legacy, migrated, PixelPolicy.EXACT, null);
                        if (!verdict.accepted()) {
                            MISMATCHES.add(where + ": " + PixelDiagnostics.measure(legacy, migrated).describe());
                        }
                        if (config.id().equals("light/none")) {
                            SAMPLE.put(s.key(), legacy);
                        }
                    }
                }
            }
        }
    }

    @Test
    void everyTriggerAndMenuPaintsTheSameAsTheLegacyOneInEveryConfiguration() {
        assertEquals(List.of(), UNSTABLE, "a rendering that differs from itself cannot be compared");
        assertEquals(List.of(), MISMATCHES.size() > 40 ? MISMATCHES.subList(0, 40) : MISMATCHES);
    }

    @Test
    void theComparisonIsNotVacuous() {
        assertEquals(22 * shots().size(), compared, "every shot in every configuration was compared (an unstable rendering is not)");
        // The component really is in the pictures: the menu opens, the states paint, the icon and the wrapping entry are drawn.
        assertNotEquals(SAMPLE.get("closed"), SAMPLE.get("open"));
        assertNotEquals(SAMPLE.get("closed"), SAMPLE.get("closed, trigger focus-visible"));
        assertNotEquals(SAMPLE.get("open"), SAMPLE.get("open, entry focus"));
        assertNotEquals(SAMPLE.get("open, entry hover"), SAMPLE.get("open, entry focus"));
        assertNotEquals(SAMPLE.get("open"), SAMPLE.get("open, icon trigger"));
        assertTrue(SAMPLE.size() == shots().size(), "a sample of every shot");
    }
}
