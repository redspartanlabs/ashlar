package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The browser's own clear affordance in a type="search" field. WebKit and Blink draw an "x" in the field once it has text, which would double the component's own clear button,
 * so the component removes it ({@code ::-webkit-search-cancel-button { appearance: none }}). Computed style cannot show this: Chrome reports a webkit pseudo-element's computed
 * values as the host's. So it is measured in pixels, with the component's own button hidden, in the real rendering of a field that has text:
 * <ol>
 *   <li>the migrated field with the rule is NOT the same picture as the same field with the rule neutralised (so the native "x" is really there to remove, and the rule really removes it);</li>
 *   <li>the migrated field is the same picture as the legacy field (which removed it with Tailwind's arbitrary variant);</li>
 *   <li>repeating a capture gives the same bytes (so a difference above is the rule, not noise).</li>
 * </ol>
 * Chrome only: WebKit's and Gecko's search fields are not driven here.
 */
class TextEntrySearchCancelTest {

    private static String shot(ParitySession session, CascadeFixtureServer server, String sheet, String body, String extraCss) throws Exception {
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String page = TextEntryBrowser.page(List.of(sheet), config, "", body, "").replace("</head>", "<style>[data-search-input-clear]{display:none !important}" + extraCss + "</style></head>");
        try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            cdp.navigate(server.registerRaw(page));
            cdp.evaluate("(function(){document.getElementById('s').focus();return 'ok'})()");
            TextEntryBrowser.hover(cdp, "#s"); // Chrome draws the native x only while the field is hovered (or focused)
            String box = cdp.evaluate("(function(){var b=document.getElementById('s').getBoundingClientRect();return JSON.stringify({x:b.x,y:b.y,width:b.width,height:b.height})})()");
            String response = cdp.call("Page.captureScreenshot", "{\"clip\":" + box.replace("}", ",\"scale\":1}") + "}");
            int at = response.indexOf("\"data\":\"");
            assertTrue(at > 0, response);
            return response.substring(at + 8, response.indexOf('"', at + 8));
        }
    }

    @Test
    void theNativeSearchCancelButtonIsRemovedExactlyAsTheLegacyFieldRemovedIt() throws Exception {
        ParitySession session = ParitySession.open("text-entry-search-cancel");
        String migrated = TextEntryBrowser.marked(TextEntryBrowser.search(TextEntryBrowser.o("id", "s", "value", "query")));
        String legacy = new IconLegacyMarkup().rewrite(new TextEntryLegacyMarkup().rewrite(migrated));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String withRule = shot(session, server, ParityFrames.ASHLAR, migrated, "");
            String withRuleAgain = shot(session, server, ParityFrames.ASHLAR, migrated, "");
            String withoutRule = shot(session, server, ParityFrames.ASHLAR, migrated, ".ash-search-input__field::-webkit-search-cancel-button{-webkit-appearance:searchfield-cancel-button !important;appearance:auto !important}");
            String legacyShot = shot(session, server, ParityFrames.generated("text-entry-oracle.css"), legacy, "");
            assertEquals(withRule, withRuleAgain, "a repeated capture is identical, so a difference is not noise");
            assertNotEquals(withRule, withoutRule, "with the rule neutralised the native x is drawn, so the rule is what removes it");
            assertEquals(withRule, legacyShot, "the migrated field is the same picture as the legacy field");
        }
    }
}
