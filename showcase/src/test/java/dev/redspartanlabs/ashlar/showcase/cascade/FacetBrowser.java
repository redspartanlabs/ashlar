package dev.redspartanlabs.ashlar.showcase.cascade;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the Facets suites share: the markup of the three components, the fixtures every suite renders, the in-page measurement, and the trusted browser input.
 *
 * <p>Unlike the older families, this markup is NOT a hand-written mirror of the templates. {@link #facetedSearch}, {@link #facet} and {@link #option} run the REAL templates (the ones the
 * showcase application compiles, {@code ashlar/components/faceted-search.jte} and its two children) through the JTE engine, in this process, so a class or an attribute a template
 * writes is the class or attribute every suite measures, and the {@code FacetContext} hand-off of the field name is the real one. The pages, the comparison and its single
 * normalisation are Text Entry's ({@link TextEntryBrowser}): a computed {@code box-shadow} with only its fully transparent zero-size layers removed.
 */
final class FacetBrowser {

    private FacetBrowser() {
    }

    // ---------------------------------------------------------------- the real templates

    private static final TemplateEngine ENGINE = TemplateEngine.createPrecompiled(ContentType.Html);

    /** One option, as its parameters: label, value, count (null: none), checked, disabled. */
    record Opt(String label, String value, Integer count, boolean checked, boolean disabled) {
        static Opt of(String label, String value) {
            return new Opt(label, value, null, false, false);
        }

        Opt count(int count) {
            return new Opt(label, value, count, checked, disabled);
        }

        Opt on() {
            return new Opt(label, value, count, true, disabled);
        }

        Opt off() {
            return new Opt(label, value, count, checked, true);
        }
    }

    /** One facet: its title, its field name and its options. */
    record Fac(String title, String name, List<Opt> options) {
        static Fac of(String title, String name, Opt... options) {
            return new Fac(title, name, List.of(options));
        }
    }

    private static final String COMPONENTS = "ashlar/components/";

    /** The Facet Option template, rendered as it is called with these parameters (the field name comes from the enclosing facet, if any). */
    static String option(Opt o) {
        StringOutput out = new StringOutput();
        ENGINE.render(COMPONENTS + "facet-option.jte", optionParams(o), out);
        return out.toString();
    }

    /** The Facet template, with a Facet Option template call for each option as its content. */
    static String facet(Fac f) {
        gg.jte.Content content = output -> {
            for (Opt o : f.options()) {
                ENGINE.render(COMPONENTS + "facet-option.jte", optionParams(o), output);
            }
        };
        StringOutput out = new StringOutput();
        ENGINE.render(COMPONENTS + "facet.jte", Map.of("title", f.title(), "name", f.name(), "content", content), out);
        return out.toString();
    }

    private static Map<String, Object> optionParams(Opt o) {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("label", o.label());
        params.put("value", o.value());
        params.put("count", o.count());
        params.put("checked", o.checked());
        params.put("disabled", o.disabled());
        return params;
    }

    /** The Faceted Search template, with a Facet template call for each facet as its content. */
    static String facetedSearch(List<Fac> facets) {
        gg.jte.Content content = output -> {
            for (Fac f : facets) {
                Map<String, Object> params = Map.of("title", f.title(), "name", f.name(), "content", (gg.jte.Content) out -> {
                    for (Opt o : f.options()) {
                        ENGINE.render(COMPONENTS + "facet-option.jte", optionParams(o), out);
                    }
                });
                ENGINE.render(COMPONENTS + "facet.jte", params, output);
            }
        };
        StringOutput out = new StringOutput();
        ENGINE.render(COMPONENTS + "faceted-search.jte", Map.of("content", content), out);
        return out.toString();
    }

    private static final Pattern BETWEEN_TAGS = Pattern.compile("\\s*([<>])\\s*");

    /** The markup with every run of whitespace collapsed to one space and none kept next to a tag boundary: the comparison form of the golden files. */
    static String normalized(String html) {
        return BETWEEN_TAGS.matcher(html.replaceAll("\\s+", " ").trim()).replaceAll("$1");
    }

    // ---------------------------------------------------------------- the parity fixtures

    /** A field: its facets (or one bare option, rendered outside any facet), the width of its column (0: the page's), and the pseudo-classes forced on {@code target} while it is measured. */
    record Case(String key, List<Fac> facets, Opt bare, int width, List<String> pseudo, String target) {
    }

    static final List<Case> CASES = new ArrayList<>();

    private static final Opt LONG = Opt.of("Scheduled maintenance window for the regional data center cluster", "scheduled").count(8);
    private static final Opt UNBROKEN = Opt.of("ACB01ACB02ACB03ACB04ACB05ACB06ACB07ACB08ACB09ACB10", "unbroken").count(12);

    /** The facet the state cases (hover, focus, active) are measured on: a plain option, a checked one with a count, a disabled one. */
    private static Fac states() {
        return Fac.of("Status", "status", Opt.of("Running", "running").count(42).on(), Opt.of("Ready", "ready").count(18), Opt.of("Failed", "failed").count(3),
                Opt.of("Maintenance", "maintenance").count(0).off());
    }

    static String input(String value) {
        return "input[value=\"" + value + "\"]";
    }

    static String row(String value) {
        return "label:has(> input[value=\"" + value + "\"])";
    }

    private static void add(String key, int width, List<String> pseudo, String target, Fac... facets) {
        CASES.add(new Case(key, List.of(facets), null, width, pseudo, target));
    }

    static {
        add("single facet", 0, List.of(), null, Fac.of("Status", "status", Opt.of("Running", "running"), Opt.of("Ready", "ready"), Opt.of("Failed", "failed")));
        add("counts and a checked option", 0, List.of(), null, states());
        add("zero count, one and many", 0, List.of(), null,
                Fac.of("Status", "status", Opt.of("None", "none").count(0), Opt.of("One", "one").count(1), Opt.of("Two", "two").count(2), Opt.of("A lot", "many").count(12345)));
        add("missing counts", 0, List.of(), null, Fac.of("Type", "type", Opt.of("Batch", "batch").count(96), Opt.of("Interactive", "interactive"), Opt.of("Scheduled", "scheduled").count(8)));
        add("disabled", 0, List.of(), null, Fac.of("Status", "status", Opt.of("Running", "running"), Opt.of("Maintenance", "maintenance").count(0).off(), Opt.of("Offline", "offline").off()));
        add("disabled and checked", 0, List.of(), null, Fac.of("Status", "status", Opt.of("Running", "running").on(), Opt.of("Maintenance", "maintenance").count(0).on().off()));
        add("long label that wraps", 200, List.of(), null, Fac.of("Type", "type", LONG, Opt.of("Batch", "batch").count(96)));
        add("long label without a break", 200, List.of(), null, Fac.of("Machine", "machine", UNBROKEN, Opt.of("ACB-01", "acb-01").count(82)));
        add("narrow column", 140, List.of(), null, Fac.of("Machine", "machine", Opt.of("ACB-01", "acb-01").count(82), Opt.of("ACB-02", "acb-02").count(61).on(), Opt.of("ACB-03", "acb-03").count(44).off()));
        add("several facets", 0, List.of(), null, Fac.of("Status", "status", Opt.of("Running", "running").count(42).on(), Opt.of("Ready", "ready").count(18), Opt.of("Failed", "failed").count(3)),
                Fac.of("Machine", "machine", Opt.of("ACB-01", "acb-01").count(82), Opt.of("ACB-02", "acb-02").count(61)), LONG_FACET());
        add("text that needs escaping", 0, List.of(), null, Fac.of("Ops & \"Support\"", "ops", Opt.of("Tier <1> & 2", "tier").count(3), Opt.of("It's \"quoted\"", "a\"b")));
        add("facet without options", 0, List.of(), null, Fac.of("Empty", "empty"));
        CASES.add(new Case("option outside any facet", List.of(), Opt.of("Orphan", "orphan").count(5), 0, List.of(), null));
        add("hover on an option", 0, List.of("hover"), row("ready"), states());
        add("hover on a checked option", 0, List.of("hover"), row("running"), states());
        add("hover on a disabled option", 0, List.of("hover"), row("maintenance"), states());
        add("hover on the legend", 0, List.of("hover"), "legend", states());
        add("active on an option", 0, List.of("hover", "active"), row("ready"), states());
        add("focus-visible on an option", 0, List.of("focus", "focus-visible"), input("ready"), states());
        add("focus-visible on a checked option", 0, List.of("focus", "focus-visible"), input("running"), states());
        add("focus-visible on a disabled option", 0, List.of("focus", "focus-visible"), input("maintenance"), states());
        add("focus only", 0, List.of("focus"), input("ready"), states());
    }

    private static Fac LONG_FACET() {
        return Fac.of("Type", "type", Opt.of("Batch", "batch").count(96), Opt.of("Interactive", "interactive").count(71), LONG);
    }

    /** The field's markup: the real templates, in a column of the case's width (a raw element: it carries no class and is not part of the comparison). */
    static String build(Case c, String id) {
        String inner = c.bare() != null ? option(c.bare()) : facetedSearch(c.facets());
        String width = c.width() > 0 ? " style=\"width:" + c.width() + "px\"" : "";
        return "<div data-field=\"" + id + "\" data-parity=\"raw\"" + width + ">" + inner + "</div>\n";
    }

    private static final Pattern TAG_START = Pattern.compile("<(label|div|span|input|fieldset|legend)(?=[\\s>])");

    /** Marks every element of the component so the scoped legacy stylesheet can address exactly those. */
    static String marked(String html) {
        Matcher m = TAG_START.matcher(html);
        StringBuilder out = new StringBuilder();
        int at = 0;
        while (m.find()) {
            out.append(html, at, m.end());
            if (!html.substring(m.end(), html.indexOf('>', m.end())).contains("data-parity")) {
                out.append(" data-parity");
            }
            at = m.end();
        }
        return out.append(html.substring(at)).toString();
    }

    /** The selector the forced pseudo-classes go on, inside the field. */
    static String target(Case c, String id) {
        return "[data-field=\"" + id + "\"] " + c.target();
    }

    // ---------------------------------------------------------------- measurement

    static final List<String> PROPERTIES = new ArrayList<>(TextEntryBrowser.PROPERTIES);

    static {
        PROPERTIES.addAll(List.of("accent-color", "flex-wrap", "flex-direction", "flex-basis", "outline-offset", "outline-color", "color-scheme", "min-height", "overflow-wrap", "word-break",
                "font-variant-numeric", "clip-path", "visibility", "z-index", "transition-property", "transition-duration", "animation-name"));
    }

    private static String jsArray(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(i == 0 ? "" : ",").append('\'').append(values.get(i)).append('\'');
        }
        return sb.append(']').toString();
    }

    /** One line per element and property (tab separated: field, element position and tag, property, value), plus every element's box. */
    static String measureScript(String fieldId) {
        return "(function(){var props=" + jsArray(PROPERTIES) + ";var out=[];var root=document.querySelector('[data-field=\"" + fieldId + "\"]');"
                + "var els=[root].concat([].slice.call(root.querySelectorAll('*')));"
                + "els.forEach(function(e,n){var s=getComputedStyle(e);var name='" + fieldId + ":'+n+':'+e.tagName.toLowerCase();"
                + "props.forEach(function(p){out.push([name,p,s.getPropertyValue(p)].join('\\t'))});"
                + "var r=e.getBoundingClientRect();out.push([name,'@x',String(Math.round(r.x*100)/100)].join('\\t'));out.push([name,'@y',String(Math.round(r.y*100)/100)].join('\\t'));"
                + "out.push([name,'@w',String(Math.round(r.width*100)/100)].join('\\t'));out.push([name,'@h',String(Math.round(r.height*100)/100)].join('\\t'));});"
                + "return out.join('\\n')})()";
    }

    // ---------------------------------------------------------------- trusted input

    /** {@code key:Tab}, {@code key:Shift+Tab}, {@code key:Space}, {@code click:selector}, {@code focus:id} (a script focus) or {@code none}. */
    static void perform(DevToolsSession cdp, String action) throws Exception {
        ChoiceBrowser.perform(cdp, action);
    }
}
