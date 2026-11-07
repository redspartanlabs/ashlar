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
 * Pins the source contract of Number Input, migrated with the Text Entry family. The template keeps every parameter, default, native attribute, ARIA wiring and data- hook, and
 * carries only Ashlar's classes. The parameters below are the ones the template declared BEFORE the migration (commit 831a2f5), so this fails if the public contract moves. The
 * rendered result is measured in a real browser by the showcase's TextEntryParityTest, TextEntryTailwindFreeTest and TextEntryBehaviourTest; this reads the sources only.
 */
class NumberInputContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@param String name",
                "@param String label",
                "@param String value = \"\"",
                "@param String placeholder = \"\"",
                "@param String min = \"\"",
                "@param String max = \"\"",
                "@param String step = \"\"",
                "@param boolean required = false",
                "@param boolean disabled = false",
                "@param boolean readonly = false",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\""), declared("number-input"));
    }

    @Test
    void theInputKeepsEveryNativeAttributeAndTheLabelHelpAndErrorWiring() throws IOException {
        String m = markup("number-input");
        for (String attribute : List.of("type=\"number\"", "id=\"${inputId}\"", "name=\"${name}\"", "value=\"${value}\"", "placeholder=\"${placeholderAttr}\"", "min=\"${minAttr}\"", "max=\"${maxAttr}\"",
                "step=\"${stepAttr}\"", "required=\"${required}\"", "disabled=\"${disabled}\"", "readonly=\"${readonly}\"", "aria-invalid=\"${ariaInvalidAttr}\"", "aria-describedby=\"${describedByAttr}\"",
                "class=\"${inputClasses}\"")) {
            assertTrue(m.contains(attribute), "the input still carries " + attribute);
        }
        assertTrue(m.contains("<label for=\"${inputId}\" class=\"ash-field__label\">") && m.contains("<span class=\"ash-field__required\" aria-hidden=\"true\"> *</span>"));
        assertTrue(m.contains("<p id=\"${helpId}\" class=\"ash-field__help\">${helpText}</p>") && m.contains("<p id=\"${errorId}\" class=\"ash-field__error\" role=\"alert\">${errorMessage}</p>"));
        assertFalse(m.contains("<script") || m.contains("ash-field__box"), "zero JavaScript, and the width cap is on the input, not on a box");
        String l = locals("number-input");
        assertTrue(l.contains("!{String describedByAttr = hasHelp && showError ? helpId + \" \" + errorId : hasHelp ? helpId : showError ? errorId : null;}"));
        assertTrue(l.contains("!{String ariaInvalidAttr = error ? \"true\" : null;}"));
    }

    @Test
    void theClassesAreAshlarsAndTheWidthCapIsOnTheInputItself() throws IOException {
        assertOnlyAshClasses("number-input");
        assertNoTailwindUtility("number-input");
        assertTrue(locals("number-input").contains("!{String inputClasses = \"ash-field__control ash-field__control--readonly ash-field__control--narrow\";}"));
    }
}
