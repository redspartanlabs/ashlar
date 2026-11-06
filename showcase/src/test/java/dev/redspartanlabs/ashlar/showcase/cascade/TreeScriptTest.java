package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real {@code tree.js} running against real Tree and Tree Item markup and the real {@code ashlar.css} in Chrome, driven by TRUSTED input from the DevTools protocol
 * ({@link TreeBrowser}): real key presses that carry {@code isTrusted === true}, move real focus and walk the real tab order, and real mouse clicks. The script is loaded
 * exactly as served (as a module, from a Blob). Nothing is mocked and no synthetic event is dispatched.
 *
 * <p>What it proves about the migration: the keyboard model is intact (one Tab stop that follows focus, Up and Down over the visible items only, Right and Left to expand,
 * collapse and move between parent and child, Home and End, Enter and Space), expansion is nothing but the two synchronised attributes (every step ends with
 * {@code aria-expanded} and {@code inert} in agreement and with what is drawn in agreement with them), a parent's state never changes its children's, two trees do not
 * touch each other, collapsed content cannot take focus, and a toggle writes no class and no inline style anywhere. {@link TreeLegacyBehaviourTest} runs the same sequence
 * against the original script and requires an identical result.
 *
 * <p>One behaviour is recorded here as it is and is a KNOWN ACCESSIBILITY DEFECT, not an intended contract: Enter or Space on an item that is both a parent and a link toggles
 * it and never follows the link, so a keyboard user cannot activate the link (a mouse click can). It is left unchanged by this migration and reported separately.
 */
class TreeScriptTest {

    private static List<TreeBrowser.Step> steps;
    private static List<String> transcript;
    private static List<String> trusted;
    private static List<String> untrusted;

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("tree-script");
        steps = TreeBrowser.steps();
        String script = TreeBrowser.script(TreeScriptTest.class, "/META-INF/resources/ashlar/js/tree.js");
        assertFalse(script.contains("</script"), "the script text can be embedded in a text/plain script element");
        String body = TreeBrowser.body(false, new TreeLegacyMarkup(), new IconLegacyMarkup());
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir()); DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
            cdp.navigate(server.registerRaw(TreeBrowser.page(List.of(ParityFrames.ASHLAR), body, script)));
            TreeBrowser.initialise(cdp);
            transcript = TreeBrowser.run(cdp, steps);
            trusted = List.of(cdp.evaluate("window.__trusted.join(',')").split(",", -1));
            untrusted = List.of(cdp.evaluate("window.__untrusted.join(',')").split(",", -1));
        }
    }

    private static List<String> fields(String snapshot) {
        return List.of(snapshot.split(";"));
    }

    private static String field(String snapshot, String name) {
        return fields(snapshot).stream().filter(f -> f.startsWith(name + "=")).findFirst().orElseThrow().substring(name.length() + 1);
    }

    @Test
    void everyStepEndsWithTheExpectedFocusAndTheExpectedState() {
        assertEquals(steps.size(), transcript.size());
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            TreeBrowser.Step step = steps.get(i);
            String snapshot = transcript.get(i);
            if (!field(snapshot, "active").equals(step.active())) {
                problems.add("#" + i + " " + step.action() + ": focus is " + field(snapshot, "active") + ", expected " + step.active() + " in " + snapshot);
            }
            for (String fragment : step.fragments()) {
                if (!snapshot.contains(fragment)) {
                    problems.add("#" + i + " " + step.action() + ": expected " + fragment + " in " + snapshot);
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void theInputWasTrustedBrowserInputAndNothingSyntheticWasDispatched() {
        long keys = steps.stream().filter(s -> s.action().startsWith("key:")).count();
        long clicks = steps.stream().filter(s -> s.action().startsWith("click:")).count();
        assertTrue(keys > 40 && clicks >= 5, "the sequence is a real keyboard and mouse session: " + keys + " keys, " + clicks + " clicks");
        assertEquals(List.of("click"), untrusted, "the only untrusted event is the script's own link.click() when Enter is pressed on a link leaf; no key press or mouse click was synthetic: " + untrusted);
        long trustedKeys = trusted.stream().filter(k -> !k.equals("click")).count();
        long trustedClicks = trusted.stream().filter(k -> k.equals("click")).count();
        assertEquals(keys, trustedKeys, "every key reached the page as isTrusted");
        assertTrue(trustedClicks >= clicks, "every mouse click reached the page as isTrusted: " + trusted);
    }

    @Test
    void aTabKeyWalksTheRealTabOrderThroughTheTreeOnItsOneTabStop() {
        // before -> a (the Tab stop), then a later Tab leaves the tree for the next button, Shift+Tab comes back to the item that holds the stop, and Tab reaches the second tree.
        assertEquals("a", field(transcript.get(2), "active"));
        int shiftTab = steps.indexOf(steps.stream().filter(s -> s.action().equals("key:Shift+Tab")).findFirst().orElseThrow());
        assertEquals("after", field(transcript.get(shiftTab - 1), "active"));
        assertEquals("f", field(transcript.get(shiftTab), "active"));
        assertEquals("x", field(transcript.get(shiftTab + 2), "active"));
        for (String snapshot : transcript) {
            assertTrue(field(snapshot, "tab").split(",").length == 2, "exactly one Tab stop per tree: " + snapshot);
        }
    }

    @Test
    void aRealKeyboardFocusShowsTheFocusRingAndTheStateStaysInAgreementAtEveryStep() {
        assertEquals("true", field(transcript.get(2), "fv"), "keyboard focus matches :focus-visible");
        for (int i = 0; i < transcript.size(); i++) {
            assertEquals("true", field(transcript.get(i), "sync"), "#" + i + " aria-expanded and inert agree: " + transcript.get(i));
            assertEquals("true", field(transcript.get(i), "visual"), "#" + i + " what is drawn (the chevron's turn, the panel's height) follows the attributes: " + transcript.get(i));
        }
    }

    @Test
    void aToggleWritesNoClassAndNoInlineStyleAnywhereInEitherTree() {
        String first = field(transcript.get(0), "cls");
        for (int i = 0; i < transcript.size(); i++) {
            assertEquals(first, field(transcript.get(i), "cls"), "#" + i + " " + steps.get(i).action() + ": a class or style attribute changed");
        }
        int toggles = 0;
        for (int i = 1; i < transcript.size(); i++) {
            if (!field(transcript.get(i), "exp").equals(field(transcript.get(i - 1), "exp"))) {
                toggles++;
            }
        }
        assertEquals(17, toggles, "the sequence expands or collapses an item seventeen times, and not one of them wrote a class or a style");
    }

    @Test
    void aParentsStateNeverChangesItsChildrensAndTheTwoTreesNeverTouchEachOther() {
        // Collapsing a left b expanded (and d collapsed); expanding it again restored exactly that.
        int collapseA = indexOf("key:ArrowLeft", "a", "a:false");
        assertTrue(transcript.get(collapseA).contains("b:true") && transcript.get(collapseA).contains("d:false"), transcript.get(collapseA));
        // Collapsing and re-expanding g left h as it was (collapsed by the earlier ArrowLeft).
        int spaceClose = indexOf("key:Space", "g", "g:false");
        assertTrue(transcript.get(spaceClose).contains("h:false"), transcript.get(spaceClose));
        // Everything done in the second tree changed nothing in the first.
        int firstInTwo = indexOf("key:ArrowRight", "x", "x:true");
        String before = field(transcript.get(firstInTwo - 1), "exp").replaceAll(",x:[a-z]+$", "");
        for (int i = firstInTwo; i < transcript.size(); i++) {
            assertEquals(before, field(transcript.get(i), "exp").replaceAll(",x:[a-z]+$", ""), "#" + i + " " + steps.get(i).action() + " changed the first tree");
        }
    }

    @Test
    void collapsedContentCannotTakeFocusEvenFromAScript() {
        int blocked = indexOf("focusjs:e", "d", "d:false");
        assertEquals("d", field(transcript.get(blocked), "active"));
        assertEquals("d", field(transcript.get(blocked - 1), "active"));
        int blockedTwo = steps.size() - 1;
        assertEquals("focusjs:y", steps.get(blockedTwo).action());
        assertEquals("x", field(transcript.get(blockedTwo), "active"));
    }

    @Test
    void arrowKeysOnlyEverVisitVisibleItemsInDocumentOrder() {
        Set<String> visited = new TreeSet<>();
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).action().startsWith("key:Arrow")) {
                visited.add(field(transcript.get(i), "active"));
            }
        }
        // e and n are only ever inside a collapsed branch when an arrow key could have reached them; e is reached once (d was expanded for that step) and n never.
        assertFalse(visited.contains("n"), "n lives under the collapsed m and is never visited: " + visited);
        assertTrue(visited.containsAll(Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "k", "m", "x", "y")), visited.toString());
        int down = indexOf("key:ArrowDown", "f", "");
        assertEquals("d", field(transcript.get(down - 1), "active"), "from d (collapsed, with e inside) the next item down is f, not e");
    }

    @Test
    void anItemThatIsBothAParentAndALinkTogglesAndNeverFollowsItsLinkFromTheKeyboardAKnownAccessibilityDefect() {
        int enter = indexOf("key:Enter", "m", "m:true");
        assertEquals("#c", field(transcript.get(enter), "hash"), "KNOWN DEFECT (not a contract): Enter toggles m and does not follow m's link, so the hash is still the one from the earlier link");
        assertNotEquals("#m", field(transcript.get(enter), "hash"));
        int space = steps.indexOf(steps.stream().filter(s -> s.action().equals("key:Space") && s.active().equals("k")).findFirst().orElseThrow());
        assertEquals(field(transcript.get(space - 1), "hash"), field(transcript.get(space), "hash"), "Space on a plain leaf does nothing");
        int link = indexOf("key:Enter", "c", "hash=#c");
        assertEquals("#c", field(transcript.get(link), "hash"), "Enter on a link leaf follows its link");
    }

    @Test
    void aMouseClickOnTheToggleTogglesAndAClickOnTheRowOrALabelDoesNot() {
        int closeG = indexOf("click:g-toggle", "g", "g:false");
        assertTrue(transcript.get(closeG).contains("g:false") && field(transcript.get(closeG - 1), "exp").contains("g:true"));
        int labelClick = indexOf("click:a-label", "a", "a:true");
        assertEquals(field(transcript.get(labelClick - 1), "exp"), field(transcript.get(labelClick), "exp"), "a click on a label toggles nothing");
        int linkClick = indexOf("click:f-label", "f", "hash=#f");
        assertEquals(field(transcript.get(linkClick - 1), "exp"), field(transcript.get(linkClick), "exp"), "a click on a link follows it and toggles nothing");
        assertEquals("f", field(transcript.get(linkClick), "tab").split(",")[0], "and the click moved the Tab stop to its item");
    }

    private static int indexOf(String action, String active, String fragment) {
        for (int i = 0; i < steps.size(); i++) {
            TreeBrowser.Step step = steps.get(i);
            if (step.action().equals(action) && step.active().equals(active) && (fragment.isEmpty() || List.of(step.fragments()).contains(fragment))) {
                return i;
            }
        }
        throw new IllegalStateException("no step " + action + " -> " + active + " " + fragment);
    }
}
