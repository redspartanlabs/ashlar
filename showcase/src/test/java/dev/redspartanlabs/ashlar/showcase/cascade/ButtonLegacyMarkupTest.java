package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.button.ButtonSize;
import dev.redspartanlabs.ashlar.button.ButtonVariant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rewrite that turns migrated Button markup back into the former Tailwind markup: what it changes, what it must leave alone, and that it fails loudly. */
class ButtonLegacyMarkupTest {

    private static final String SPINNER = "<span data-btn-spinner class=\"ash-btn__spinner\" aria-hidden=\"true\"></span>";

    private static String button(String classes, String extra, String inner) {
        return "<button type=\"button\" class=\"" + classes + "\" data-btn data-btn-variant=\"primary\"" + extra + ">" + inner + "</button>";
    }

    @Test
    void theButtonElementGetsExactlyTheFormerButtonStylesStringForItsVariantSizeAndState() throws Exception {
        ButtonLegacyMarkup legacy = new ButtonLegacyMarkup();
        String out = legacy.rewrite(button("ash-btn ash-btn--danger ash-btn--lg ash-btn--icon ash-btn--block", "", ""));
        String expected = ButtonLegacyStyles.classes(ButtonVariant.DANGER, ButtonSize.LARGE, true, true, false);
        assertTrue(out.contains("class=\"" + expected + "\""), out);
        assertTrue(out.contains("data-btn data-btn-variant=\"primary\""), "the hooks are left as served");
    }

    @Test
    void pressedIsReadFromTheModifierClassOrFromAriaPressed() throws Exception {
        ButtonLegacyMarkup legacy = new ButtonLegacyMarkup();
        String expected = ButtonLegacyStyles.classes(ButtonVariant.SECONDARY, ButtonSize.MEDIUM, false, false, true);
        String modifier = legacy.rewrite(button("ash-btn ash-btn--secondary ash-btn--md ash-btn--pressed", "", ""));
        assertTrue(modifier.contains("class=\"" + expected + " ash-btn--pressed\""), "the Foundry hook survives the rewrite: " + modifier);
        assertTrue(legacy.rewrite(button("ash-btn ash-btn--secondary ash-btn--md", " aria-pressed=\"true\"", "")).contains("class=\"" + expected + "\""));
        String resting = ButtonLegacyStyles.classes(ButtonVariant.SECONDARY, ButtonSize.MEDIUM, false, false, false);
        assertTrue(legacy.rewrite(button("ash-btn ash-btn--secondary ash-btn--md", " aria-pressed=\"false\"", "")).contains("class=\"" + resting + "\""));
        assertFalse(expected.equals(resting));
    }

    @Test
    void theSpinnerAndTheIconFollowTheButtonsLoadingStateAsTheOldTemplateDid() throws Exception {
        ButtonLegacyMarkup legacy = new ButtonLegacyMarkup();
        String inner = SPINNER + "<span data-btn-icon class=\"ash-btn__icon\" aria-hidden=\"true\"></span><span data-btn-label class=\"ash-btn__label\">Save</span>";
        String idle = legacy.rewrite(button("ash-btn ash-btn--primary ash-btn--md", "", inner));
        assertTrue(idle.contains("class=\"" + legacy.part("spinner-hidden") + "\""), idle);
        assertFalse(idle.contains("ash-btn__"), "every part is rewritten: " + idle);
        assertTrue(idle.contains("<span data-btn-icon aria-hidden=\"true\">"), "an idle icon wrapper had no class: " + idle);
        assertTrue(idle.contains("<span data-btn-label>Save"), "the label had no class: " + idle);
        String busy = legacy.rewrite(button("ash-btn ash-btn--primary ash-btn--md", " aria-busy=\"true\" aria-disabled=\"true\"", inner));
        assertTrue(busy.contains("class=\"" + legacy.part("spinner-shown") + "\""), busy);
        assertTrue(busy.contains("<span data-btn-icon class=\"hidden\" aria-hidden=\"true\">"), busy);
        // The state does not leak into the next button.
        String after = legacy.rewrite(button("ash-btn ash-btn--primary ash-btn--md", " aria-busy=\"true\"", inner) + button("ash-btn ash-btn--primary ash-btn--md", "", inner));
        assertTrue(after.lastIndexOf(legacy.part("spinner-hidden")) > after.indexOf(legacy.part("spinner-shown")), after);
    }

    @Test
    void screenReaderTextBecomesTheTailwindUtilityAndExtraClassesOnTheButtonSurvive() throws Exception {
        ButtonLegacyMarkup legacy = new ButtonLegacyMarkup();
        String out = legacy.rewrite("<span class=\"ash-sr-only ash-btn__label\">Close</span><span class=\"ash-sr-only ash-btn__status\" data-btn-status></span>"
                + "<span class=\"ash-btn ash-btn--secondary ash-btn--sm pointer-events-none\">Choose File</span>");
        assertEquals(2, out.split("class=\"sr-only\"", -1).length - 1, out);
        assertTrue(out.contains(ButtonLegacyStyles.classes(ButtonVariant.SECONDARY, ButtonSize.SMALL, false, false, false) + " pointer-events-none\""), out);
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownButtonClassFailsLoudly() throws Exception {
        ButtonLegacyMarkup legacy = new ButtonLegacyMarkup();
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-btnish\">y</a></div>";
        assertEquals(other, legacy.rewrite(other));
        assertThrows(IllegalStateException.class, () -> legacy.rewrite("<span class=\"ash-btn__unknown\"></span>"));
    }

    @Test
    void everyLegacyTokenTheCasesCanProduceIsCountedForTheOraclesNonVacuityCheck() throws Exception {
        ButtonLegacyMarkup legacy = new ButtonLegacyMarkup();
        assertTrue(legacy.allTokens().containsAll(java.util.List.of("bg-blue-600", "hover:bg-blue-700", "dark:hover:bg-slate-700", "aria-disabled:opacity-60", "w-full",
                "h-14", "shadow-inner", "animate-spin", "sr-only", "hidden", "inline-block")));
    }

    @Test
    void countsOnlyRealButtons() {
        assertEquals(2, ButtonLegacyMarkup.countButtons(button("ash-btn ash-btn--primary ash-btn--md", "", "") + button("ash-btn ash-btn--link ash-btn--xl ash-btn--icon", "", "")
                + "<span class=\"ash-btn__icon\"></span>"));
    }
}
