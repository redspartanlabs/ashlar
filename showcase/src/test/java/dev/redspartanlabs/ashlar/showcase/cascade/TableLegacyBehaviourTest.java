package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the Table DOES, in a real browser, with trusted input: the ORIGINAL {@code table.js} (frozen as a test resource) driving the legacy markup, and the migrated {@code table.js} driving
 * the migrated markup, are put through the same sequence of real key presses and mouse clicks, and the recorded results must be identical step for step. Each step records, for four
 * tables, what a reader would see and what assistive technology would be told: the row order, which rows are on the page (by geometry, not by a class or an attribute), which are selected
 * (checkbox, marker and the fill the marker draws), every heading's {@code aria-sort} and sort glyph (text and colour), the selection bar and its count, the select-all state including
 * {@code indeterminate}, the scroller and both empty states (by geometry), the live status text and its {@code aria-live}, the page indicator, the pager buttons, the search value, and
 * where focus is. The accessibility tree (Chrome's own, through the DevTools {@code Accessibility} domain) is compared at four points, normalised to roles, names, states and nesting
 * (no node ids, no DOM ids).
 *
 * <p>The sequence covers sorting a text, a number and a column with blank cells (three clicks: ascending, descending, back to the server's order), paging, selecting a row, a page and
 * clearing, selection surviving sorting and searching, a search that matches one row and one that matches none, a server-rendered empty table, the tab order, and the same controls
 * driven from the keyboard. The assertions after it state what the platform and the component contract require; they were written from the contract, not from the old output.
 *
 * <p>{@link #theOnlyScriptDifferencesAreTheEnumeratedLines} is the textual guarantee: apart from comments, the migrated script differs from the original in exactly the eleven class
 * toggles it replaced.
 */
class TableLegacyBehaviourTest {

    private static final Map<String, String> LEGACY = new LinkedHashMap<>();
    private static final Map<String, String> MIGRATED = new LinkedHashMap<>();
    /** How many times the script wrote a class or style attribute while the sequence ran (recorded for both, compared with a purpose: the original wrote classes, the migrated one writes none). */
    private static final Map<String, Integer> CLASS_OR_STYLE_WRITES = new LinkedHashMap<>();

    private record Step(String label, List<String> actions) {
    }

    private static Step step(String label, String... actions) {
        return new Step(label, List.of(actions));
    }

    private static String resource(String path) throws IOException {
        try (InputStream in = TableLegacyBehaviourTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException(path + " is not on the test classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        }
    }

    // ---------------------------------------------------------------- the page

    private static String body(boolean legacy) throws Exception {
        String html = "<input id=\"before\" type=\"text\" aria-label=\"before\">"
                + "<div data-field=\"bt1\">" + TableBrowser.table(TableBrowser.o("searchable", true, "selectable", true, "sortable", true, "pageSize", 3, "rows", 6)) + "</div>"
                + "<div data-field=\"bt2\">" + TableBrowser.table(TableBrowser.o("searchable", true, "sortable", true, "rows", 6)) + "</div>"
                + "<div data-field=\"bt3\">" + TableBrowser.table(TableBrowser.o("searchable", true, "empty", true, "rows", 0)) + "</div>"
                + "<div data-field=\"bt4\">" + TableBrowser.table(TableBrowser.o("variants", true, "selectable", true, "sortable", true, "searchable", true)) + "</div>"
                + "<input id=\"after\" type=\"text\" aria-label=\"after\">";
        return legacy ? new IconLegacyMarkup().rewrite(new TableLegacyMarkup().rewrite(html)) : html;
    }

    /** What the reader sees and is told, one table at a time. Visibility is geometry, so it means the same whether a class, an attribute or a rule hides the element. */
    private static final String SNAPSHOT = """
            (function(){
            var LEG=%LEG%;
            function vis(e){if(!e)return 'absent';var r=e.getBoundingClientRect(),cs=getComputedStyle(e);return (cs.display!=='none'&&cs.visibility!=='hidden'&&r.width>0&&r.height>0)?'shown':'hidden'}
            function mark(r){return LEG?r.classList.contains('bg-blue-50/60'):r.hasAttribute('data-selected')}
            function desc(e){if(!e)return 'none';if(e===document.body)return 'BODY';var d=e.tagName.toLowerCase();
              for(var i=0;i<e.attributes.length;i++){var n=e.attributes[i].name;if(n.indexOf('data-table')===0)d+='['+n+']'}
              var tr=e.closest('tr[data-row-key]');if(tr)d+='@'+tr.getAttribute('data-row-key');var f=e.closest('[data-field]');if(f)d+='#'+f.getAttribute('data-field');
              if(e.id)d+='id='+e.id;return d+(e.matches(':focus-visible')?' (ring)':'')}
            function q(r,s){return r.querySelector(s)}
            function snap(f){
              var root=document.querySelector('[data-field='+f+']');
              var rows=[].map.call(root.querySelectorAll('tbody tr'),function(r){var c=q(r,'[data-table-select-row]');
                return r.getAttribute('data-row-key')+(vis(r)==='shown'?'':'~')+(c&&c.checked?'*':'')+(mark(r)?'#':'')+(c&&c.checked!==mark(r)?'!':'')});
              var heads=[].map.call(root.querySelectorAll('thead th[data-table-column]'),function(h){var i=q(h,'[data-table-sort-icon]');return (h.getAttribute('aria-sort')||'-')+':'+(i?i.textContent+'/'+getComputedStyle(i).color:'')});
              var sa=q(root,'[data-table-select-all]'),bar=q(root,'[data-table-selection-bar]'),cnt=q(root,'[data-table-selection-count]'),st=q(root,'[data-table-status]'),
                  pi=q(root,'[data-table-page-indicator]'),pv=q(root,'[data-table-prev]'),nx=q(root,'[data-table-next]'),se=q(root,'[data-table-search]');
              var picked=[].filter.call(root.querySelectorAll('tbody tr'),mark)[0];
              return {rows:rows.join(' '),heads:heads.join(' | '),selectAll:sa?(sa.checked?'checked':'unchecked')+(sa.indeterminate?'+indeterminate':''):'-',
                bar:vis(bar)+(cnt?' "'+cnt.textContent+'"':''),scroll:vis(q(root,'[data-table-scroll]')),none:vis(q(root,'[data-table-empty=none]')),filtered:vis(q(root,'[data-table-empty=filtered]')),
                status:st?st.textContent+' | live='+st.getAttribute('aria-live'):'-',page:pi?pi.textContent:'-',prev:pv?String(pv.disabled):'-',next:nx?String(nx.disabled):'-',search:se?se.value:'-',
                selectedFill:picked?getComputedStyle(picked).backgroundColor:'-'}}
            return JSON.stringify({active:desc(document.activeElement),bt1:snap('bt1'),bt2:snap('bt2'),bt3:snap('bt3'),bt4:snap('bt4')})})()
            """;

    /** Chrome's accessibility tree as roles, names, states and nesting: ignored nodes are dropped (their children kept), and no id of any kind is printed. */
    private static final String NORMALISE = """
            (function(){var r=%RAW%;var nodes=r.result.nodes,by={};nodes.forEach(function(n){by[n.nodeId]=n});
            function val(x){return x&&x.value!==undefined?String(x.value):''}
            function line(n,d){var p=(n.properties||[]).filter(function(p){return p.name!=='url'}).map(function(p){return p.name+'='+val(p.value)}).sort().join(',');return '  '.repeat(d)+val(n.role)+' "'+val(n.name)+'"'+(p?' {'+p+'}':'')}
            var out=[];function walk(id,d){var n=by[id];if(!n)return;var next=d;if(!n.ignored){out.push(line(n,d));next=d+1}(n.childIds||[]).forEach(function(c){walk(c,next)})}
            nodes.filter(function(n){return !n.parentId}).forEach(function(n){walk(n.nodeId,0)});return out.join('\\n')})()
            """;

    private static String snapshot(DevToolsSession cdp, boolean legacy) throws Exception {
        return cdp.evaluate(SNAPSHOT.replace("%LEG%", String.valueOf(legacy)));
    }

    private static String accessibility(DevToolsSession cdp) throws Exception {
        String raw = cdp.call("Accessibility.getFullAXTree", "{}");
        return cdp.evaluate(NORMALISE.replace("%RAW%", raw));
    }

    // ---------------------------------------------------------------- the sequence

    private static String sort(String f, int offset, int column) {
        return "[data-field=" + f + "] thead th:nth-of-type(" + (column + offset) + ") [data-table-sort]";
    }

    private static String checkbox(String f, String key) {
        return "[data-field=" + f + "] tbody tr[data-row-key=\"" + key + "\"] [data-table-select-row]";
    }

    private static String part(String f, String hook) {
        return "[data-field=" + f + "] [" + hook + "]";
    }

    private static List<Step> steps() {
        List<Step> s = new ArrayList<>();
        s.add(step("bt1 Account sort 1 (ascending)", "click:" + sort("bt1", 1, 1)));
        s.add(step("bt1 Account sort 2 (descending)", "click:" + sort("bt1", 1, 1)));
        s.add(step("bt1 Account sort 3 (the server's order)", "click:" + sort("bt1", 1, 1)));
        s.add(step("bt1 Seats sort 1 (ascending, as numbers)", "click:" + sort("bt1", 1, 3)));
        s.add(step("bt1 Seats sort 2 (descending)", "click:" + sort("bt1", 1, 3)));
        s.add(step("bt1 Seats sort 3 (the server's order)", "click:" + sort("bt1", 1, 3)));
        s.add(step("bt1 Seats sort 4 (ascending again)", "click:" + sort("bt1", 1, 3)));
        s.add(step("bt1 next page", "click:" + part("bt1", "data-table-next")));
        s.add(step("bt1 select ACC-1001 on page 2", "click:" + checkbox("bt1", "ACC-1001")));
        s.add(step("bt1 previous page", "click:" + part("bt1", "data-table-prev")));
        s.add(step("bt1 select all on page 1", "click:" + part("bt1", "data-table-select-all")));
        s.add(step("bt1 next page again (partly selected)", "click:" + part("bt1", "data-table-next")));
        s.add(step("bt1 clear selection", "click:" + part("bt1", "data-table-clear-selection")));
        s.add(step("bt1 select ACC-1004 on page 2", "click:" + checkbox("bt1", "ACC-1004")));
        s.add(step("bt1 Account sort 1 (selection survives, back to page 1)", "click:" + sort("bt1", 1, 1)));
        s.add(step("bt1 select ACC-1002", "click:" + checkbox("bt1", "ACC-1002")));
        s.add(step("bt1 click the search field", "click:" + part("bt1", "data-table-search")));
        s.add(step("bt1 search for avery (selected rows now hidden)", "type:avery"));
        s.add(step("bt1 search for averyzzz (nothing matches)", "type:zzz"));
        s.add(step("bt1 erase the search", "key:Backspace", "key:Backspace", "key:Backspace", "key:Backspace", "key:Backspace", "key:Backspace", "key:Backspace", "key:Backspace"));
        s.add(step("bt1 clear selection after the search", "click:" + part("bt1", "data-table-clear-selection")));
        s.add(step("bt2 Seats sort 1", "click:" + sort("bt2", 0, 3)));
        s.add(step("bt2 Seats sort 2", "click:" + sort("bt2", 0, 3)));
        s.add(step("bt2 search for ey", "click:" + part("bt2", "data-table-search"), "type:ey"));
        s.add(step("bt2 Owner sort from the keyboard (Enter)", "focusSel:" + sort("bt2", 0, 2), "key:Enter"));
        s.add(step("bt2 Owner sort from the keyboard (Space)", "key:Space"));
        s.add(step("bt3 server-rendered empty table, search for x", "click:" + part("bt3", "data-table-search"), "type:x"));
        s.add(step("bt4 Amount sort 1 (blank last)", "click:" + sort("bt4", 1, 3)));
        s.add(step("bt4 Amount sort 2 (blank still last)", "click:" + sort("bt4", 1, 3)));
        s.add(step("bt4 Name sort 1", "click:" + sort("bt4", 1, 1)));
        s.add(step("bt4 select all", "click:" + part("bt4", "data-table-select-all")));
        s.add(step("bt4 deselect V-1 from the keyboard (Space)", "focusSel:" + checkbox("bt4", "V-1"), "key:Space"));
        s.add(step("bt4 clear selection", "click:" + part("bt4", "data-table-clear-selection")));
        s.add(step("bt1 next page from the keyboard (Enter)", "focusSel:" + part("bt1", "data-table-next"), "key:Enter"));
        return s;
    }

    private static void run(DevToolsSession cdp, boolean legacy, Map<String, String> out) throws Exception {
        out.put("initial", snapshot(cdp, legacy));
        cdp.evaluate("(function(){window.__writes=0;new MutationObserver(function(r){window.__writes+=r.length}).observe(document.body,{subtree:true,attributes:true,attributeFilter:['class','style']});return 'ok'})()");
        out.put("AX initial", accessibility(cdp));
        // The tab order through the untouched page, as far as it goes.
        cdp.evaluate("(function(){document.getElementById('before').focus();return 'ok'})()");
        StringBuilder tabs = new StringBuilder();
        for (int k = 0; k < 24; k++) {
            TextEntryBrowser.perform(cdp, "key:Tab");
            String now = snapshot(cdp, legacy);
            int at = now.indexOf("\"active\":\"") + 10;
            tabs.append(now, at, now.indexOf("\"", at)).append(" > ");
        }
        out.put("Tab order", tabs.toString());
        for (Step step : steps()) {
            for (String action : step.actions()) {
                if (action.startsWith("focusSel:")) {
                    cdp.evaluate("(function(){document.querySelector(" + DevToolsSession.quote(action.substring(9)) + ").focus();return 'ok'})()");
                } else {
                    TextEntryBrowser.perform(cdp, action);
                }
            }
            out.put(step.label(), snapshot(cdp, legacy));
            if (step.label().equals("bt1 select all on page 1") || step.label().equals("bt1 search for averyzzz (nothing matches)") || step.label().equals("bt4 Amount sort 2 (blank still last)")) {
                out.put("AX after " + step.label(), accessibility(cdp));
            }
        }
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("table-legacy-behaviour");
        ParityConfigs.Config config = ParityConfigs.get("light/none");
        String legacyScript = "<script>" + resource("/parity/table/table-legacy.js") + "</script>";
        String migratedScript = "<script>" + resource("/META-INF/resources/ashlar/js/table.js") + "</script>";
        assertFalse(legacyScript.substring(8, legacyScript.length() - 9).contains("</script"));
        assertFalse(migratedScript.substring(8, migratedScript.length() - 9).contains("</script"));
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String legacyUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.generated("table-oracle.css")), config, "", body(true), legacyScript));
            String migratedUrl = server.registerRaw(TextEntryBrowser.page(List.of(ParityFrames.APP), config, "", body(false), migratedScript));
            for (boolean legacy : List.of(true, false)) {
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    cdp.navigate(legacy ? legacyUrl : migratedUrl);
                    run(cdp, legacy, legacy ? LEGACY : MIGRATED);
                    Thread.sleep(50);
                    CLASS_OR_STYLE_WRITES.put(legacy ? "legacy" : "migrated", Integer.parseInt(cdp.evaluate("String(window.__writes)")));
                }
            }
        }
    }

    // ---------------------------------------------------------------- reading a recorded step

    /** One recorded fact: {@code table} is bt1..bt4, {@code name} a key of its snapshot ({@code rows}, {@code bar}, {@code status}, ...). */
    private static String fact(Map<String, String> run, String step, String table, String name) {
        String json = run.get(step);
        assertTrue(json != null, "no such step: " + step);
        int start = json.indexOf("\"" + table + "\":{");
        assertTrue(start >= 0, table + " in " + json);
        Matcher m = Pattern.compile("\"" + name + "\":\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json.substring(start, json.indexOf("}", start)));
        assertTrue(m.find(), name + " of " + table + " in " + step);
        return m.group(1).replace("\\\"", "\"");
    }

    private static String active(Map<String, String> run, String step) {
        String json = run.get(step);
        int at = json.indexOf("\"active\":\"") + 10;
        return json.substring(at, json.indexOf('"', at));
    }

    private static String m(String step, String table, String name) {
        return fact(MIGRATED, step, table, name);
    }

    // ---------------------------------------------------------------- tests

    @Test
    void everyStepGivesTheSameResultInTheLegacyAndTheMigratedRendering() {
        assertEquals(LEGACY.keySet(), MIGRATED.keySet());
        assertTrue(LEGACY.size() >= 40, "recorded steps: " + LEGACY.size());
        for (String step : LEGACY.keySet()) {
            assertEquals(LEGACY.get(step), MIGRATED.get(step), step);
        }
    }

    @Test
    void theAccessibilityTreeIsTheSameAtFourPointsAndStillDescribesATable() {
        List<String> points = LEGACY.keySet().stream().filter(k -> k.startsWith("AX")).toList();
        assertEquals(4, points.size(), points.toString());
        for (String point : points) {
            assertEquals(LEGACY.get(point), MIGRATED.get(point), point);
            assertFalse(MIGRATED.get(point).contains("url="), "no address is compared: it differs between two pages by construction");
        }
        String tree = MIGRATED.get("AX initial");
        for (String expected : List.of("table \"Accounts\"", "columnheader \"Account\"", "columnheader \"Select all rows on this page\"", "checkbox \"Select all rows on this page\"",
                "checkbox \"Select row ACC-1001\"", "rowheader \"ACC-1001\"", "button \"Account\"", "searchbox \"Search\"")) {
            assertTrue(tree.contains(expected), expected);
        }
        assertTrue(tree.contains("paragraph \"\" {atomic=false,live=polite,relevant=additions text}"), "the status is a polite live region");
        assertTrue(MIGRATED.get("AX after bt1 select all on page 1").contains("StaticText \"4 selected\""), "the selection count is exposed once it is shown");
        assertTrue(MIGRATED.get("AX after bt1 select all on page 1").contains("checkbox \"Select row ACC-1006\" {checked=true"), "a selected row's checkbox is checked");
        assertFalse(MIGRATED.get("AX after bt1 search for averyzzz (nothing matches)").contains("Select row ACC-1001"), "rows that are not shown are not in the tree");
        assertTrue(MIGRATED.get("AX after bt1 search for averyzzz (nothing matches)").contains("No matching records"), "the empty state that appears is");
    }

    @Test
    void sortingGoesAscendingThenDescendingThenBackToTheServersOrderAndNumbersSortAsNumbers() {
        assertEquals("ACC-1001 ACC-1002 ACC-1003 ACC-1004~ ACC-1005~ ACC-1006~", m("initial", "bt1", "rows"));
        assertEquals("ascending:↑", m("bt1 Account sort 1 (ascending)", "bt1", "heads").split(" \\| ")[0].replaceAll("/.*", ""));
        assertEquals("ACC-1006 ACC-1005 ACC-1004 ACC-1003~ ACC-1002~ ACC-1001~", m("bt1 Account sort 2 (descending)", "bt1", "rows"));
        assertEquals("descending:↓", m("bt1 Account sort 2 (descending)", "bt1", "heads").split(" \\| ")[0].replaceAll("/.*", ""));
        assertEquals(m("initial", "bt1", "rows"), m("bt1 Account sort 3 (the server's order)", "bt1", "rows"));
        assertEquals("none:⇅", m("bt1 Account sort 3 (the server's order)", "bt1", "heads").split(" \\| ")[0].replaceAll("/.*", ""));
        // 1,204 / 38 / 7 / 410 / 95 / 2 as numbers, not as text.
        assertEquals("ACC-1006 ACC-1003 ACC-1002 ACC-1005~ ACC-1004~ ACC-1001~", m("bt1 Seats sort 1 (ascending, as numbers)", "bt1", "rows"));
        assertEquals("ACC-1001 ACC-1004 ACC-1005 ACC-1002~ ACC-1003~ ACC-1006~", m("bt1 Seats sort 2 (descending)", "bt1", "rows"));
        assertEquals("ascending:↑", m("bt1 Seats sort 1 (ascending, as numbers)", "bt1", "heads").split(" \\| ")[2].replaceAll("/.*", ""));
        assertEquals("none:⇅", m("bt1 Seats sort 1 (ascending, as numbers)", "bt1", "heads").split(" \\| ")[0].replaceAll("/.*", ""), "only the sorted column is marked");
        assertEquals("ACC-1006 ACC-1005~ ACC-1004~ ACC-1003 ACC-1002~ ACC-1001~", m("bt2 Owner sort from the keyboard (Space)", "bt2", "rows"), "Finley, Emery, Devon, Casey, Blake, Avery");
        assertEquals("V-1 V-2", m("bt4 Amount sort 2 (blank still last)", "bt4", "rows"), "the blank cell stays last in a descending sort");
    }

    @Test
    void sortingAndPagingKeepTheStatusPageIndicatorAndPagerButtonsInStep() {
        assertEquals("Showing 1–3 of 6 records | live=polite", m("initial", "bt1", "status"));
        assertEquals("Page 1 of 2", m("initial", "bt1", "page"));
        assertEquals("true", m("initial", "bt1", "prev"));
        assertEquals("false", m("initial", "bt1", "next"));
        assertEquals("Showing 4–6 of 6 records | live=polite", m("bt1 next page", "bt1", "status"));
        assertEquals("Page 2 of 2", m("bt1 next page", "bt1", "page"));
        assertEquals("false", m("bt1 next page", "bt1", "prev"));
        assertEquals("true", m("bt1 next page", "bt1", "next"));
        assertEquals("ACC-1006~ ACC-1003~ ACC-1002~ ACC-1005 ACC-1004 ACC-1001", m("bt1 next page", "bt1", "rows"));
        assertEquals("Showing 6 of 6 records | live=polite", m("initial", "bt2", "status"), "a table without a page size counts what it shows");
    }

    @Test
    void selectionIsTrackedByRowNotByPositionAndTheCheckboxAndTheRowMarkerAlwaysAgree() {
        for (String step : MIGRATED.keySet()) {
            if (step.startsWith("AX") || step.equals("Tab order")) {
                continue;
            }
            for (String table : List.of("bt1", "bt4")) {
                assertFalse(m(step, table, "rows").contains("!"), step + ": a checked row and a marked row disagree in " + table + ": " + m(step, table, "rows"));
            }
        }
        assertEquals("ACC-1006~ ACC-1003~ ACC-1002~ ACC-1005 ACC-1004 ACC-1001*#", m("bt1 select ACC-1001 on page 2", "bt1", "rows"));
        assertEquals("shown \"1 selected\"", m("bt1 select ACC-1001 on page 2", "bt1", "bar"));
        assertEquals("unchecked+indeterminate", m("bt1 select ACC-1001 on page 2", "bt1", "selectAll"), "one of three rows on the page");
        assertEquals("ACC-1006*# ACC-1003*# ACC-1002*# ACC-1005~ ACC-1004~ ACC-1001~*#", m("bt1 select all on page 1", "bt1", "rows"), "select all takes the rows on this page and keeps the one on the other");
        assertEquals("shown \"4 selected\"", m("bt1 select all on page 1", "bt1", "bar"));
        assertEquals("checked", m("bt1 select all on page 1", "bt1", "selectAll"));
        assertEquals("unchecked+indeterminate", m("bt1 next page again (partly selected)", "bt1", "selectAll"));
        assertEquals("ACC-1006~ ACC-1003~ ACC-1002~ ACC-1005 ACC-1004 ACC-1001", m("bt1 clear selection", "bt1", "rows"));
        assertTrue(m("bt1 clear selection", "bt1", "bar").startsWith("hidden"), "clearing hides the bar");
        assertEquals("ACC-1001 ACC-1002 ACC-1003 ACC-1004~*# ACC-1005~ ACC-1006~", m("bt1 Account sort 1 (selection survives, back to page 1)", "bt1", "rows"), "sorting does not lose the selected row");
        assertEquals("shown \"2 selected\"", m("bt1 search for avery (selected rows now hidden)", "bt1", "bar"), "searching hides rows but not their selection");
        assertEquals("ACC-1001 ACC-1002~*# ACC-1003~ ACC-1004~*# ACC-1005~ ACC-1006~", m("bt1 search for avery (selected rows now hidden)", "bt1", "rows"));
        assertNotEquals("-", m("bt1 next page again (partly selected)", "bt1", "selectedFill"), "a selected row has a fill");
        assertEquals("-", m("bt1 clear selection", "bt1", "selectedFill"));
    }

    @Test
    void searchingNarrowsTheRowsAndAnEmptyResultSaysSoAndErasingTheQueryBringsEverythingBack() {
        assertEquals("Showing 1–1 of 1 records | live=polite", m("bt1 search for avery (selected rows now hidden)", "bt1", "status"));
        assertEquals("Page 1 of 1", m("bt1 search for avery (selected rows now hidden)", "bt1", "page"));
        assertEquals("No records match “averyzzz” | live=polite", m("bt1 search for averyzzz (nothing matches)", "bt1", "status"));
        assertEquals("hidden", m("bt1 search for averyzzz (nothing matches)", "bt1", "scroll"));
        assertEquals("shown", m("bt1 search for averyzzz (nothing matches)", "bt1", "filtered"));
        assertEquals("hidden", m("bt1 search for averyzzz (nothing matches)", "bt1", "none"));
        assertEquals("true", m("bt1 search for averyzzz (nothing matches)", "bt1", "prev"));
        assertEquals("true", m("bt1 search for averyzzz (nothing matches)", "bt1", "next"));
        assertEquals("shown", m("bt1 erase the search", "bt1", "scroll"));
        assertEquals("hidden", m("bt1 erase the search", "bt1", "filtered"));
        assertEquals("Showing 1–3 of 6 records | live=polite", m("bt1 erase the search", "bt1", "status"));
        assertEquals("", m("bt1 erase the search", "bt1", "search"));
        assertEquals("Showing 2 of 6 records | live=polite", m("bt2 search for ey", "bt2", "status"));
        assertEquals("shown", m("initial", "bt3", "none"), "a table the server rendered empty says so");
        assertEquals("shown", m("bt3 server-rendered empty table, search for x", "bt3", "none"), "and typing in its search does not change that");
        assertEquals("hidden", m("bt3 server-rendered empty table, search for x", "bt3", "filtered"));
        assertEquals("", m("bt3 server-rendered empty table, search for x", "bt3", "status").replace(" | live=polite", ""));
    }

    @Test
    void theKeyboardDrivesTheSameControlsAndAMouseClickLeavesNoFocusRingWhileKeysDo() {
        String order = MIGRATED.get("Tab order");
        assertTrue(order.startsWith("input[data-table-search]#bt1 (ring) > input[data-table-select-all]#bt1 (ring) > button[data-table-sort]#bt1 (ring)"), order);
        assertFalse(order.contains("@ACC-1004"), "a row that is not on the page cannot be tabbed to");
        assertFalse(order.contains("data-table-prev"), "a disabled button is skipped");
        assertTrue(order.contains("input[data-table-select-row]@ACC-1003#bt1 (ring) > button[data-table-next]#bt1 (ring)"), order);
        assertTrue(order.contains("a@V-2#bt4 (ring)"), "a link a caller put in a cell is in the tab order");
        assertEquals("ascending:↑", m("bt2 Owner sort from the keyboard (Enter)", "bt2", "heads").split(" \\| ")[1].replaceAll("/.*", ""), "Enter on a sort button sorts");
        assertEquals("descending:↓", m("bt2 Owner sort from the keyboard (Space)", "bt2", "heads").split(" \\| ")[1].replaceAll("/.*", ""), "and so does Space");
        assertEquals("button[data-table-sort]#bt2 (ring)", active(MIGRATED, "bt2 Owner sort from the keyboard (Enter)"));
        assertEquals("button[data-table-sort]#bt1", active(MIGRATED, "bt1 Account sort 1 (ascending)"), "a mouse click focuses the button but draws no ring");
        assertEquals("input[data-table-select-row]@ACC-1001#bt1", active(MIGRATED, "bt1 select ACC-1001 on page 2"));
        assertEquals("button[data-table-clear-selection]#bt1", active(MIGRATED, "bt1 clear selection"), "focus stays on the control that was used, even though it hides itself");
        assertEquals("Page 2 of 2", m("bt1 next page from the keyboard (Enter)", "bt1", "page"));
    }

    @Test
    void theSelectionOfAKeyedRowFromTheKeyboardWorksAndSelectAllCoversTheVariantsTable() {
        assertEquals("V-1*# V-2*#", m("bt4 select all", "bt4", "rows"));
        assertEquals("checked", m("bt4 select all", "bt4", "selectAll"));
        assertEquals("V-1 V-2*#", m("bt4 deselect V-1 from the keyboard (Space)", "bt4", "rows"));
        assertEquals("unchecked+indeterminate", m("bt4 deselect V-1 from the keyboard (Space)", "bt4", "selectAll"));
        assertEquals("shown \"1 selected\"", m("bt4 deselect V-1 from the keyboard (Space)", "bt4", "bar"));
        assertTrue(m("bt4 clear selection", "bt4", "bar").startsWith("hidden"));
    }

    @Test
    void theMigratedScriptWritesNoClassAndNoStyleWhileTheOriginalDidSoTheObserverCanSeeIt() {
        assertTrue(CLASS_OR_STYLE_WRITES.get("legacy") > 20, "the original toggled classes throughout the sequence: " + CLASS_OR_STYLE_WRITES);
        assertEquals(0, CLASS_OR_STYLE_WRITES.get("migrated"), "the migrated script writes state attributes, never a class or a style");
    }

    @Test
    void theOnlyScriptDifferencesAreTheEnumeratedLines() throws IOException {
        List<String> legacy = new ArrayList<>(resource("/parity/table/table-legacy.js").lines().map(String::strip).toList());
        List<String> migrated = new ArrayList<>(resource("/META-INF/resources/ashlar/js/table.js").lines().map(String::strip).toList());
        List<String> removed = List.of(
                "icon.classList.toggle(\"text-blue-600\", active);",
                "icon.classList.toggle(\"dark:text-blue-400\", active);",
                "icon.classList.toggle(\"text-slate-400\", !active);",
                "icon.classList.toggle(\"dark:text-slate-500\", !active);",
                "row.classList.toggle(\"bg-blue-50/60\", isSelected);",
                "row.classList.toggle(\"dark:bg-blue-950/40\", isSelected);",
                "selectionBar.classList.toggle(\"hidden\", !hasSelection);",
                "selectionBar.classList.toggle(\"flex\", hasSelection);",
                "emptyFiltered.classList.toggle(\"hidden\", !filteredToNothing);",
                "scroll.classList.toggle(\"hidden\", filteredToNothing);",
                "emptyNone.classList.add(\"hidden\");");
        List<String> added = List.of(
                "row.toggleAttribute(\"data-selected\", isSelected);",
                "selectionBar.hidden = !hasSelection;",
                "emptyFiltered.hidden = !filteredToNothing;",
                "scroll.hidden = filteredToNothing;",
                "emptyNone.hidden = true;");
        for (String line : removed) {
            assertTrue(legacy.remove(line), "the original script has: " + line);
            assertFalse(migrated.contains(line), "the migrated script no longer has: " + line);
        }
        for (String line : added) {
            assertTrue(migrated.remove(line), "the migrated script has: " + line);
            assertFalse(legacy.contains(line), "the original script lacks: " + line);
        }
        // What is left must be the same code in the same order; comment lines (which a migration is free to add) are not code.
        legacy.removeIf(l -> l.startsWith("//"));
        migrated.removeIf(l -> l.startsWith("//"));
        assertEquals(legacy, migrated, "apart from the enumerated lines and comments the two scripts are line for line the same");
        assertFalse(resource("/META-INF/resources/ashlar/js/table.js").contains("classList"), "the migrated script touches no class");
        assertFalse(resource("/META-INF/resources/ashlar/js/table.js").contains(".style"), "and no inline style");
    }
}
