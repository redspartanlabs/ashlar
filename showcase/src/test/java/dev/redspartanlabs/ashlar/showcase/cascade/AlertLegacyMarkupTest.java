package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that renders a migrated Alert with its legacy classes must be exact, and must fail loudly rather than half-rewrite. */
class AlertLegacyMarkupTest {

    private static final String TITLED = "<div class=\"ash-alert ash-alert--warning\" data-alert data-alert-variant=\"warning\">\n"
            + "<svg\n class=\"ash-icon ash-alert__icon\" viewBox=\"0 0 24 24\"></svg>\n"
            + "<div class=\"ash-alert__content\">\n<span class=\"ash-sr-only\">Warning:</span>\n"
            + "<h3 class=\"ash-alert__title\">T</h3>\n\n<div class=\"ash-alert__body\">\n<p>B</p>\n<div>nested</div>\n</div>\n</div>\n</div>";
    private static final String PLAIN = "<div class=\"ash-alert ash-alert--error\" data-alert data-alert-variant=\"error\">"
            + "<svg class=\"ash-icon ash-alert__icon\"></svg><div class=\"ash-alert__content\">"
            + "<span class=\"ash-sr-only\">Error:</span><div class=\"ash-alert__body\">B</div></div></div>";

    @Test
    void aTitledAlertIsRewrittenRoleByRoleToItsExactLegacyClasses() throws IOException {
        AlertLegacyMarkup legacy = new AlertLegacyMarkup();
        String out = legacy.rewrite("<p>before</p>" + TITLED + "<p>after</p>");
        assertTrue(out.startsWith("<p>before</p><div class=\"" + legacy.classes("container", "warning") + "\" data-alert data-alert-variant=\"warning\">"), out);
        assertTrue(out.contains("class=\"inline-block shrink-0 " + legacy.classes("icon", "warning") + "\""), out);
        assertTrue(out.contains("<div class=\"" + legacy.classes("content", "warning") + "\">"), out);
        assertTrue(out.contains("<span class=\"sr-only\">Warning:</span>"), out);
        assertTrue(out.contains("<h3 class=\"" + legacy.classes("title", "warning") + "\">T</h3>"), out);
        assertTrue(out.contains("<div class=\"" + legacy.classes("body-titled", "warning") + "\">"), out);
        assertTrue(out.endsWith("</div>\n</div>\n</div><p>after</p>"), out);
        assertFalse(out.contains("ash-alert"), out);
        assertTrue(legacy.classes("body-titled", "warning").startsWith("mt-1 "), "the body after a title keeps its mt-1");
    }

    @Test
    void anUntitledAlertGetsThePlainBodyWithoutTheTitleSpacing() throws IOException {
        AlertLegacyMarkup legacy = new AlertLegacyMarkup();
        String out = legacy.rewrite(PLAIN);
        assertTrue(out.contains("<div class=\"" + legacy.classes("body-plain", "error") + "\">B</div>"), out);
        assertFalse(legacy.classes("body-plain", "error").contains("mt-1"));
        assertFalse(out.contains("ash-alert"), out);
    }

    @Test
    void markupWithoutAnAlertAndOtherAshClassesAreLeftExactlyAsServed() throws IOException {
        String page = "<span class=\"ash-sr-only\">x</span><span class=\"ash-badge ash-badge--info\" data-badge></span>";
        assertEquals(page, new AlertLegacyMarkup().rewrite(page));
    }

    @Test
    void severalAlertsAreEachRewrittenWithTheirOwnVariant() throws IOException {
        AlertLegacyMarkup legacy = new AlertLegacyMarkup();
        String out = legacy.rewrite(TITLED + PLAIN);
        assertTrue(out.contains(legacy.classes("container", "warning")) && out.contains(legacy.classes("container", "error")), out);
        assertEquals(2, AlertLegacyMarkup.count(TITLED + PLAIN));
        assertEquals(0, AlertLegacyMarkup.count(out));
    }

    @Test
    void anAlertMissingAPartFailsLoudlyInsteadOfBeingHalfRewritten() throws IOException {
        AlertLegacyMarkup legacy = new AlertLegacyMarkup();
        String noIcon = PLAIN.replace("<svg class=\"ash-icon ash-alert__icon\"></svg>", "");
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(noIcon));
        String unknownPart = PLAIN.replace("</div></div>", "<i class=\"ash-alert__extra\"></i></div></div>");
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(unknownPart));
        String mismatched = PLAIN.replace("ash-alert--error", "ash-alert--info");
        assertThrows(IllegalStateException.class, () -> legacy.rewrite(mismatched));
    }

    @Test
    void everyVariantHasEveryRoleAndTheLegacyTokensAreTheFiftyFiveTheOracleMustCompile() throws IOException {
        AlertLegacyMarkup legacy = new AlertLegacyMarkup();
        for (String variant : AlertLegacyMarkup.VARIANTS) {
            for (String role : new String[] {"container", "icon", "title", "body-titled", "body-plain"}) {
                assertFalse(legacy.classes(role, variant).isBlank(), role + "/" + variant);
            }
        }
        assertEquals(55, legacy.allTokens().size(), legacy.allTokens().toString());
    }
}
