package dev.redspartanlabs.ashlar.css;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static dev.redspartanlabs.ashlar.css.TextEntrySources.assertNoTailwindUtility;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.assertOnlyAshClasses;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.declared;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.locals;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.markup;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.script;
import static dev.redspartanlabs.ashlar.css.TextEntrySources.template;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the source contract of Checkbox, migrated with the Choice Controls (PR 1). The template keeps every parameter, default, native attribute, ARIA wiring and id, and carries
 * only Ashlar classes. The parameters below are the ones the template declared BEFORE the migration (commit 0c26bc2), so this fails if the public contract moves. The rendered
 * result is measured in a real browser by the showcase tests ChoiceParityTest, ChoicePixelTest, ChoiceTailwindFreeTest and ChoiceBehaviourTest; this reads the sources only.
 */
class CheckboxContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@import dev.redspartanlabs.ashlar.AshlarAssets",
                "@param String name",
                "@param String label",
                "@param String value = \"on\"",
                "@param boolean checked = false",
                "@param boolean disabled = false",
                "@param boolean required = false",
                "@param boolean indeterminate = false",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\""), declared("checkbox"));
    }

    @Test
    void noTailwindUtilityRemainsAndOnlyAshlarClassesAreWritten() throws IOException {
        assertOnlyAshClasses("checkbox");
        assertNoTailwindUtility("checkbox");
    }

    @Test
    void theCheckboxKeepsItsNativeInputItsLabelItsRequiredMarkerAndItsHelpAndErrorWiring() throws IOException {
        String m = markup("checkbox");
        assertEquals("<div class=\"ash-checkbox\"> <div class=\"ash-choice\"> <input type=\"checkbox\" id=\"${checkboxId}\" name=\"${name}\" value=\"${value}\" checked=\"${checked}\""
                + " disabled=\"${disabled}\" required=\"${required}\" aria-invalid=\"${ariaInvalidAttr}\" aria-describedby=\"${describedByAttr}\" data-checkbox-indeterminate=\"${indeterminateAttr}\""
                + " class=\"ash-choice__input\" > <div class=\"ash-choice__body\"> <label for=\"${checkboxId}\" class=\"ash-choice__label\"> ${label} @if (required)"
                + " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span> @endif </label> @if (hasHelp) <p id=\"${helpId}\" class=\"ash-choice__description\">${helpText}</p> @endif"
                + " @if (showError) <p id=\"${errorId}\" class=\"ash-choice__error\" role=\"alert\">${errorMessage}</p> @endif </div> </div> </div>"
                + " @if (indeterminate) <script type=\"module\" src=\"${AshlarAssets.js(\"checkbox.js\")}\"></script> @endif", m);
        String l = locals("checkbox");
        assertTrue(l.contains("!{String checkboxId = id.isEmpty() ? name : id;}"));
        assertTrue(l.contains("!{String helpId = checkboxId + \"-help\";}") && l.contains("!{String errorId = checkboxId + \"-error\";}"));
        assertTrue(l.contains("!{boolean showError = error && !errorMessage.isEmpty();}"));
        assertTrue(l.contains("!{String describedByAttr = hasHelp && showError ? helpId + \" \" + errorId : hasHelp ? helpId : showError ? errorId : null;}"));
        assertTrue(l.contains("!{String ariaInvalidAttr = error ? \"true\" : null;}"), "aria-invalid follows the error flag, not the message");
        assertTrue(l.contains("!{String indeterminateAttr = indeterminate ? \"true\" : null;}"));
        assertFalse(l.contains("checkboxClasses"), "the Tailwind class local is gone");
    }

    @Test
    void theScriptIsStillLoadedOnlyForAnIndeterminateCheckboxAndIsUnchangedInKind() throws IOException {
        assertTrue(markup("checkbox").endsWith("@if (indeterminate) <script type=\"module\" src=\"${AshlarAssets.js(\"checkbox.js\")}\"></script> @endif"));
        String js = script("checkbox").replaceAll("(?m)^\\s*//.*$", "");
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\"") && js.contains("[data-checkbox-indeterminate=\"true\"]") && js.contains("checkbox.indeterminate = true;"));
        assertFalse(js.contains("classList") || js.contains("className") || js.contains(".style"), "the script sets the DOM-only property and nothing presentational");
    }
}
