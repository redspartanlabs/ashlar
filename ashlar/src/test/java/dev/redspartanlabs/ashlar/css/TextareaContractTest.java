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
 * Pins the source contract of Textarea, migrated with the Text Entry family. The template keeps every parameter, default, native attribute, ARIA wiring and data- hook, and
 * carries only Ashlar's classes. The parameters below are the ones the template declared BEFORE the migration (commit 831a2f5), so this fails if the public contract moves. The
 * rendered result is measured in a real browser by the showcase's TextEntryParityTest, TextEntryTailwindFreeTest and TextEntryBehaviourTest; this reads the sources only.
 */
class TextareaContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@import dev.redspartanlabs.ashlar.AshlarAssets",
                "@param String name",
                "@param String label",
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
                "@param String autocomplete = \"\"",
                "@param int rows = 4",
                "@param boolean resizable = true",
                "@param boolean showCharacterCount = false"), declared("textarea"));
    }

    @Test
    void theTextareaKeepsEveryNativeAttributeTheCounterHooksAndTheWiring() throws IOException {
        String m = markup("textarea");
        for (String attribute : List.of("id=\"${textareaId}\"", "name=\"${name}\"", "placeholder=\"${placeholderAttr}\"", "required=\"${required}\"", "disabled=\"${disabled}\"", "readonly=\"${readonly}\"",
                "minlength=\"${minlength}\"", "maxlength=\"${maxlength}\"", "autocomplete=\"${autocompleteAttr}\"", "rows=\"${rows}\"", "aria-invalid=\"${ariaInvalidAttr}\"",
                "aria-describedby=\"${describedByAttr}\"", "class=\"${textareaClasses}\"")) {
            assertTrue(m.contains(attribute), "the textarea still carries " + attribute);
        }
        assertTrue(m.contains("<div class=\"ash-field\" data-textarea-counter=\"${counterMarkerAttr}\">"), "the counter hook stays on the wrapper");
        assertTrue(m.contains("<span id=\"${countId}\" data-textarea-count class=\"ash-textarea__count\">${counterText}</span>"), "the count keeps its id and hook, and has no state class");
        assertTrue(m.contains("<div class=\"ash-textarea__row\">") && m.contains("<p id=\"${helpId}\" class=\"ash-textarea__help\">${helpText}</p>"));
        assertTrue(m.contains("<p id=\"${errorId}\" class=\"ash-field__error\" role=\"alert\">${errorMessage}</p>"));
        assertTrue(m.endsWith("@if (showCharacterCount) <script type=\"module\" src=\"${AshlarAssets.js(\"textarea.js\")}\"></script> @endif"), "the script is loaded only with the counter");
        String l = locals("textarea");
        assertTrue(l.contains("!{String describedByJoined = (helpPart + \" \" + errorPart + \" \" + countPart).trim().replaceAll(\" +\", \" \");}"));
        assertTrue(l.contains("!{String counterText = maxlength != null ? initialCount + \" / \" + maxlength : initialCount + \" characters\";}"));
    }

    @Test
    void theClassesAreAshlarsAndAFixedTextareaIsAModifier() throws IOException {
        assertOnlyAshClasses("textarea");
        assertNoTailwindUtility("textarea");
        assertTrue(locals("textarea").contains("!{String textareaClasses = \"ash-field__control ash-field__control--readonly ash-textarea__control\" + (resizable ? \"\" : \" ash-textarea__control--fixed\");}"));
        assertFalse(template("textarea").replaceAll("(?s)<%--.*?--%>", "").contains("resizeClass"), "the Tailwind resize local is gone");
    }

    @Test
    void theScriptWritesTheCountTextAndOneAttributeAndNoClass() throws IOException {
        String js = script("textarea").replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList") || js.contains("className") || js.contains(".style") || js.contains("text-red") || js.contains("text-slate"), "no class and no style is written");
        assertTrue(js.contains("counter.toggleAttribute(\"data-at-limit\", atLimit);"));
        assertTrue(js.contains("counter.textContent = hasMax ? `${length} / ${textarea.maxLength}` : `${length} characters`;"));
        assertTrue(js.contains("const atLimit = hasMax && length >= textarea.maxLength;"), "the limit rule is unchanged");
        assertTrue(js.contains("textarea.addEventListener(\"input\", update);") && js.contains("document.querySelectorAll(\"[data-textarea-counter]\")"), "per-instance scoping and the input listener are unchanged");
    }
}
