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
 * Pins the source contract of File Upload, migrated with the Choice Controls (PR 3). The template keeps every parameter, default, native attribute, ARIA wiring and id, the real file
 * input inside the card's label, the {@code data-*} hooks, the per-instance script tag and the decorative Choose File span, and carries only Ashlar classes. The parameters below are the
 * ones the template declared BEFORE the migration (commit 05fd7c8), so this fails if the public contract moves. The script is unchanged byte for byte in behavior: this pins its
 * shape (what it reads and writes, and what it never does). The rendered result is measured in a real browser by the showcase tests; this reads the sources only.
 */
class FileUploadContractTest {

    @Test
    void theParametersAndTheirDefaultsAreUnchanged() throws IOException {
        assertEquals(List.of(
                "@import dev.redspartanlabs.ashlar.icon.Icon",
                "@import dev.redspartanlabs.ashlar.button.ButtonStyles",
                "@import dev.redspartanlabs.ashlar.button.ButtonVariant",
                "@import dev.redspartanlabs.ashlar.button.ButtonSize",
                "@import dev.redspartanlabs.ashlar.AshlarAssets",
                "@param String name",
                "@param String label",
                "@param boolean multiple = false",
                "@param String accept = \"\"",
                "@param boolean required = false",
                "@param boolean disabled = false",
                "@param String id = \"\"",
                "@param String helpText = \"\"",
                "@param boolean error = false",
                "@param String errorMessage = \"\""), declared("file-upload"));
    }

    @Test
    void noTailwindUtilityRemainsAndOnlyAshlarClassesAreWritten() throws IOException {
        assertOnlyAshClasses("file-upload");
        assertNoTailwindUtility("file-upload");
        String m = template("file-upload").replaceAll("(?s)<%--.*?--%>", "");
        assertFalse(m.contains("has-[") || m.contains("pointer-events-none") || java.util.regex.Pattern.compile("(?<![\\w-])sr-only").matcher(m).find(), "no Tailwind variant, pointer-events or sr-only utility");
    }

    @Test
    void theRealFileInputStaysInsideTheCardsLabelWithItsAttributesHooksAndTheScriptTag() throws IOException {
        String m = markup("file-upload");
        assertEquals("<div class=\"ash-field ash-file-upload\" data-file-upload> <label for=\"${inputId}\" class=\"ash-field__label\"> ${label} @if (required)"
                + " <span class=\"ash-field__required\" aria-hidden=\"true\"> *</span> @endif </label>"
                + " <label class=\"ash-file-upload__zone\"> <input type=\"file\" id=\"${inputId}\" name=\"${name}\" multiple=\"${multiple}\" accept=\"${acceptAttr}\" required=\"${required}\""
                + " disabled=\"${disabled}\" aria-label=\"${label}\" aria-invalid=\"${ariaInvalidAttr}\" aria-describedby=\"${describedByAttr}\" data-file-upload-input"
                + " class=\"ash-file-upload__input ash-sr-only\" >"
                + " <span aria-hidden=\"true\" class=\"ash-file-upload__icon\"> @template.ashlar.components.icon(icon = Icon.UPLOAD, classes = \"ash-icon--lg\") </span>"
                + " <p data-file-upload-status data-idle-text=\"${idleText}\" class=\"ash-file-upload__status\">${idleText}</p>"
                + " <span class=\"${chooseButtonClasses} ash-file-upload__choose\">Choose File</span> </label>"
                + " @if (hasHelp) <p id=\"${helpId}\" class=\"ash-field__help\">${helpText}</p> @endif"
                + " @if (showError) <p id=\"${errorId}\" class=\"ash-field__error\" role=\"alert\">${errorMessage}</p> @endif </div>"
                + " <script type=\"module\" src=\"${AshlarAssets.js(\"file-upload.js\")}\"></script>", m);
        assertFalse(m.contains("role=\"switch\"") || m.contains("<button") || m.contains("ondrag") || m.contains("ondrop") || m.contains("aria-live"),
                "no second button, no drag and drop handler, no aria-live: the control and its semantics are unchanged");
    }

    @Test
    void theIdsTheDescribedByWiringTheInvalidFlagTheIdleTextAndTheChooseClassesAreComputedAsBefore() throws IOException {
        String l = locals("file-upload");
        assertTrue(l.contains("!{String inputId = id.isEmpty() ? name : id;}"));
        assertTrue(l.contains("!{String helpId = inputId + \"-help\";}") && l.contains("!{String errorId = inputId + \"-error\";}"));
        assertTrue(l.contains("!{boolean showError = error && !errorMessage.isEmpty();}"));
        assertTrue(l.contains("!{String describedByAttr = hasHelp && showError ? helpId + \" \" + errorId : hasHelp ? helpId : showError ? errorId : null;}"));
        assertTrue(l.contains("!{String ariaInvalidAttr = error ? \"true\" : null;}"), "aria-invalid follows the error flag, not the message");
        assertTrue(l.contains("!{String idleText = multiple ? \"Choose files to upload\" : \"Choose a file to upload\";}"));
        assertTrue(l.contains("!{String chooseButtonClasses = ButtonStyles.classes(ButtonVariant.SECONDARY, ButtonSize.SMALL, false, false, false);}"), "the Choose File span is a Secondary Small Button's classes");
        assertTrue(l.contains("!{String acceptAttr = accept.isEmpty() ? null : accept;}"));
    }

    @Test
    void theScriptIsTheOriginalOnlyReadsTheInputAndNeverAddsDragAndDropResetOrCancelHandling() throws IOException {
        String js = script("file-upload");
        assertTrue(js.contains("input.addEventListener(\"change\""), "it listens for change");
        assertTrue(js.contains("document.addEventListener(\"DOMContentLoaded\""), "it initialises each instance on DOMContentLoaded");
        assertFalse(js.contains("\"reset\"") || js.contains("\"cancel\"") || js.contains("drag") || js.contains("drop") || js.contains("aria-live") || js.contains("input.files ="),
                "D1/D6: no reset sync, no cancel handling, no drag and drop, no aria-live, and it never writes input.files");
        assertEquals(2, js.split("addEventListener", -1).length - 1, "exactly two listeners: change, and DOMContentLoaded");
    }
}
