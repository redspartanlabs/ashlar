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
 * Pins the source contract of Search Input, migrated with the Text Entry family. The template keeps every parameter, default, native attribute, ARIA wiring and data- hook, and
 * carries only Ashlar's classes. The parameters below are the ones the template declared BEFORE the migration (commit 831a2f5), so this fails if the public contract moves. The
 * rendered result is measured in a real browser by the showcase's TextEntryParityTest, TextEntryTailwindFreeTest and TextEntryBehaviourTest; this reads the sources only.
 */
class SearchInputContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@import dev.redspartanlabs.ashlar.icon.Icon",
                "@import dev.redspartanlabs.ashlar.AshlarAssets",
                "@param String name",
                "@param String label",
                "@param String value = \"\"",
                "@param String placeholder = \"Search...\"",
                "@param boolean required = false",
                "@param boolean disabled = false",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\""), declared("search-input"));
    }

    @Test
    void theInputAndTheClearButtonKeepEveryNativeAttributeAndHook() throws IOException {
        String m = markup("search-input");
        for (String attribute : List.of("type=\"search\"", "id=\"${inputId}\"", "name=\"${name}\"", "value=\"${value}\"", "placeholder=\"${placeholderAttr}\"", "required=\"${required}\"",
                "aria-invalid=\"${ariaInvalidAttr}\"", "aria-describedby=\"${describedByAttr}\"", "data-search-input-field", "class=\"${inputClasses}\"")) {
            assertTrue(m.contains(attribute), "the input still carries " + attribute);
        }
        assertTrue(m.contains("<div class=\"ash-field\" data-search-input>"), "the root hook stays");
        assertTrue(m.contains("<button type=\"button\" disabled=\"${disabled}\" hidden=\"${clearHidden}\" aria-label=\"Clear search\" data-search-input-clear class=\"ash-search-input__clear\" >"),
                "the clear button is a plain button with an accessible name, the hidden attribute and its hook: " + m);
        assertTrue(locals("search-input").contains("!{boolean clearHidden = !hasValue;}"), "it is hidden exactly while the field is empty");
        assertTrue(locals("search-input").contains("!{String inputClasses = \"ash-field__control ash-search-input__field\";}"));
        assertTrue(m.endsWith("<script type=\"module\" src=\"${AshlarAssets.js(\"search-input.js\")}\"></script>"), "the script is loaded for every Search Input, as before");
        assertFalse(m.contains("ash-field__control--readonly"), "a Search Input has no read-only presentation");
    }

    @Test
    void theClassesAreAshlarsAndNoDisplayTokenRemains() throws IOException {
        assertOnlyAshClasses("search-input");
        assertNoTailwindUtility("search-input");
        assertFalse(template("search-input").replaceAll("(?s)<%--.*?--%>", "").contains("clearDisplay"), "the flex/hidden class local is gone");
    }

    @Test
    void theScriptWritesOnlyTheNativeHiddenAttributeAndKeepsItsBehaviour() throws IOException {
        String js = script("search-input").replaceAll("(?m)^\\s*//.*$", "");
        assertFalse(js.contains("classList") || js.contains("className") || js.contains(".style") || js.contains("\"flex\""), "no class and no style is written");
        assertTrue(js.contains("clearButton.hidden = !hasValue;"));
        assertTrue(js.contains("input.addEventListener(\"input\", updateClearVisibility);"));
        assertTrue(js.contains("input.value = \"\";") && js.contains("input.dispatchEvent(new Event(\"input\", { bubbles: true }));") && js.contains("input.focus();"), "clearing still empties, announces and refocuses");
        assertTrue(js.contains("document.querySelectorAll(\"[data-search-input]\")"), "per-instance scoping is unchanged");
    }
}
