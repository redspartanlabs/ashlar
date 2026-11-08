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
 * Pins the source contract of Radio Group, migrated with the Choice Controls (PR 1). The template keeps every parameter, default, native attribute, ARIA wiring and id, and carries
 * only Ashlar classes. The parameters below are the ones the template declared BEFORE the migration (commit 0c26bc2), so this fails if the public contract moves. The rendered
 * result is measured in a real browser by the showcase tests ChoiceParityTest, ChoicePixelTest, ChoiceTailwindFreeTest and ChoiceBehaviourTest; this reads the sources only.
 */
class RadioGroupContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@import java.util.List",
                "@import dev.redspartanlabs.ashlar.radio.RadioOption",
                "@param String name",
                "@param String label",
                "@param List<RadioOption> options",
                "@param String selectedValue = \"\"",
                "@param boolean required = false",
                "@param boolean disabled = false",
                "@param String orientation = \"vertical\"",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\""), declared("radio-group"));
    }

    @Test
    void noTailwindUtilityRemainsAndOnlyAshlarClassesAreWritten() throws IOException {
        assertOnlyAshClasses("radio-group");
        assertNoTailwindUtility("radio-group");
    }

    @Test
    void theRadioGroupKeepsItsFieldsetLegendOptionsAndWiring() throws IOException {
        String m = markup("radio-group");
        assertTrue(m.startsWith("<fieldset class=\"ash-radio-group\" disabled=\"${disabled}\" aria-describedby=\"${describedByAttr}\"> <legend class=\"ash-radio-group__legend\"> ${label} @if (required)"
                + " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span> @endif </legend> <div class=\"${optionsLayoutClasses}\"> @for (RadioOption option : options)"), m);
        assertTrue(m.contains("<div class=\"ash-choice\"> <input type=\"radio\" id=\"${optionId}\" name=\"${name}\" value=\"${option.value()}\" checked=\"${isSelected}\" disabled=\"${option.disabled()}\""
                + " required=\"${required}\" aria-invalid=\"${ariaInvalidAttr}\" class=\"ash-choice__input\" > <div class=\"ash-choice__body\"> <label for=\"${optionId}\" class=\"ash-choice__label\">${option.label()}</label>"
                + " @if (hasDescription) <p class=\"ash-choice__description\">${option.description()}</p> @endif </div> </div> @endfor </div>"), m);
        assertTrue(m.contains("@if (hasHelp) <p id=\"${helpId}\" class=\"ash-choice__description ash-radio-group__help\">${helpText}</p> @endif"));
        assertTrue(m.contains("@if (showError) <p id=\"${errorId}\" class=\"ash-choice__error ash-radio-group__error\" role=\"alert\">${errorMessage}</p> @endif </fieldset>"));
        assertFalse(m.contains("<script"), "a Radio Group has no JavaScript: selection, arrow keys and required validation are native");
        String src = template("radio-group");
        assertTrue(src.contains("!{String optionId = groupId + \"-\" + option.value();}") && src.contains("!{boolean isSelected = option.value().equals(selectedValue);}")
                && src.contains("!{boolean hasDescription = option.description() != null && !option.description().isEmpty();}"), "the option locals are unchanged");
    }

    @Test
    void theDisabledStateIsNativeAndNoLabelOrDescriptionClassIsChosenInTheTemplateAnyMore() throws IOException {
        String src = template("radio-group").replaceAll("(?s)<%--.*?--%>", "");
        assertTrue(src.contains("disabled=\"${option.disabled()}\""), "an option is disabled by its own flag; the group by the fieldset's disabled attribute, which disables every radio natively");
        assertFalse(src.contains("optionDisabled") || src.contains("optionLabelClasses") || src.contains("optionDescriptionClasses") || src.contains("radioClasses"), "the Tailwind class locals are gone");
        String l = locals("radio-group");
        assertTrue(l.contains("!{String optionsLayoutClasses = isHorizontal ? \"ash-radio-group__options ash-radio-group__options--horizontal\" : \"ash-radio-group__options\";}"));
        assertTrue(l.contains("!{boolean isHorizontal = \"horizontal\".equalsIgnoreCase(orientation);}"));
        assertTrue(l.contains("!{String describedByAttr = hasHelp && showError ? helpId + \" \" + errorId : hasHelp ? helpId : showError ? errorId : null;}"));
        assertTrue(l.contains("!{String ariaInvalidAttr = error ? \"true\" : null;}"));
    }
}
