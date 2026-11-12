package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Table family on the REAL Showcase: the packaged application, started as a separate process, and its real pages. Two things are checked there because they cannot be checked on a
 * fixture.
 *
 * <p>First, the markup the real templates (the Table page, the Data Table page and every other page that renders a Table) actually emit: every element that carries a Table class carries
 * nothing but Ashlar classes, every Table class the real pages use is one the legacy rewriter knows (it fails loudly on any other, so the style comparison in {@link ShowcaseRuntimeTest}
 * reaches all of them), and rewriting those pages back to the legacy classes leaves no Table class behind.
 *
 * <p>Second, the real {@code table.js}, as the application serves it, driven by TRUSTED input on the real Table and Data Table pages, for every table on them and every sort button of every
 * table: three clicks give ascending, descending and the server's order again, and the rows really are in the order the heading says (checked against the contract, with blank cells last);
 * a search that matches nothing shows the empty state and hides the scroller, and erasing it restores both; selecting a row marks it, shows the selection bar and counts it, and clearing
 * undoes all of it. No class and no inline style is written inside any table while this happens.
 */
class TableRealPagesTest {

    /** Pages that render a Table somewhere (found by searching the templates for the component), and the home page. */
    private static final List<String> PAGES = List.of("table", "data-table", "badge", "faceted-search", "filter-bar", "list", "page-shell", "pagination");

    private static final Pattern CLASS_ATTRIBUTE = Pattern.compile("<([a-z0-9]+)\\b[^>]*?\\bclass=\"([^\"]*)\"[^>]*>");
    private static final Map<String, String> HTML = new LinkedHashMap<>();
    private static final Map<String, List<String>> PROBLEMS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Integer>> COUNTS = new LinkedHashMap<>();

    private static String fetch(HttpClient client, int port, String path) throws Exception {
        HttpResponse<byte[]> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode(), path);
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    @BeforeAll
    static void run() throws Exception {
        String bootJar = System.getProperty("ashlar.showcase.jar");
        if (bootJar == null) {
            throw new IllegalStateException("ashlar.showcase.jar must be set; run through Gradle (:showcase:test)");
        }
        ParitySession session = ParitySession.open("table-real-pages");
        HttpClient client = HttpClient.newHttpClient();
        try (RunningShowcase app = RunningShowcase.start(Path.of(bootJar)); RealPageProxy proxy = new RealPageProxy(app.port())) {
            for (String slug : PAGES) {
                HTML.put(slug, fetch(client, app.port(), "/ashlar/components/" + slug));
            }
            HTML.put("home", fetch(client, app.port(), "/ashlar/"));
            proxy.configure(new RealPageProxy.Load("light", "snapshot", null));
            for (String slug : List.of("table", "data-table")) {
                try (DevToolsSession cdp = new DevToolsSession(session.chrome().executable())) {
                    cdp.navigate(proxy.url("/ashlar/components/" + slug));
                    Thread.sleep(300);
                    drive(cdp, slug);
                }
            }
        }
    }

    private static String ev(DevToolsSession cdp, String js) throws Exception {
        return cdp.evaluate("(function(){try{return String(" + js + ")}catch(e){return 'ERR '+e}})()");
    }

    private static final String HELPERS = "(function(){"
            + "window.shown=function(e){return !!e&&e.getClientRects().length>0&&getComputedStyle(e).visibility!=='hidden'};"
            + "window.T=function(i){return document.querySelector('[data-t=\"'+i+'\"]')};"
            + "window.order=function(i){var b=T(i).querySelector('[data-table-body]');return !b?'':[].map.call(b.rows,function(r){return r.getAttribute('data-row-key')+':'+r.textContent.trim().replace(/\\s+/g,' ')}).join('|')};"
            // The contract, written independently of table.js: the rows of column k are in order for the direction, and the rows with nothing in that column are last.
            + "window.sortedOk=function(i,btn,dir){var t=T(i),th=btn.closest('[data-table-column]'),ths=[].slice.call(t.querySelectorAll('[data-table-column]')),k=ths.indexOf(th),off=t.querySelector('[data-table-select-all]')?1:0,"
            + "type=th.getAttribute('data-table-type')||'text',rows=[].slice.call(t.querySelector('[data-table-body]').rows);"
            + "function val(r){var c=r.cells[k+off];if(!c)return '';var o=c.getAttribute('data-sort-value');return (o!==null&&o!==''?o:c.textContent).trim()}"
            + "function num(v){var n=parseFloat(v.replace(/[^0-9.eE+-]/g,''));return isNaN(n)?null:n}"
            + "function dat(v){var n=Date.parse(v);return isNaN(n)?null:n}"
            + "var keyed=rows.map(function(r){var v=val(r);return {v:v,n:type==='number'?num(v):type==='date'?dat(v):v,blank:(type==='number'&&num(v)===null)||(type==='date'&&dat(v)===null)}});"
            + "var seenBlank=false;for(var a=0;a<keyed.length;a++){if(keyed[a].blank)seenBlank=true;else if(seenBlank)return 'a value follows a blank in '+k}"
            + "var real=keyed.filter(function(x){return !x.blank});for(var b=1;b<real.length;b++){var p=real[b-1],q=real[b],c=(type==='text')?p.n.localeCompare(q.n,undefined,{numeric:true,sensitivity:'base'}):p.n-q.n;"
            + "if(dir==='ascending'&&c>0)return 'not ascending at '+b+': '+p.v+' > '+q.v;if(dir==='descending'&&c<0)return 'not descending at '+b+': '+p.v+' < '+q.v}return 'ok'};"
            + "var ts=document.querySelectorAll('[data-table]');ts.forEach(function(t,i){t.setAttribute('data-t',String(i));t.querySelectorAll('[data-table-sort]').forEach(function(b,j){b.setAttribute('data-s',i+'-'+j)})});"
            + "window.__writes=0;ts.forEach(function(t){new MutationObserver(function(r){window.__writes+=r.length}).observe(t,{subtree:true,attributes:true,attributeFilter:['class','style']})});"
            + "return String(ts.length)})()";

    /** Every table of the page, every sort button, a search, a selection; problems are collected, not thrown, so one run reports them all. */
    private static void drive(DevToolsSession cdp, String slug) throws Exception {
        List<String> problems = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        PROBLEMS.put(slug, problems);
        COUNTS.put(slug, counts);
        int tables = Integer.parseInt(cdp.evaluate(HELPERS));
        counts.put("tables", tables);
        for (int i = 0; i < tables; i++) {
            String t = "[data-t=\"" + i + "\"]";
            int rows = Integer.parseInt(ev(cdp, "(function(b){return b?b.rows.length:0})(T(" + i + ").querySelector('[data-table-body]'))"));
            int sorts = Integer.parseInt(ev(cdp, "T(" + i + ").querySelectorAll('[data-table-sort]').length"));
            counts.merge("rows", rows, Integer::sum);
            if (rows == 0) {
                // No rows on screen to sort. Either the page shows the loading state (no table at all) or the server rendered an empty list: the scroller is hidden, so the header buttons are
                // not on screen, and the Empty State is.
                boolean loading = "true".equals(ev(cdp, "!!T(" + i + ").querySelector('[data-table-loading]')"));
                counts.merge(loading ? "loading tables" : "empty tables", 1, Integer::sum);
                String state = loading ? ev(cdp, "[shown(T(" + i + ").querySelector('[data-table-loading]')),shown(T(" + i + ").querySelector('[data-table-scroll]'))].join(',')")
                        : ev(cdp, "[shown(T(" + i + ").querySelector('[data-table-empty=none]')),shown(T(" + i + ").querySelector('[data-table-scroll]'))].join(',')");
                if (!state.equals("true,false")) {
                    problems.add(slug + " table " + i + ": a table with no rows shows (" + (loading ? "loading state" : "empty state") + ", scroller) = " + state);
                }
                continue;
            }
            for (int j = 0; j < sorts; j++) {
                String button = "[data-s=\"" + i + "-" + j + "\"]";
                String original = ev(cdp, "order(" + i + ")");
                counts.merge("sort buttons", 1, Integer::sum);
                for (String expected : List.of("ascending", "descending", "none")) {
                    TextEntryBrowser.click(cdp, button);
                    String aria = ev(cdp, "document.querySelector('" + button + "').closest('[data-table-column]').getAttribute('aria-sort')");
                    if (!aria.equals(expected)) {
                        problems.add(slug + " table " + i + " button " + j + ": aria-sort " + aria + ", expected " + expected);
                    }
                    String where = slug + " table " + i + " button " + j + " " + expected;
                    if (expected.equals("none")) {
                        if (!original.equals(ev(cdp, "order(" + i + ")"))) {
                            problems.add(where + ": the server's order did not come back");
                        }
                    } else {
                        String ok = ev(cdp, "sortedOk(" + i + ",document.querySelector('" + button + "')," + DevToolsSession.quote(expected) + ")");
                        if (!ok.equals("ok")) {
                            problems.add(where + ": " + ok);
                        }
                    }
                }
            }
            if (rows > 0 && "true".equals(ev(cdp, "!!T(" + i + ").querySelector('[data-table-search]')"))) {
                counts.merge("searches", 1, Integer::sum);
                TextEntryBrowser.click(cdp, t + " [data-table-search]");
                TextEntryBrowser.perform(cdp, "type:zzzqq");
                String filtered = ev(cdp, "[shown(T(" + i + ").querySelector('[data-table-empty=filtered]')),shown(T(" + i + ").querySelector('[data-table-scroll]'))].join(',')");
                if (!filtered.equals("true,false")) {
                    problems.add(slug + " table " + i + ": a search for nothing shows (empty state, scroller) = " + filtered);
                }
                for (int b = 0; b < 5; b++) {
                    TextEntryBrowser.perform(cdp, "key:Backspace");
                }
                String restored = ev(cdp, "[shown(T(" + i + ").querySelector('[data-table-empty=filtered]')),shown(T(" + i + ").querySelector('[data-table-scroll]'))].join(',')");
                if (!restored.equals("false,true")) {
                    problems.add(slug + " table " + i + ": erasing the search shows (empty state, scroller) = " + restored);
                }
            }
            if (rows > 0 && "true".equals(ev(cdp, "!!T(" + i + ").querySelector('[data-table-select-row]')"))) {
                counts.merge("selections", 1, Integer::sum);
                String firstVisible = ev(cdp, "(function(){var r=[].filter.call(T(" + i + ").querySelectorAll('[data-table-select-row]'),shown)[0];r.setAttribute('data-p','first');return 'ok'})()");
                if (!firstVisible.equals("ok")) {
                    problems.add(slug + " table " + i + ": no visible row checkbox: " + firstVisible);
                    continue;
                }
                TextEntryBrowser.click(cdp, t + " [data-p=\"first\"]");
                String selected = ev(cdp, "[T(" + i + ").querySelectorAll('[data-table-row][data-selected]').length,shown(T(" + i + ").querySelector('[data-table-selection-bar]')),T(" + i + ").querySelector('[data-table-selection-count]').textContent].join(',')");
                if (!selected.equals("1,true,1 selected")) {
                    problems.add(slug + " table " + i + ": after selecting one row (marked rows, bar shown, count) = " + selected);
                }
                TextEntryBrowser.click(cdp, t + " [data-table-clear-selection]");
                String cleared = ev(cdp, "[T(" + i + ").querySelectorAll('[data-table-row][data-selected]').length,shown(T(" + i + ").querySelector('[data-table-selection-bar]')),[].filter.call(T(" + i + ").querySelectorAll('[data-table-select-row]'),function(c){return c.checked}).length].join(',')");
                if (!cleared.equals("0,false,0")) {
                    problems.add(slug + " table " + i + ": after clearing (marked rows, bar shown, checked boxes) = " + cleared);
                }
            }
        }
        counts.put("class and style writes inside tables", Integer.parseInt(ev(cdp, "window.__writes")));
    }

    // ---------------------------------------------------------------- the markup the real templates emit

    /** Every element that has a Table class: its tag and its whole class list. */
    private static List<String[]> tableElements(String html) {
        List<String[]> found = new ArrayList<>();
        Matcher m = CLASS_ATTRIBUTE.matcher(html);
        while (m.find()) {
            if (m.group(2).contains("ash-table")) {
                found.add(new String[] {m.group(1), m.group(2)});
            }
        }
        return found;
    }

    @Test
    void everyElementWithATableClassCarriesOnlyAshlarClassesOnEveryRealPage() {
        TreeSet<String> foreign = new TreeSet<>();
        int total = 0;
        for (Map.Entry<String, String> page : HTML.entrySet()) {
            for (String[] el : tableElements(page.getValue())) {
                total++;
                for (String token : el[1].trim().split("\\s+")) {
                    if (!token.startsWith("ash-")) {
                        foreign.add(page.getKey() + ": " + token + " on <" + el[0] + ">");
                    }
                }
            }
        }
        assertEquals(new TreeSet<String>(), foreign, "a Table element carries a class that is not Ashlar's");
        assertTrue(tableElements(HTML.get("table")).size() > 100 && tableElements(HTML.get("data-table")).size() > 100, "the Table and Data Table pages render many Table elements");
        assertTrue(total > 1000, "table elements over all pages: " + total);
        assertTrue(HTML.values().stream().filter(h -> !tableElements(h).isEmpty()).count() >= 8, "pages that render a Table");
    }

    @Test
    void theLegacyRewriterKnowsEveryTableClassTheRealPagesUseAndLeavesNoneBehind() throws Exception {
        TableLegacyMarkup rewriter = new TableLegacyMarkup();
        Pattern leftover = Pattern.compile("class=\"[^\"]*\\bash-table");
        for (Map.Entry<String, String> page : HTML.entrySet()) {
            String rewritten = rewriter.rewrite(page.getValue());
            assertFalse(leftover.matcher(rewritten).find(), page.getKey() + ": a Table class survived the rewrite");
            boolean hasTables = !tableElements(page.getValue()).isEmpty();
            assertEquals(hasTables, !rewritten.equals(page.getValue()), page.getKey() + ": the rewrite changes exactly the pages that render a Table");
        }
    }

    @Test
    void theRealPagesRenderEveryPartOfTheFamilyAtLeastOnceBetweenThem() {
        TreeSet<String> tokens = new TreeSet<>();
        HTML.values().forEach(h -> tableElements(h).forEach(el -> tokens.addAll(List.of(el[1].trim().split("\\s+")))));
        for (String part : List.of("ash-table", "ash-table__toolbar", "ash-table__selection", "ash-table__clear", "ash-table__search", "ash-table__search-input", "ash-table__scroll", "ash-table__table",
                "ash-table__head", "ash-table__column", "ash-table__column--end", "ash-table__sort", "ash-table__sort-icon", "ash-table__select", "ash-table__checkbox", "ash-table__body", "ash-table__row",
                "ash-table__cell", "ash-table__cell--end", "ash-table__cell--header", "ash-table__cell--mono", "ash-table__cell--nowrap", "ash-table__blank", "ash-table__empty", "ash-table__footer",
                "ash-table__status", "ash-table__pager", "ash-table__page-button", "ash-table__page-indicator", "ash-table__loading", "ash-table__caption", "ash-sr-only")) {
            assertTrue(tokens.contains(part), part + " is rendered by some real page: " + tokens);
        }
    }

    // ---------------------------------------------------------------- the real script on the real pages

    @Test
    void everyTableOnTheRealTableAndDataTablePagesSortsSearchesAndSelectsAsTheContractSays() {
        for (String slug : List.of("table", "data-table")) {
            assertEquals(List.of(), PROBLEMS.get(slug), slug);
            Map<String, Integer> c = COUNTS.get(slug);
            assertTrue(c.get("tables") >= 4, slug + ": " + c);
            assertTrue(c.get("sort buttons") >= 8, slug + ": " + c);
            assertTrue(c.get("searches") >= 1, slug + ": " + c);
            assertTrue(c.get("selections") >= 1, slug + ": " + c);
        }
        assertEquals(1, COUNTS.get("table").get("empty tables"), "the Table page has one table the server rendered empty: " + COUNTS.get("table"));
        assertEquals(1, COUNTS.get("table").get("loading tables"), "and one in the loading state: " + COUNTS.get("table"));
        assertEquals(1, COUNTS.get("data-table").get("empty tables"), "the Data Table page has one table the server rendered empty: " + COUNTS.get("data-table"));
        assertEquals(null, COUNTS.get("data-table").get("loading tables"), COUNTS.get("data-table").toString());
    }

    @Test
    void theRealScriptWritesNoClassAndNoStyleInsideAnyTable() {
        for (String slug : List.of("table", "data-table")) {
            assertEquals(0, COUNTS.get(slug).get("class and style writes inside tables"), slug);
        }
    }
}
