package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What File Upload DOES, in a real browser, with trusted input, in the legacy rendering and in the migrated one: every step below runs against both and the recorded results must be
 * identical, and each is also asserted against what a native file input and a label do (written from the platform, not from the old rendering).
 *
 * <p>Covered: a click on the card, on the field label and on the Choose File span opens the chooser (the real input receives one trusted click each; the chooser itself is intercepted, so no
 * operating-system dialog ever opens); a click on a disabled card does nothing; Tab reaches the input and shows the focus ring, Space and Enter each ask for the chooser, a disabled input is
 * skipped; a mouse click focuses the input but draws no ring; one file, several files (a count) and a long name; scripted clearing falls back to the idle text; a required input is invalid
 * without a file, shows no red border until a submit is blocked (then {@code :user-invalid} and the red border), and is valid, and not red, once a file is chosen; a reset clears the file but,
 * as before (D1), leaves the status text stale; a dropped file is not accepted (D1: no drag and drop); and the transitions and reduced motion of the card.
 *
 * <p>Limits, stated rather than hidden: the operating-system file dialog and a real picker cancellation cannot be driven (the chooser is intercepted, so the browser's own {@code cancel}
 * event never fires; a synthetic one is shown to be ignored, as the script has no handler for it); DevTools cannot clear a file input with an empty list (clearing is scripted: files
 * replaced by an empty list and a {@code change} event); a drop is dispatched through DevTools and is not an operating-system drag. Chrome only.
 */
class FileUploadBehaviourTest {

    private static final Map<String, String> LEGACY = new LinkedHashMap<>();
    private static final Map<String, String> MIGRATED = new LinkedHashMap<>();
    private static final Map<String, String> MOTION_LEGACY = new LinkedHashMap<>();
    private static final Map<String, String> MOTION_MIGRATED = new LinkedHashMap<>();

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static String zone(String id) {
        return "document.querySelector('[data-field=" + id + "] > label:nth-of-type(2)')";
    }

    private static String body(boolean legacy) throws Exception {
        String html = "<input id=\"before\" type=\"text\" aria-label=\"before\">"
                + "<form id=\"f1\" onsubmit=\"window.LOG.push('submit f1');return false\">" + FileUploadBrowser.marked(FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "r1", "required", true)))
                + "<button id=\"sub1\" type=\"submit\">Go</button><button id=\"rst1\" type=\"reset\">Reset</button></form>"
                + "<form id=\"f2\">" + FileUploadBrowser.marked(FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "s1"))) + FileUploadBrowser.marked(FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "m1", "multiple", true)))
                + FileUploadBrowser.marked(FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "d1", "disabled", true))) + "<button id=\"rst2\" type=\"reset\">Reset</button></form>"
                + "<input id=\"after\" type=\"text\" aria-label=\"after\">";
        return legacy ? new IconLegacyMarkup().rewrite(new ButtonLegacyMarkup().rewrite(new TextEntryLegacyMarkup().rewrite(new FileUploadLegacyMarkup().rewrite(html)))) : html;
    }

    private static final String HOOK = "<script>window.LOG=[];window.CNT={};['r1','s1','m1','d1'].forEach(function(id){var i=document.getElementById(id);CNT[id]={click:0,trusted:0,change:0,input:0,cancel:0};"
            + "i.addEventListener('click',function(e){CNT[id].click++;if(e.isTrusted)CNT[id].trusted++;e.preventDefault()});"
            + "i.addEventListener('change',function(){CNT[id].change++});i.addEventListener('input',function(){CNT[id].input++});i.addEventListener('cancel',function(){CNT[id].cancel++})});</script>";

    private static final String STATE = "JSON.stringify({active:document.activeElement.id||document.activeElement.tagName,cnt:CNT,fv:['r1','s1','m1'].map(function(i){return document.getElementById(i).matches(':focus-visible')})})";
    private static final String STATUS = "['r1','s1','m1','d1'].map(function(i){var r=document.querySelector('[data-field='+i+']');var f=document.getElementById(i).files;return i+':'+r.querySelector('[data-file-upload-status]').textContent+' (files='+f.length+')'}).join(' ; ')";

    private static void steps(DevToolsSession cdp, Map<String, String> out) throws Exception {
        cdp.call("Page.setInterceptFileChooserDialog", "{\"enabled\":true}");
        out.put("initial", ev(cdp, STATUS));
        TextEntryBrowser.click(cdp, "[data-field=s1] > label:nth-of-type(2)");
        out.put("click card s1", ev(cdp, STATE));
        out.put("ring after a mouse click", TextEntryBrowser.visibleShadow(ev(cdp, "getComputedStyle(" + zone("s1") + ").boxShadow")));
        TextEntryBrowser.click(cdp, "[data-field=s1] > label:nth-of-type(1)");
        out.put("click field label s1", ev(cdp, STATE));
        TextEntryBrowser.click(cdp, "[data-field=m1] label > span:last-child");
        out.put("click Choose File of m1", ev(cdp, STATE));
        TextEntryBrowser.click(cdp, "[data-field=d1] > label:nth-of-type(2)");
        out.put("click disabled card d1", ev(cdp, STATE));
        cdp.evaluate("(function(){document.getElementById('before').focus();return 'ok'})()");
        TextEntryBrowser.perform(cdp, "key:Tab");
        out.put("Tab from before", ev(cdp, STATE));
        out.put("ring after Tab on r1", TextEntryBrowser.visibleShadow(ev(cdp, "getComputedStyle(" + zone("r1") + ").boxShadow")));
        TextEntryBrowser.perform(cdp, "key:Space");
        out.put("Space on r1", ev(cdp, "JSON.stringify(CNT.r1)"));
        TextEntryBrowser.perform(cdp, "key:Enter");
        out.put("Enter on r1", ev(cdp, "JSON.stringify(CNT.r1)"));
        StringBuilder tabs = new StringBuilder();
        for (int k = 0; k < 6; k++) {
            TextEntryBrowser.perform(cdp, "key:Tab");
            tabs.append(ev(cdp, "document.activeElement.id||document.activeElement.tagName")).append(" > ");
        }
        out.put("Tab order from r1", tabs.toString());
        FileUploadBrowser.select(cdp, "#s1", List.of("report.pdf"));
        out.put("s1 one file", ev(cdp, STATUS) + " " + ev(cdp, "JSON.stringify(CNT.s1)"));
        FileUploadBrowser.select(cdp, "#m1", List.of("a.txt", "b.txt", "c.txt"));
        out.put("m1 three files", ev(cdp, STATUS));
        FileUploadBrowser.select(cdp, "#m1", List.of("a.txt"));
        out.put("m1 one file", ev(cdp, STATUS));
        out.put("m1 cleared by script", ev(cdp, "(function(){m1.files=new DataTransfer().files;m1.dispatchEvent(new Event('change',{bubbles:true}));return " + STATUS + "})()"));
        FileUploadBrowser.select(cdp, "#s1", List.of("a.txt", "b.txt"));
        out.put("s1 (not multiple) given two files", ev(cdp, STATUS));
        out.put("r1 before submit", ev(cdp, "JSON.stringify({valueMissing:r1.validity.valueMissing,valid:r1.validity.valid,userInvalid:r1.matches(':user-invalid'),checkValidity:document.getElementById('f1').checkValidity()})"));
        out.put("card border before submit", ev(cdp, "getComputedStyle(" + zone("r1") + ").borderTopColor"));
        TextEntryBrowser.click(cdp, "#sub1");
        out.put("r1 after a blocked submit", ev(cdp, "JSON.stringify({log:LOG,userInvalid:r1.matches(':user-invalid'),active:document.activeElement.id,msg:r1.validationMessage})"));
        out.put("card border after a blocked submit", ev(cdp, "getComputedStyle(" + zone("r1") + ").borderTopColor"));
        FileUploadBrowser.select(cdp, "#r1", List.of("report.pdf"));
        out.put("r1 after choosing", ev(cdp, "JSON.stringify({valid:r1.validity.valid,userInvalid:r1.matches(':user-invalid')})") + " border=" + ev(cdp, "getComputedStyle(" + zone("r1") + ").borderTopColor"));
        TextEntryBrowser.click(cdp, "#sub1");
        out.put("submit with a file", ev(cdp, "JSON.stringify(LOG)"));
        TextEntryBrowser.click(cdp, "#rst1");
        out.put("r1 after form reset", ev(cdp, STATUS));
        TextEntryBrowser.click(cdp, "#rst2");
        out.put("s1 and m1 after form reset", ev(cdp, STATUS));
        out.put("a synthetic cancel is ignored", ev(cdp, "(function(){var i=document.getElementById('s1');var a=document.querySelector('[data-field=s1] [data-file-upload-status]').textContent;i.dispatchEvent(new Event('cancel'));return a+' -> '+document.querySelector('[data-field=s1] [data-file-upload-status]').textContent})()"));
        String top = ev(cdp, "Math.round(" + zone("s1") + ".getBoundingClientRect().top+60)");
        String at = "\"x\":250,\"y\":" + top;
        String data = "\"data\":{\"items\":[],\"files\":[" + DevToolsSession.quote(FileUploadBrowser.file("report.pdf").toString()) + "],\"dragOperationsMask\":1}";
        cdp.call("Input.dispatchDragEvent", "{\"type\":\"dragEnter\"," + at + "," + data + "}");
        cdp.call("Input.dispatchDragEvent", "{\"type\":\"dragOver\"," + at + "," + data + "}");
        cdp.call("Input.dispatchDragEvent", "{\"type\":\"drop\"," + at + "," + data + "}");
        Thread.sleep(300);
        out.put("a file dropped on the card", ev(cdp, STATUS) + " href=" + ev(cdp, "location.href.substring(0,17)"));
    }

    private static String unfrozen(String page) {
        String freeze = "<style>*,*::before,*::after{transition:none !important;animation-play-state:paused !important}</style>";
        assertTrue(page.contains(freeze), "the shared page's transition freeze");
        return page.replace(freeze, "");
    }

    private static void motion(DevToolsSession cdp, String url, boolean reduced, Map<String, String> out, String prefix) throws Exception {
        cdp.call("Emulation.setEmulatedMedia", "{\"features\":[{\"name\":\"prefers-reduced-motion\",\"value\":\"" + (reduced ? "reduce" : "no-preference") + "\"}]}");
        cdp.navigate(url);
        out.put(prefix + " computed", ev(cdp, "(function(){var c=getComputedStyle(" + zone("m1") + ");return [c.transitionProperty,c.transitionDuration,c.transitionTimingFunction,c.transitionDelay].join(' | ')})()"));
        List<Integer> n = cdp.nodes("[data-field=m1] > label:nth-of-type(2)");
        String before = ev(cdp, "getComputedStyle(" + zone("m1") + ").borderTopColor");
        cdp.force(n, List.of("hover"));
        String immediately = ev(cdp, "getComputedStyle(" + zone("m1") + ").borderTopColor");
        Thread.sleep(450);
        String after = ev(cdp, "getComputedStyle(" + zone("m1") + ").borderTopColor");
        out.put(prefix + " before", before);
        out.put(prefix + " immediately", immediately);
        out.put(prefix + " after", after);
        cdp.force(n, List.of());
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("file-upload-behaviour");
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String scripts = HOOK + FileUploadBrowser.script();
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("file-upload-oracle.css")), config, "", body(true), scripts));
            String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", body(false), scripts));
            for (boolean migrated : List.of(false, true)) {
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    cdp.navigate(migrated ? migratedUrl : legacyUrl);
                    steps(cdp, migrated ? MIGRATED : LEGACY);
                }
            }
            String motionBody = FileUploadBrowser.marked(FileUploadBrowser.fileUpload(FileUploadBrowser.o("id", "m1")));
            String legacyMotion = unfrozen(TextEntryBrowser.page(List.of(ParityFrames.generated("file-upload-oracle.css")), config, "",
                    new IconLegacyMarkup().rewrite(new ButtonLegacyMarkup().rewrite(new TextEntryLegacyMarkup().rewrite(new FileUploadLegacyMarkup().rewrite(motionBody)))), ""));
            String migratedMotion = unfrozen(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", motionBody, ""));
            for (boolean reduced : List.of(false, true)) {
                String prefix = reduced ? "reduced" : "normal";
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    motion(cdp, server.registerRaw(legacyMotion), reduced, MOTION_LEGACY, prefix);
                }
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    motion(cdp, server.registerRaw(migratedMotion), reduced, MOTION_MIGRATED, prefix);
                }
            }
        }
    }

    @Test
    void everyStepGivesTheSameResultInTheLegacyAndTheMigratedRendering() {
        assertEquals(LEGACY.keySet(), MIGRATED.keySet());
        assertTrue(LEGACY.size() >= 25, "steps: " + LEGACY.size());
        for (String step : LEGACY.keySet()) {
            assertEquals(LEGACY.get(step), MIGRATED.get(step), step);
        }
    }

    @Test
    void aClickOnTheCardTheFieldLabelOrTheChooseFileSpanAsksTheRealInputForTheChooserAndNothingElseDoes() {
        Map<String, String> m = MIGRATED;
        assertTrue(m.get("click card s1").contains("\"active\":\"s1\"") && m.get("click card s1").contains("\"s1\":{\"click\":1,\"trusted\":1"), m.get("click card s1"));
        assertTrue(m.get("click field label s1").contains("\"s1\":{\"click\":2,\"trusted\":2"), "the field's own label forwards to the same input: " + m.get("click field label s1"));
        assertTrue(m.get("click Choose File of m1").contains("\"m1\":{\"click\":1,\"trusted\":1"), "the Choose File span takes no pointer events, so the click reaches the card's label: " + m.get("click Choose File of m1"));
        assertTrue(m.get("click disabled card d1").contains("\"d1\":{\"click\":0,") && m.get("click disabled card d1").contains("\"active\":\"BODY\""), "a disabled card does nothing: " + m.get("click disabled card d1"));
        assertEquals("none", TextEntryBrowser.visibleShadow(m.get("ring after a mouse click")), "a mouse click focuses the input but draws no ring");
        assertTrue(m.get("click card s1").contains("\"fv\":[false,false,false]"), "and it is not :focus-visible: " + m.get("click card s1"));
    }

    @Test
    void tabReachesTheInputWithTheFocusRingSpaceAndEnterAskForTheChooserAndADisabledInputIsSkipped() {
        Map<String, String> m = MIGRATED;
        assertTrue(m.get("Tab from before").contains("\"active\":\"r1\"") && m.get("Tab from before").contains("\"fv\":[true,false,false]"), m.get("Tab from before"));
        assertTrue(TextEntryBrowser.visibleShadow(m.get("ring after Tab on r1")).startsWith("rgb(255, 255, 255) 0px 0px 0px 2px, oklch(0.623 0.214 259.815) 0px 0px 0px 4px"), m.get("ring after Tab on r1"));
        assertTrue(m.get("Space on r1").startsWith("{\"click\":1,\"trusted\":1"), "Space: " + m.get("Space on r1"));
        assertTrue(m.get("Enter on r1").startsWith("{\"click\":2,\"trusted\":2"), "Enter: " + m.get("Enter on r1"));
        assertEquals("sub1 > rst1 > s1 > m1 > rst2 > after > ", m.get("Tab order from r1"), "the disabled input d1 is skipped");
    }

    @Test
    void oneFileSeveralFilesAndAnEmptySelectionSetTheStatusText() {
        Map<String, String> m = MIGRATED;
        assertTrue(m.get("initial").contains("s1:Choose a file to upload (files=0)") && m.get("initial").contains("m1:Choose files to upload (files=0)"), m.get("initial"));
        assertTrue(m.get("s1 one file").contains("s1:report.pdf (files=1)") && m.get("s1 one file").contains("\"change\":1,\"input\":1"), m.get("s1 one file"));
        assertTrue(m.get("m1 three files").contains("m1:3 files selected (files=3)"), m.get("m1 three files"));
        assertTrue(m.get("m1 one file").contains("m1:a.txt (files=1)"), "one file shows its name even when multiple: " + m.get("m1 one file"));
        assertTrue(m.get("m1 cleared by script").contains("m1:Choose files to upload (files=0)"), "no files: the idle text: " + m.get("m1 cleared by script"));
        assertTrue(m.get("s1 (not multiple) given two files").contains("s1:a.txt (files=1)"), "a single-file input keeps one file: " + m.get("s1 (not multiple) given two files"));
    }

    @Test
    void aRequiredInputIsInvalidWithoutAFileRedOnlyAfterABlockedSubmitAndValidAndNotRedOnceAFileIsChosen() {
        Map<String, String> m = MIGRATED;
        assertEquals("{\"valueMissing\":true,\"valid\":false,\"userInvalid\":false,\"checkValidity\":false}", m.get("r1 before submit"));
        assertEquals("oklch(0.869 0.022 252.894)", m.get("card border before submit"), "no red before the user has tried to submit");
        assertTrue(m.get("r1 after a blocked submit").contains("\"log\":[]") && m.get("r1 after a blocked submit").contains("\"userInvalid\":true") && m.get("r1 after a blocked submit").contains("\"active\":\"r1\"")
                && m.get("r1 after a blocked submit").contains("Please select a file."), "the submit is blocked, the input is :user-invalid and focused: " + m.get("r1 after a blocked submit"));
        assertEquals("oklch(0.704 0.191 22.216)", m.get("card border after a blocked submit"), ":user-invalid draws the red-400 border");
        assertTrue(m.get("r1 after choosing").startsWith("{\"valid\":true,\"userInvalid\":false}") && m.get("r1 after choosing").endsWith("border=oklch(0.869 0.022 252.894)"), m.get("r1 after choosing"));
        assertEquals("[\"submit f1\"]", m.get("submit with a file"), "a valid form submits");
    }

    @Test
    void aFormResetClearsTheFileButLeavesTheStatusTextStaleAsItAlwaysDidAndACancelEventAndADropChangeNothing() {
        Map<String, String> m = MIGRATED;
        assertTrue(m.get("r1 after form reset").contains("r1:report.pdf (files=0)"), "D1: the file is gone, the status is stale: " + m.get("r1 after form reset"));
        assertTrue(m.get("s1 and m1 after form reset").contains("s1:a.txt (files=0)") && m.get("s1 and m1 after form reset").contains("m1:Choose files to upload (files=0)"), m.get("s1 and m1 after form reset"));
        assertEquals("a.txt -> a.txt", m.get("a synthetic cancel is ignored"), "D1: the script has no cancel handling");
        assertTrue(m.get("a file dropped on the card").contains("s1:a.txt (files=0)") && m.get("a file dropped on the card").endsWith("href=http://127.0.0.1:"), "D1: no drag and drop: " + m.get("a file dropped on the card"));
    }

    @Test
    void theCardFadesItsColoursFor150MillisecondsAndAnimatesAndReducedMotionTurnsTheListOffInBothRenderings() {
        String list = "color, background-color, border-color, outline-color, text-decoration-color, fill, stroke, --tw-gradient-from, --tw-gradient-via, --tw-gradient-to | 0.15s | cubic-bezier(0.4, 0, 0.2, 1) | 0s";
        assertEquals(list, MOTION_MIGRATED.get("normal computed"));
        assertEquals(MOTION_LEGACY.get("normal computed"), MOTION_MIGRATED.get("normal computed"));
        assertEquals("none | 0.15s | cubic-bezier(0.4, 0, 0.2, 1) | 0s", MOTION_MIGRATED.get("reduced computed"));
        assertEquals(MOTION_LEGACY.get("reduced computed"), MOTION_MIGRATED.get("reduced computed"));
        for (Map<String, String> m : List.of(MOTION_LEGACY, MOTION_MIGRATED)) {
            assertNotEquals(m.get("normal before"), m.get("normal immediately"), "the colour has started to move");
            assertNotEquals(m.get("normal after"), m.get("normal immediately"), "and is not there yet");
            assertEquals("oklch(0.704 0.04 256.788)", m.get("normal after"), "and it ends at slate-400");
            assertEquals("oklch(0.704 0.04 256.788)", m.get("reduced immediately"), "with reduced motion the colour is there at once");
            assertFalse(m.get("reduced immediately").isEmpty());
        }
    }
}
