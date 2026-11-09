package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static dev.redspartanlabs.ashlar.css.TextEntrySources.assertNoTailwindUtility;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.assertOnlyAshClasses;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.declared;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.locals;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.markup;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.template;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the source contract of Toggle, migrated with the Choice Controls (PR 2). The template keeps every parameter, default, native attribute, ARIA wiring and id, the real checkbox
 * under the track and thumb, and carries only Ashlar classes. The parameters below are the ones the template declared BEFORE the migration (commit d558802), so this fails if the
 * public contract moves. The rendered result is measured in a real browser by the showcase tests; this reads the sources only.
 */
class ToggleContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@param String name",
                "@param String label",
                "@param String value = \"on\"",
                "@param boolean checked = false",
                "@param boolean disabled = false",
                "@param boolean required = false",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\"",
                "@param String labelPosition = \"end\""), declared("toggle"));
    }

    @Test
    void noTailwindUtilityRemainsAndOnlyAshlarClassesAreWritten() throws IOException {
        assertOnlyAshClasses("toggle");
        assertNoTailwindUtility("toggle");
        String m = template("toggle").replaceAll("(?s)<%--.*?--%>", "");
        assertFalse(m.contains("peer") || java.util.regex.Pattern.compile("(?<![\\w-])sr-only").matcher(m).find(), "no Tailwind peer or sr-only utility");
    }

    @Test
    void theRealCheckboxStaysUnderTheTrackAndThumbAsTheirEarlierSiblingInsideOneLabel() throws IOException {
        String m = markup("toggle");
        assertEquals("<div class=\"ash-toggle\"> <label class=\"${labelClasses}\"> <span class=\"ash-toggle__switch\"> <input type=\"checkbox\" id=\"${toggleId}\" name=\"${name}\" value=\"${value}\""
                + " checked=\"${checked}\" disabled=\"${disabled}\" required=\"${required}\" aria-invalid=\"${ariaInvalidAttr}\" aria-describedby=\"${describedByAttr}\""
                + " class=\"ash-toggle__input ash-sr-only\" > <span aria-hidden=\"true\" class=\"ash-toggle__track\"></span> <span aria-hidden=\"true\" class=\"ash-toggle__thumb\"></span> </span>"
                + " <span class=\"ash-toggle__text\"> ${label} @if (required) <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span> @endif </span> </label>"
                + " @if (hasHelp) <p id=\"${helpId}\" class=\"ash-choice__description ash-toggle__help\">${helpText}</p> @endif"
                + " @if (showError) <p id=\"${errorId}\" class=\"ash-choice__error ash-toggle__error\" role=\"alert\">${errorMessage}</p> @endif </div>", m);
        assertFalse(m.contains("role=\"switch\"") || m.contains("<script"), "it stays a plain checkbox with no script");
    }

    @Test
    void theIdsTheDescribedByWiringTheInvalidFlagAndTheLabelPositionAreComputedAsBefore() throws IOException {
        String l = locals("toggle");
        assertTrue(l.contains("!{String toggleId = id.isEmpty() ? name : id;}"));
        assertTrue(l.contains("!{String helpId = toggleId + \"-help\";}") && l.contains("!{String errorId = toggleId + \"-error\";}"));
        assertTrue(l.contains("!{boolean showError = error && !errorMessage.isEmpty();}"));
        assertTrue(l.contains("!{String describedByAttr = hasHelp && showError ? helpId + \" \" + errorId : hasHelp ? helpId : showError ? errorId : null;}"));
        assertTrue(l.contains("!{String ariaInvalidAttr = error ? \"true\" : null;}"), "aria-invalid follows the error flag, not the message");
        assertTrue(l.contains("!{boolean labelStart = \"start\".equalsIgnoreCase(labelPosition);}"));
        assertTrue(l.contains("!{String labelClasses = labelStart ? \"ash-toggle__label ash-toggle__label--start\" : \"ash-toggle__label\";}"));
    }
}
