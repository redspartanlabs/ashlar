package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The real {@code tabs.js} running against real Tabs and Tab Panel markup and the real {@code ashlar.css} in Chrome: selection by click, automatic activation by
 * Arrow Left/Right/Home/End (with wrapping and focus movement), disabled tabs skipped and inert, independent groups, and the reconcile at init when a panel's own
 * {@code active} disagrees with the tab the server rendered as selected. The script is loaded exactly as served (as a module, from a Blob), and every check reads the DOM
 * and the computed style the browser produced. Nothing is mocked.
 *
 * <p>What it proves about the migration: selecting a tab changes attributes only. Every scenario compares the {@code class} of every tab, panel and tablist before and
 * after (it must not change), the selected look (colour and border) swaps because the stylesheet answers {@code aria-selected}, and a panel is shown and hidden through the
 * native {@code hidden} attribute and stays hidden even against another declaration in the same layer. Enter and Space on a focused tab are the native activation of a
 * {@code <button>} (a click), which the click scenarios drive; a browser cannot be handed a trusted key press here.
 */
class TabsScriptTest {

    private static final Map<String, String[]> RESULTS = new LinkedHashMap<>();

    private static final List<String> EXPECTED = List.of(
            "init reconciles every panel to the tab rendered as selected", "init leaves the classes of every tab, panel and list alone",
            "a click selects the tab, sets the roving tabindex and swaps the panels", "a click swaps the selected look through aria-selected alone", "a click changes no class",
            "a hidden panel is display none and the visible one is shown", "a hidden panel stays hidden against another display declaration in the same layer",
            "Arrow Right selects the next tab and moves focus to it", "Arrow Right wraps from the last tab to the first", "Arrow Left selects the previous tab and moves focus",
            "Arrow Left wraps from the first tab to the last", "Home selects the first enabled tab", "End selects the last enabled tab", "a disabled tab is skipped by the arrows",
            "Home and End skip a disabled first and last tab", "a click on a disabled tab does nothing", "a key the tabs do not handle changes nothing", "groups are independent",
            "keyboard selection changes no class", "the focused tab is the selected tab (automatic activation)", "the arrows from outside any tab start at the first");

    private static final String SCRIPT = """
            (async function () {
              var out = [];
              function rec(name, ok, detail) { out.push([name, ok ? 'ok' : 'FAIL', detail || ''].join('\\t')); }
              function $(id) { return document.getElementById(id); }
              function cs(el, p) { return getComputedStyle(el).getPropertyValue(p); }
              function allClasses() { return [].map.call(document.querySelectorAll('[data-tabs],[data-tabs-list],[data-tab],[data-tab-panel],[data-tab-id]'), function (e) { return e.id + ':' + (e.getAttribute('class') || ''); }).join('|'); }
              function sel(groupId) { var t = document.querySelector('#' + groupId + ' [aria-selected="true"]'); return t ? t.getAttribute('data-tab-id') : null; }
              function shown(groupId) { return [].filter.call(document.querySelectorAll('[data-tab-panel][data-tabs-id="' + groupId + '"]'), function (p) { return !p.hidden && cs(p, 'display') !== 'none'; }).map(function (p) { return p.getAttribute('data-tab-id'); }).join(','); }
              function tabindexes(groupId) { return [].map.call(document.querySelectorAll('#' + groupId + ' [data-tab]'), function (t) { return t.getAttribute('data-tab-id') + '=' + t.getAttribute('tabindex'); }).join(','); }
              function key(el, k) { el.dispatchEvent(new KeyboardEvent('keydown', { key: k, bubbles: true, cancelable: true })); }
              function tab(groupId, id) { return document.querySelector('#' + groupId + ' [data-tab-id="' + id + '"]'); }
              try {
                var src = document.getElementById('src').textContent;
                await import(URL.createObjectURL(new Blob([src], { type: 'text/javascript' })));
                var before = allClasses();
                // The server rendered group A with tab b selected, but panel a visible and panel b hidden: the script reconciles at init.
                var preA = shown('A');
                document.dispatchEvent(new Event('DOMContentLoaded'));
                rec('init reconciles every panel to the tab rendered as selected', preA === 'a' && sel('A') === 'b' && shown('A') === 'b' && tabindexes('A') === 'a=-1,b=0,c=-1', 'before=' + preA + ' after=' + shown('A') + ' ' + tabindexes('A'));
                rec('init leaves the classes of every tab, panel and list alone', allClasses() === before, 'classes changed at init');
                var start = allClasses();

                // click
                var selectedColor = cs(tab('A', 'b'), 'color'), selectedBorder = cs(tab('A', 'b'), 'border-bottom-color'), plainColor = cs(tab('A', 'a'), 'color');
                tab('A', 'c').click();
                rec('a click selects the tab, sets the roving tabindex and swaps the panels', sel('A') === 'c' && tabindexes('A') === 'a=-1,b=-1,c=0' && shown('A') === 'c', sel('A') + ' ' + tabindexes('A') + ' ' + shown('A'));
                rec('a click swaps the selected look through aria-selected alone', cs(tab('A', 'c'), 'color') === selectedColor && cs(tab('A', 'c'), 'border-bottom-color') === selectedBorder && cs(tab('A', 'b'), 'color') === plainColor
                    && cs(tab('A', 'b'), 'border-bottom-color') === 'rgba(0, 0, 0, 0)' && selectedColor !== plainColor, 'c=' + cs(tab('A', 'c'), 'color') + ' b=' + cs(tab('A', 'b'), 'color'));
                rec('a click changes no class', allClasses() === start, 'classes changed');
                var panelB = $('A-panel-b'), panelC = $('A-panel-c');
                rec('a hidden panel is display none and the visible one is shown', cs(panelB, 'display') === 'none' && panelB.hidden && cs(panelC, 'display') !== 'none' && !panelC.hidden, cs(panelB, 'display') + ' ' + cs(panelC, 'display'));
                var layered = document.createElement('style'); layered.textContent = '@layer ash.components { .ash-tab-panel { display: flex; } }'; document.head.appendChild(layered);
                rec('a hidden panel stays hidden against another display declaration in the same layer', cs(panelB, 'display') === 'none' && cs(panelC, 'display') === 'flex', cs(panelB, 'display') + ' ' + cs(panelC, 'display'));
                layered.remove();

                // arrows (automatic activation)
                tab('A', 'c').focus();
                key(tab('A', 'c'), 'ArrowRight');
                rec('Arrow Right wraps from the last tab to the first', sel('A') === 'a' && shown('A') === 'a' && document.activeElement === tab('A', 'a'), sel('A') + ' ' + shown('A'));
                key(tab('A', 'a'), 'ArrowRight');
                rec('Arrow Right selects the next tab and moves focus to it', sel('A') === 'b' && document.activeElement === tab('A', 'b') && shown('A') === 'b', sel('A') + ' ' + shown('A'));
                key(tab('A', 'b'), 'ArrowLeft');
                rec('Arrow Left selects the previous tab and moves focus', sel('A') === 'a' && document.activeElement === tab('A', 'a'), sel('A'));
                key(tab('A', 'a'), 'ArrowLeft');
                rec('Arrow Left wraps from the first tab to the last', sel('A') === 'c' && document.activeElement === tab('A', 'c') && shown('A') === 'c', sel('A'));
                key(tab('A', 'c'), 'Home');
                var home = sel('A') === 'a';
                key(tab('A', 'a'), 'End');
                rec('Home selects the first enabled tab', home, 'home=' + home);
                rec('End selects the last enabled tab', sel('A') === 'c' && shown('A') === 'c', sel('A'));
                rec('the focused tab is the selected tab (automatic activation)', document.activeElement === tab('A', 'c') && document.activeElement.getAttribute('aria-selected') === 'true', String(document.activeElement && document.activeElement.id));

                // disabled
                tab('B', 'one').focus();
                key(tab('B', 'one'), 'ArrowRight');
                var skipped = sel('B') === 'three' && document.activeElement === tab('B', 'three');
                key(tab('B', 'three'), 'ArrowLeft');
                rec('a disabled tab is skipped by the arrows', skipped && sel('B') === 'one', 'skipped=' + skipped + ' back=' + sel('B'));
                tab('C', 'two').focus();
                key(tab('C', 'two'), 'Home'); var h = sel('C');
                key(tab('C', 'two'), 'End'); var e2 = sel('C');
                rec('Home and End skip a disabled first and last tab', h === 'two' && e2 === 'three', 'home=' + h + ' end=' + e2);
                var beforeDisabled = sel('B'); tab('B', 'two').click();
                rec('a click on a disabled tab does nothing', sel('B') === beforeDisabled && tab('B', 'two').getAttribute('aria-selected') === 'false' && shown('B') === beforeDisabled, sel('B'));
                var s1 = sel('A'), f1 = document.activeElement; key(tab('A', 'c'), 'ArrowDown'); key(tab('A', 'c'), 'a'); key(tab('A', 'c'), 'Enter');
                rec('a key the tabs do not handle changes nothing', sel('A') === s1 && document.activeElement === f1, sel('A'));
                // groups
                var aBefore = sel('A'); tab('B', 'three').click();
                rec('groups are independent', sel('A') === aBefore && shown('A') === aBefore && sel('B') === 'three' && shown('B') === 'three', 'A=' + sel('A') + ' B=' + sel('B'));
                // outside: focus not on a tab -> ArrowRight starts at the first enabled tab
                document.body.focus(); tab('D', 'x').blur();
                key(document.querySelector('#D [data-tabs-list]'), 'ArrowRight');
                rec('the arrows from outside any tab start at the first', sel('D') === 'x' && shown('D') === 'x', sel('D'));
                rec('keyboard selection changes no class', allClasses() === start, 'classes changed');
              } catch (e) {
                rec('SCRIPT ERROR', false, String(e && e.stack || e));
              }
              var pre = document.createElement('pre'); pre.id = 'ash-tabs-js'; pre.textContent = out.join('\\n'); document.body.appendChild(pre);
            })();
            """;

    private static String tab(String group, String id, String label, boolean selected, boolean disabled) {
        return "<button type=\"button\" id=\"" + group + "-tab-" + id + "\" data-tab data-tab-id=\"" + id + "\" role=\"tab\" aria-selected=\"" + selected + "\" aria-controls=\"" + group + "-panel-" + id
                + "\" tabindex=\"" + (selected ? "0" : "-1") + "\"" + (disabled ? " disabled" : "") + " class=\"ash-tabs__tab\"><span class=\"ash-tabs__label\">" + label + "</span></button>";
    }

    private static String panel(String group, String id, boolean visible) {
        return "<div id=\"" + group + "-panel-" + id + "\" data-tab-panel data-tabs-id=\"" + group + "\" data-tab-id=\"" + id + "\" role=\"tabpanel\" aria-labelledby=\"" + group + "-tab-" + id
                + "\" tabindex=\"0\"" + (visible ? "" : " hidden") + " class=\"ash-tab-panel\">Panel " + id + "</div>";
    }

    private static String group(String id, String tabs, String panels) {
        return "<div id=\"" + id + "\" data-tabs class=\"ash-tabs\"><div data-tabs-list role=\"tablist\" aria-label=\"" + id + "\" class=\"ash-tabs__list\">" + tabs + "</div></div>" + panels;
    }

    private static String markup() {
        return group("A", tab("A", "a", "A", false, false) + tab("A", "b", "B", true, false) + tab("A", "c", "C", false, false), panel("A", "a", true) + panel("A", "b", false) + panel("A", "c", false))
                + group("B", tab("B", "one", "One", true, false) + tab("B", "two", "Two", false, true) + tab("B", "three", "Three", false, false),
                        panel("B", "one", true) + panel("B", "two", false) + panel("B", "three", false))
                + group("C", tab("C", "one", "One", false, true) + tab("C", "two", "Two", true, false) + tab("C", "three", "Three", false, false) + tab("C", "four", "Four", false, true),
                        panel("C", "one", false) + panel("C", "two", true) + panel("C", "three", false) + panel("C", "four", false))
                + group("D", tab("D", "x", "X", false, false) + tab("D", "y", "Y", true, false), panel("D", "x", false) + panel("D", "y", true));
    }

    @BeforeAll
    static void run() throws Exception {
        ParitySession session = ParitySession.open("tabs-script");
        String js;
        try (InputStream in = TabsScriptTest.class.getResourceAsStream("/META-INF/resources/ashlar/js/tabs.js")) {
            if (in == null) {
                throw new IOException("tabs.js is not on the test classpath");
            }
            js = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertFalse(js.contains("</script"), "the script text can be embedded in a text/plain script element");
        String page = "<!doctype html><html data-theme=\"light\"><head><link rel=\"stylesheet\" href=\"" + ParityFrames.ASHLAR + "\"><style>*,*::before,*::after{transition:none !important}</style></head><body style=\"margin:8px\">"
                + markup() + "<script id=\"src\" type=\"text/plain\">" + js + "</script><script>window.addEventListener('load',function(){" + SCRIPT.replace("</script", "<\\/script")
                + "});</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String dom = session.chrome().dumpDom(server.registerRaw(page));
            for (String line : ParityProbe.lines(dom, "ash-tabs-js")) {
                String[] f = line.split("\t", -1);
                RESULTS.put(f[0], f);
            }
        }
    }

    @Test
    void thePageRanTheScriptAndEveryScenarioReported() {
        assertFalse(RESULTS.containsKey("SCRIPT ERROR"), () -> RESULTS.get("SCRIPT ERROR") == null ? "" : String.join(" | ", RESULTS.get("SCRIPT ERROR")));
        for (String name : EXPECTED) {
            assertTrue(RESULTS.containsKey(name), "scenario did not report: " + name + " in " + RESULTS.keySet());
        }
        assertEquals(EXPECTED.size(), RESULTS.size(), RESULTS.keySet().toString());
    }

    @Test
    void everyScenarioPassedInRealChrome() {
        for (String name : EXPECTED) {
            String[] result = RESULTS.get(name);
            assertTrue(result != null && result[1].equals("ok"), name + " -> " + (result == null ? "missing" : String.join(" | ", result)));
        }
    }
}
