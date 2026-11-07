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
 * Pins the source contract of Text Input, migrated with the Text Entry family. The template keeps every parameter, default, native attribute, ARIA wiring and data- hook, and
 * carries only Ashlar's classes. The parameters below are the ones the template declared BEFORE the migration (commit 831a2f5), so this fails if the public contract moves. The
 * rendered result is measured in a real browser by the showcase's TextEntryParityTest, TextEntryTailwindFreeTest and TextEntryBehaviourTest; this reads the sources only.
 */
class TextInputContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@import dev.redspartanlabs.ashlar.icon.Icon",
                "@import dev.redspartanlabs.ashlar.AshlarAssets",
                "@param String name",
                "@param String label",
                "@param String type = \"text\"",
                "@param String value = \"\"",
                "@param String placeholder = \"\"",
                "@param boolean required = false",
                "@param boolean disabled = false",
                "@param boolean readonly = false",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\"",
                "@param Integer minlength = null",
                "@param Integer maxlength = null",
                "@param String pattern = \"\"",
                "@param String autocomplete = \"\"",
                "@param String inputmode = \"\"",
                "@param String mask = \"\"",
                "@param Icon leadingIcon = null",
                "@param Icon trailingIcon = null"), declared("text-input"));
    }

    @Test
    void theInputKeepsEveryNativeAttributeAndTheLabelHelpAndErrorWiring() throws IOException {
        String m = markup("text-input");
        for (String attribute : List.of("type=\"${resolvedType}\"", "id=\"${inputId}\"", "name=\"${name}\"", "value=\"${value}\"", "placeholder=\"${placeholderAttr}\"", "required=\"${required}\"",
                "disabled=\"${disabled}\"", "readonly=\"${readonly}\"", "minlength=\"${minlength}\"", "maxlength=\"${maxlength}\"", "pattern=\"${patternAttr}\"", "autocomplete=\"${autocompleteAttr}\"",
                "inputmode=\"${inputmodeAttr}\"", "aria-invalid=\"${ariaInvalidAttr}\"", "aria-describedby=\"${describedByAttr}\"", "data-mask=\"${maskAttr}\"", "class=\"${inputClasses}\"")) {
            assertTrue(m.contains(attribute), "the input still carries " + attribute);
        }
        assertTrue(m.contains("<label for=\"${inputId}\" class=\"ash-field__label\">") && m.contains("<span class=\"ash-field__required\" aria-hidden=\"true\"> *</span>"));
        assertTrue(m.contains("<p id=\"${helpId}\" class=\"ash-field__help\">${helpText}</p>") && m.contains("<p id=\"${errorId}\" class=\"ash-field__error\" role=\"alert\">${errorMessage}</p>"));
        String l = locals("text-input");
        assertTrue(l.contains("!{String inputId = id.isEmpty() ? name : id;}"));
        assertTrue(l.contains("!{boolean showError = error && !errorMessage.isEmpty();}"));
        assertTrue(l.contains("!{String describedByAttr = hasHelp && showError ? helpId + \" \" + errorId : hasHelp ? helpId : showError ? errorId : null;}"));
        assertTrue(l.contains("!{String ariaInvalidAttr = error ? \"true\" : null;}"), "aria-invalid follows the error flag, not the message");
        assertTrue(l.contains("(inputmode.isEmpty() && \"phone\".equals(mask)) ? \"tel\" : inputmode"), "the phone mask still narrows an unset inputmode");
    }

    @Test
    void theClassesAreAshlarsAndThePaddingModifiersFollowTheIcons() throws IOException {
        assertOnlyAshClasses("text-input");
        assertNoTailwindUtility("text-input");
        assertTrue(locals("text-input").contains("!{String inputClasses = \"ash-field__control ash-field__control--readonly\" + (leadingIcon != null ? \" ash-field__control--leading\" : \"\")"
                + " + (trailingIcon != null ? \" ash-field__control--trailing\" : \"\");}"));
        String m = markup("text-input");
        assertTrue(m.contains("<div class=\"ash-field__box\">"));
        assertTrue(m.contains("<span class=\"ash-field__icon\"> @template.ashlar.components.icon(icon = leadingIcon, classes = \"ash-icon--sm\") </span>"));
        assertTrue(m.contains("<span class=\"ash-field__icon ash-field__icon--end\"> @template.ashlar.components.icon(icon = trailingIcon, classes = \"ash-icon--sm\") </span>"));
    }

    @Test
    void theScriptIsStillLoadedOnlyForAMaskAndTheScriptIsUnchangedInKind() throws IOException {
        assertTrue(markup("text-input").endsWith("@if (hasMask) <script type=\"module\" src=\"${AshlarAssets.js(\"text-input.js\")}\"></script> @endif"));
        String js = script("text-input").replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList") || js.contains("className") || js.contains(".style"), "the mask script never touched a class or a style, and still does not");
    }
}
