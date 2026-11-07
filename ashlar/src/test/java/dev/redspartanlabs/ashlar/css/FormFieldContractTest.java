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
 * Pins the source contract of Form Field, migrated with the Text Entry family. The template keeps every parameter, default, native attribute, ARIA wiring and data- hook, and
 * carries only Ashlar's classes. The parameters below are the ones the template declared BEFORE the migration (commit 831a2f5), so this fails if the public contract moves. The
 * rendered result is measured in a real browser by the showcase's TextEntryParityTest, TextEntryTailwindFreeTest and TextEntryBehaviourTest; this reads the sources only.
 */
class FormFieldContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@param String fieldId",
                "@param String label",
                "@param gg.jte.Content control",
                "@param String helpText = \"\"",
                "@param String errorMessage = \"\"",
                "@param boolean required = false"), declared("form-field"));
    }

    @Test
    void noTailwindUtilityRemainsInTheTemplate() throws IOException {
        assertOnlyAshClasses("form-field");
        assertNoTailwindUtility("form-field");
    }

    @Test
    void theScaffoldingKeepsItsLabelHelpAndErrorWiringAndRenderedSemantics() throws IOException {
        String m = markup("form-field");
        assertEquals("<div class=\"ash-field\"> <label for=\"${fieldId}\" class=\"ash-field__label\"> ${label} @if (required) <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span> @endif </label>"
                + " ${control} @if (hasHelp) <p id=\"${helpId}\" class=\"ash-field__help\">${helpText}</p> @endif"
                + " @if (hasError) <p id=\"${errorId}\" class=\"ash-field__error\" role=\"alert\">${errorMessage}</p> @endif </div>", m);
        String l = locals("form-field");
        assertTrue(l.contains("!{String helpId = fieldId + \"-help\";}") && l.contains("!{String errorId = fieldId + \"-error\";}"), "the ids stay deterministic: {fieldId}-help and {fieldId}-error");
        assertTrue(l.contains("!{boolean hasHelp = !helpText.isEmpty();}") && l.contains("!{boolean hasError = !errorMessage.isEmpty();}"));
        assertFalse(m.contains("<script"), "a Form Field has no runtime behaviour");
    }
}
