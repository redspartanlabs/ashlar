package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rewrite that turns migrated File Upload markup back into the former Tailwind markup: what it changes, what it must leave to Text Entry (the field's label, marker, help and error),
 * what it must leave alone, that it fails loudly, that the legacy fixture is grounded in the original template, and that the markup the parity suites render (the mirror in
 * {@link FileUploadBrowser}) writes exactly the classes the shipped template writes.
 */
class FileUploadLegacyMarkupTest {

    @Test
    void anUploadGetsEachOfItsLegacyStringsTheIconsSizeAndTheChooseSpansUtilityAndTheFieldPartsAreLeftToTextEntry() throws Exception {
        FileUploadLegacyMarkup l = new FileUploadLegacyMarkup();
        String out = l.rewrite(FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "f", "required", true, "help", "h", "error", true, "errMsg", "e")));
        for (String role : List.of("root", "zone", "input", "icon", "status")) {
            assertTrue(out.contains("class=\"" + l.classes(role) + "\""), role + " in " + out);
        }
        assertTrue(out.contains("<svg class=\"ash-icon " + l.classes("icon-svg") + "\""), "the Icon keeps ash-icon and gets the size classes it was given: " + out);
        assertTrue(out.contains("class=\"ash-btn ash-btn--secondary ash-btn--sm " + l.classes("choose") + "\""), out);
        assertFalse(out.contains("ash-file-upload") || out.contains("ash-sr-only") || out.contains("ash-icon--lg"), out);
        for (String own : List.of("ash-field__label", "ash-field__required", "ash-field__help", "ash-field__error")) {
            assertTrue(out.contains("class=\"" + own + "\""), own + " is Text Entry's to rewrite: " + out);
        }
        assertTrue(out.startsWith("<div class=\"mb-6\" data-file-upload"), "the root is the field's own mb-6: " + out);
    }

    @Test
    void theFieldPartsLegacyStringsAreExactlyTextEntrys() throws Exception {
        FileUploadLegacyMarkup l = new FileUploadLegacyMarkup();
        TextEntryLegacyMarkup t = new TextEntryLegacyMarkup();
        assertEquals(t.classes("field"), l.classes("root"), "the root's mb-6 is the field's");
        for (String role : List.of("label", "required", "help", "error")) {
            assertEquals(t.classes(role), l.classes(role), role + ": D7, reusing ash-field__" + role + " is the same Tailwind");
        }
    }

    @Test
    void otherMarkupIsLeftExactlyAsServedAndAnUnknownClassFailsLoudly() throws Exception {
        String other = "<div class=\"ash-card\"><span class=\"ash-sr-only\">x</span><a class=\"ash-file-uploads\">y</a><svg class=\"ash-icon ash-icon--lg\"></svg><span class=\"ash-btn ash-btn--secondary ash-btn--sm\"></span></div>";
        assertEquals(other, new FileUploadLegacyMarkup().rewrite(other));
        assertThrows(IllegalStateException.class, () -> new FileUploadLegacyMarkup().rewrite("<span class=\"ash-file-upload__unknown\"></span>"));
        assertThrows(IllegalStateException.class, () -> new FileUploadLegacyMarkup().rewrite("<span class=\"ash-file-upload__zone ash-file-upload__status\"></span>"));
        assertThrows(IllegalStateException.class, () -> new FileUploadLegacyMarkup().rewrite("<input class=\"ash-file-upload__input\">"), "the hidden input always carries ash-sr-only");
        assertThrows(IllegalStateException.class, () -> new FileUploadLegacyMarkup().rewrite("<span class=\"ash-file-upload__choose\"></span>"), "the Choose File span always carries the Secondary Small Button's classes");
        assertThrows(IllegalStateException.class, () -> new FileUploadLegacyMarkup().rewrite("<span class=\"ash-file-upload__icon\"></span><svg class=\"ash-icon\"></svg>"), "the Icon in the wrapper is always large");
    }

    @Test
    void theLegacyFixtureIsGroundedInTheOriginalTemplateAndHasEveryTokenTheComparisonRelies() throws Exception {
        FileUploadLegacyMarkup l = new FileUploadLegacyMarkup();
        assertTrue(l.allTokens().containsAll(List.of("flex", "w-full", "max-w-sm", "cursor-pointer", "flex-col", "items-center", "gap-3", "rounded-lg", "border-2", "border-dashed", "border-slate-300",
                "bg-slate-50", "px-6", "py-8", "text-center", "transition-colors", "motion-reduce:transition-none", "hover:border-slate-400", "hover:bg-slate-100", "has-[:focus-visible]:ring-2",
                "has-[:focus-visible]:ring-offset-2", "has-[:disabled]:cursor-not-allowed", "has-[:disabled]:opacity-60", "has-[:disabled]:hover:border-slate-300", "has-[:user-invalid]:border-red-400",
                "has-[[aria-invalid=true]]:border-red-400", "dark:bg-slate-800/50", "dark:hover:bg-slate-800", "dark:has-[:disabled]:hover:bg-slate-800/50", "dark:has-[:user-invalid]:border-red-500",
                "sr-only", "h-6", "w-6", "truncate", "max-w-full", "pointer-events-none", "mb-6", "mb-2", "mt-1")));
        assertEquals("mb-6", l.classes("root"));
        assertEquals("pointer-events-none", l.classes("choose"));
        assertEquals("h-6 w-6", l.classes("icon-svg"));
    }

    // ---- the mirror

    private static final Pattern OWN = Pattern.compile("(?<![\\w-])ash-(?:field(?:__[a-z-]+)?|file-upload(?:__[a-z-]+)?|sr-only|icon--lg)(?![\\w-])");

    private static Set<String> ashClasses(String text) {
        Set<String> found = new TreeSet<>();
        Matcher m = OWN.matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    private static String shipped() throws Exception {
        try (InputStream in = FileUploadLegacyMarkupTest.class.getResourceAsStream("/ashlar/components/file-upload.jte")) {
            assertTrue(in != null, "file-upload.jte is not on the test classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", "");
        }
    }

    /** The parity suites render markup written by {@link FileUploadBrowser}, which mirrors the template: the Ashlar classes it writes are exactly the ones the mirror writes. */
    @Test
    void theMirrorWritesExactlyTheClassesTheShippedTemplateWrites() throws Exception {
        assertEquals(ashClasses(shipped()), ashClasses(FileUploadBrowser.fileUpload(FileUploadBrowser.o("required", true, "help", "h", "error", true, "errMsg", "e"))));
    }

    @Test
    void theMirrorAndTheTemplateAgreeOnTheDomShapeAndTheChooseSpanIsTheButtonsOwnClasses() throws Exception {
        String shape = shipped().replaceAll("(?m)^@(?:param|import) .*$", "").replaceAll("(?m)^!\\{.*\\}$", "").replaceAll("\\s+", " ");
        for (String fragment : List.of("<div class=\"ash-field ash-file-upload\" data-file-upload>", "<label class=\"ash-file-upload__zone\">", "class=\"ash-file-upload__input ash-sr-only\"",
                "<span aria-hidden=\"true\" class=\"ash-file-upload__icon\">", "class=\"ash-file-upload__status\"", "<span class=\"${chooseButtonClasses} ash-file-upload__choose\">Choose File</span>")) {
            assertTrue(shape.contains(fragment), fragment);
        }
        String mirror = FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "f"));
        assertTrue(mirror.contains("<span class=\"ash-btn ash-btn--secondary ash-btn--sm ash-file-upload__choose\">Choose File</span>"), mirror);
        assertTrue(mirror.indexOf("ash-file-upload__input") < mirror.indexOf("ash-file-upload__icon") && mirror.indexOf("ash-file-upload__icon") < mirror.indexOf("ash-file-upload__status")
                && mirror.indexOf("ash-file-upload__status") < mirror.indexOf("ash-file-upload__choose"), "the input, the icon, the status, then Choose File, all inside the card's label");
    }

    @Test
    void theRealPageProbeNamesNoFileUploadElement() throws Exception {
        String probe = new String(FileUploadLegacyMarkupTest.class.getResourceAsStream("/cascade/real-page-probe.js").readAllBytes(), StandardCharsets.UTF_8);
        assertFalse(probe.contains("file-upload"), "the probe is unchanged by this migration and names no File Upload element");
    }
}
