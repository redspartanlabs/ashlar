package dev.redspartanlabs.ashlar.showcase.cascade;

import dev.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog;
import dev.redspartanlabs.ashlar.showcase.catalog.ComponentPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The static parity gates of Tree and Tree Item (migrated together), measured in real Chrome on the shared parity harness (unchanged). Hover and keyboard focus are
 * {@link TreeStateParityTest}; the script is {@link TreeScriptTest} and {@link TreeLegacyBehaviourTest}; the page without Tailwind is {@link TreeTailwindFreeTest}.
 *
 * <p>Evidence is a controlled oracle: the components' legacy Tailwind markup (compiled by the real Tailwind CLI) next to their migrated markup, in the same browser, for each
 * of the 22 theme and skin configurations, with Tailwind present and with Tailwind removed. The legacy markup is the migrated markup rewritten by {@link TreeLegacyMarkup},
 * so both sides are the same trees in the same state. The cases cover a flat tree, a collapsed and an expanded parent, links, icons, a three-level tree with every item
 * expanded and with every item collapsed, an expanded parent with a collapsed child, a collapsed parent with an expanded child and grandchild (the case where a state
 * selector written with a descendant combinator would leak), mixed siblings, a parent that is also a link, and a 220px column whose long labels truncate.
 *
 * <p>The tree has no breakpoint, so every configuration is measured at 440px, and the four theme and skin configurations that matter for non-colour properties are also
 * measured at 320 and 900px. The whole measurement is repeated once with Chrome forced to {@code prefers-reduced-motion: reduce}. Negative controls show each gate can fail.
 *
 * <p>Evidence is Chrome only. Computed-style and box parity is the visual evidence; there is no screenshot comparison.
 */
class TreeParityTest {

    private static final String ORACLE = ParityFrames.generated("tree-oracle.css");
    private static final String SCOPED = ParityFrames.generated("tree-legacy-scoped.css");
    private static final List<ParityConfigs.Config> CONFIGS = ParityConfigs.all();
    private static final List<ParityConfigs.Config> EDGE_CONFIGS = ParityConfigs.reduced();
    private static final List<Integer> WIDTHS = List.of(440);
    private static final List<Integer> EDGE_WIDTHS = List.of(320, 900);
    private static final List<String> EXTRA_PROPS = List.of("font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform", "text-align", "text-decoration-line",
            "text-overflow", "white-space", "cursor", "appearance", "pointer-events", "transition-property", "transition-duration", "transition-delay", "transition-timing-function", "row-gap",
            "column-gap", "align-items", "justify-content", "flex-direction", "flex-grow", "flex-basis", "min-width", "outline-style", "background-image", "list-style-type", "list-style-position",
            "rotate", "grid-template-rows", "vertical-align", "box-shadow");

    /** One tree node: a leaf when {@code children} is empty, a parent otherwise. */
    private record Node(String id, String label, boolean icon, boolean link, boolean expanded, List<Node> children) {
    }

    /** One tree and the layout it sits in. */
    private record Group(String name, List<Node> roots, String layout) {
    }

    private static Node leaf(String id, String label) {
        return new Node(id, label, false, false, false, List.of());
    }

    private static Node leaf(String id, String label, boolean icon, boolean link) {
        return new Node(id, label, icon, link, false, List.of());
    }

    private static Node parent(String id, String label, boolean expanded, boolean icon, boolean link, Node... children) {
        return new Node(id, label, icon, link, expanded, List.of(children));
    }

    private static final List<Group> GROUPS = List.of(
            new Group("flat", List.of(leaf("one", "Fruit"), leaf("two", "Vegetables", true, false), leaf("three", "Grains")), "block"),
            new Group("closed", List.of(parent("src", "src", false, true, false, leaf("a", "app.js", true, true), leaf("b", "main.css", true, true)), leaf("readme", "README.md", true, true)), "block"),
            new Group("open", List.of(parent("src", "src", true, true, false, leaf("a", "button.jte", true, true), leaf("b", "icon.jte"),
                    parent("pages", "pages", false, true, false, leaf("home", "home.jte", true, true))), leaf("readme", "README.md", true, true)), "block"),
            new Group("deep", List.of(parent("a", "Alpha", true, true, false, parent("b", "Beta", true, true, false, parent("c", "Gamma", true, true, false, leaf("d", "Delta", true, true)), leaf("e", "Epsilon")),
                    leaf("f", "Zeta")), leaf("g", "Eta")), "block"),
            new Group("allclosed", List.of(parent("a", "Alpha", false, false, false, parent("b", "Beta", false, false, false, leaf("c", "Gamma"))), parent("d", "Delta", false, false, false, leaf("e", "Epsilon"))), "block"),
            new Group("leaka", List.of(parent("a", "Alpha", true, false, false, parent("b", "Beta", false, false, false, leaf("c", "Gamma"), leaf("d", "Delta")), leaf("e", "Epsilon")), leaf("f", "Zeta")), "block"),
            new Group("leakb", List.of(parent("a", "Alpha", false, false, false, parent("b", "Beta", true, false, false, parent("c", "Gamma", true, false, false, leaf("d", "Delta")), leaf("e", "Epsilon"))),
                    leaf("f", "Zeta")), "block"),
            new Group("mixed", List.of(parent("a", "Alpha", true, true, false, leaf("b", "Beta"), parent("c", "Gamma", false, true, false, leaf("d", "Delta"))), parent("e", "Epsilon", false, true, false, leaf("f", "Zeta")),
                    parent("g", "Eta", true, true, false, parent("h", "Theta", true, true, false, parent("i", "Iota", false, true, false, leaf("j", "Kappa"))))), "block"),
            new Group("links", List.of(parent("a", "Linked parent, collapsed", false, true, true, leaf("b", "Child", true, true)), parent("c", "Linked parent, expanded", true, true, true, leaf("d", "Child", true, true)),
                    leaf("e", "A link leaf", true, true)), "block"),
            new Group("narrow", List.of(parent("a", "A rather long label that has to be cut off with an ellipsis", true, true, false,
                    leaf("b", "A rather long link label that has to be cut off too", true, true), leaf("c", "Short")), leaf("d", "Another long label which also gets truncated in a column")), "narrow"));

    // ---- state of the measurement ----

    private static TreeLegacyMarkup treeLegacy;
    private static IconLegacyMarkup iconLegacy;
    /** The measurements the value assertions read, by configuration id: light and dark without a skin. */
    private static final Set<String> RETAINED_IDS = Set.of("light/none", "dark/none");
    private static final Map<String, ParityProbe.Probe> RETAINED = new LinkedHashMap<>();
    private static ParityProbe.Probe reduced;
    private static final Map<String, List<ParityProbe.Finding>> PRESENT = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_SCOPED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REMOVED_FULL = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> SCOPED_FIDELITY = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> REDUCED = new LinkedHashMap<>();
    private static final Map<String, List<ParityProbe.Finding>> CONTROLS = new LinkedHashMap<>();
    private static final Map<String, Integer> APPLIED_PRESENT = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REMOVED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_SCOPED = new TreeMap<>();
    private static final Map<String, Integer> APPLIED_REDUCED = new TreeMap<>();
    private static final Map<String, Integer> ROWS = new LinkedHashMap<>();
    private static String migratedForCounting;

    // ---- markup (what the two templates render, written out) ----

    private static String mark(String kind, String name) {
        return " data-parity=\"" + kind + "\" data-parity-case=\"" + name + "\"";
    }

    private static final String SVG = "<svg class=\"ash-icon ash-icon--sm\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" aria-hidden=\"true\"";

    private static String item(Group g, Node node, int level) {
        String name = g.name() + "-" + node.id();
        boolean parent = !node.children().isEmpty();
        StringBuilder sb = new StringBuilder("<li role=\"treeitem\"");
        if (parent) {
            sb.append(" aria-expanded=\"").append(node.expanded()).append("\"");
        }
        sb.append(" aria-level=\"").append(level).append("\" tabindex=\"-1\" data-tree-item class=\"ash-tree__item\"").append(mark("item", name)).append(">");
        sb.append("<div class=\"ash-tree__row\"").append(mark("row", name)).append(">");
        if (parent) {
            sb.append("<button type=\"button\" tabindex=\"-1\" aria-hidden=\"true\" data-tree-toggle class=\"ash-tree__toggle\"").append(mark("toggle", name)).append(">");
            sb.append("<span data-tree-chevron class=\"ash-tree__chevron\"").append(mark("chevron", name)).append(">").append(SVG).append(mark("chevron-svg", name))
                    .append("><path d=\"M9 5l7 7-7 7\"/></svg></span></button>");
        } else {
            sb.append("<span class=\"ash-tree__spacer\" aria-hidden=\"true\"").append(mark("spacer", name)).append("></span>");
        }
        if (node.icon()) {
            sb.append("<span class=\"ash-tree__icon\" aria-hidden=\"true\"").append(mark("icon", name)).append(">").append(SVG).append(mark("icon-svg", name))
                    .append("><path d=\"M4 4h16v16H4z\"/></svg></span>");
        }
        if (node.link()) {
            sb.append("<a href=\"#").append(node.id()).append("\" tabindex=\"-1\" class=\"ash-tree__label\"").append(mark("label", name)).append(">").append(node.label()).append("</a>");
        } else {
            sb.append("<span class=\"ash-tree__label\"").append(mark("label", name)).append(">").append(node.label()).append("</span>");
        }
        sb.append("</div>");
        if (parent) {
            sb.append("<div data-tree-children").append(node.expanded() ? "" : " inert").append(" class=\"ash-tree__panel\"").append(mark("panel", name)).append(">");
            sb.append("<div class=\"ash-tree__clip\"").append(mark("clip", name)).append("><ul role=\"group\" class=\"ash-tree__group\"").append(mark("group", name)).append(">");
            node.children().forEach(c -> sb.append(item(g, c, level + 1)));
            sb.append("</ul></div></div>");
        }
        return sb.append("</li>").toString();
    }

    private static String tree(Group g) {
        StringBuilder sb = new StringBuilder("<ul id=\"" + g.name() + "\" role=\"tree\" data-tree class=\"ash-tree\"" + mark("tree", g.name()) + ">");
        g.roots().forEach(n -> sb.append(item(g, n, 1)));
        return sb.append("</ul>").toString();
    }

    /** The trees a negative control is measured on: enough to hold every property a control breaks, small enough that sixty-odd frames stay within the test heap. */
    private static final Set<String> CONTROL_GROUPS = Set.of("flat", "open", "leaka", "leakb", "narrow", "links");

    private static String markup(boolean legacyMarkup) {
        return markup(legacyMarkup, GROUPS);
    }

    private static String markup(boolean legacyMarkup, List<Group> groups) {
        StringBuilder sb = new StringBuilder();
        for (Group g : groups) {
            String style = g.layout().equals("narrow") ? "box-sizing:border-box;width:220px" : "box-sizing:border-box";
            sb.append("<div style=\"margin:4px 0\"><div data-layout=\"").append(g.layout()).append("\" style=\"").append(style).append("\">").append(tree(g)).append("</div></div>");
        }
        String html = sb.toString();
        return legacyMarkup ? iconLegacy.rewrite(treeLegacy.rewrite(html)) : html;
    }

    // ---- frames ----

    private static String key(ParityConfigs.Config c, String phase, int width) {
        return c.id() + "|" + phase + "|" + width;
    }

    private static String sized(String frame, int width) {
        return frame.replace("width:440px", "width:" + width + "px");
    }

    private static String frameFor(ParityConfigs.Config c, String phase, int width, String environment, String migrated, String legacyMarkup) {
        String id = key(c, phase, width);
        switch (phase) {
            case "L":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", legacyMarkup), width);
            case "P":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), "", migrated), width);
            case "R":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(ParityFrames.ASHLAR, ParityConfigs.SKIN_LINKS), environment, migrated), width);
            case "S":
                return sized(ParityFrames.frame(id, c, ParityFrames.with(SCOPED, ParityConfigs.SKIN_LINKS), environment, legacyMarkup), width);
            default:
                throw new IllegalArgumentException(phase);
        }
    }

    // ---- control experiments: each breaks the migrated tree in ONE way and must be detected ----

    private record Control(String name, String config, int width, String css, String expectedProperty) {
    }

    private static final String CHEVRON_TURN = ".ash-tree__item[aria-expanded=\"true\"] > .ash-tree__row > .ash-tree__toggle > .ash-tree__chevron";

    private static final List<Control> CONTROL_LIST = List.of(
            new Control("the root gap is wrong", "light/none", 440, ".ash-tree{gap:0.25rem}", "row-gap"),
            new Control("the root is not a column", "light/none", 440, ".ash-tree{flex-direction:row}", "flex-direction"),
            new Control("the root text is larger", "light/none", 440, ".ash-tree{font-size:1rem}", "font-size"),
            new Control("the root line height is wrong", "light/none", 440, ".ash-tree{--ash-tree-leading:1.6}", "line-height"),
            new Control("the root keeps its list marker", "light/none", 440, ".ash-tree{list-style:disc}", "list-style-type"),
            new Control("the root keeps a browser margin", "light/none", 440, ".ash-tree{margin:8px}", "margin-top"),
            new Control("the root keeps a browser padding", "light/none", 440, ".ash-tree{padding:8px}", "padding-left"),
            new Control("an item keeps its list marker", "light/none", 440, ".ash-tree__item{list-style:disc}", "list-style-type"),
            new Control("a row inline padding is wrong", "light/none", 440, ".ash-tree__row{padding-inline:4px}", "padding-left"),
            new Control("a row block padding is wrong", "light/none", 440, ".ash-tree__row{padding-block:4px}", "padding-top"),
            new Control("a row gap is wrong", "light/none", 440, ".ash-tree__row{gap:0.5rem}", "column-gap"),
            new Control("a row radius is wrong", "light/none", 440, ".ash-tree__row{border-radius:0}", "border-top-left-radius"),
            new Control("a row text colour is wrong", "light/none", 440, ".ash-tree__row{--ash-tree-fg:red}", "color"),
            new Control("a row dark text ignores the theme", "dark/none", 440, ".ash-tree__row{--ash-tree-fg:var(--color-slate-700)}", "color"),
            new Control("the toggle is the wrong size", "light/none", 440, ".ash-tree__toggle{width:1rem;height:1rem}", "width"),
            new Control("the toggle may shrink", "light/none", 440, ".ash-tree__toggle{flex-shrink:1}", "flex-shrink"),
            new Control("the toggle radius is wrong", "light/none", 440, ".ash-tree__toggle{border-radius:0}", "border-top-left-radius"),
            new Control("the toggle is not a pointer", "light/none", 440, ".ash-tree__toggle{cursor:default}", "cursor"),
            new Control("the toggle colour is wrong", "light/none", 440, ".ash-tree__row{--ash-tree-toggle:red}", "color"),
            new Control("the toggle dark colour ignores the theme", "dark/none", 440, ".ash-tree__row{--ash-tree-toggle:var(--color-slate-400)}", "color"),
            new Control("the toggle loses its native appearance", "light/none", 440, ".ash-tree__toggle{appearance:none}", "appearance"),
            new Control("the toggle keeps the browser font", "light/none", 440, ".ash-tree__toggle{font:13.333px Arial}", "font-size"),
            new Control("the toggle has a border", "light/none", 440, ".ash-tree__toggle{border-width:2px}", "border-top-width"),
            new Control("the toggle has the browser background", "light/none", 440, ".ash-tree__toggle{background-color:buttonface}", "background-color"),
            new Control("the toggle has padding", "light/none", 440, ".ash-tree__toggle{padding:4px}", "padding-top"),
            new Control("an expanded chevron does not turn", "light/none", 440, CHEVRON_TURN + "{rotate:none}", "rotate"),
            new Control("an expanded chevron turns the wrong way", "light/none", 440, CHEVRON_TURN + "{rotate:-90deg}", "rotate"),
            new Control("a collapsed chevron turns", "light/none", 440, ".ash-tree__chevron{rotate:90deg}", "rotate"),
            new Control("a chevron turns for an expanded ANCESTOR (descendant selector)", "light/none", 440, ".ash-tree__item[aria-expanded=\"true\"] .ash-tree__chevron{rotate:90deg}", "rotate"),
            new Control("the chevron transition is slower", "light/none", 440, ".ash-tree__chevron{transition-duration:300ms}", "transition-duration"),
            new Control("the chevron transition animates the wrong properties", "light/none", 440, ".ash-tree__chevron{transition-property:color}", "transition-property"),
            new Control("the chevron easing is wrong", "light/none", 440, ".ash-tree__chevron{transition-timing-function:linear}", "transition-timing-function"),
            new Control("the leaf spacer is the wrong size", "light/none", 440, ".ash-tree__spacer{width:0.5rem}", "width"),
            new Control("the leaf spacer may shrink", "light/none", 440, ".ash-tree__spacer{flex-shrink:1}", "flex-shrink"),
            new Control("the item icon may shrink", "light/none", 440, ".ash-tree__icon{flex-shrink:1}", "flex-shrink"),
            new Control("the item icon colour is wrong", "light/none", 440, ".ash-tree__row{--ash-tree-icon:red}", "color"),
            new Control("the item icon dark colour ignores the theme", "dark/none", 440, ".ash-tree__row{--ash-tree-icon:var(--color-slate-400)}", "color"),
            new Control("the label weight is wrong", "light/none", 440, ".ash-tree__label{font-weight:700}", "font-weight"),
            new Control("the label colour is wrong", "light/none", 440, ".ash-tree__row{--ash-tree-label:red}", "color"),
            new Control("the label dark colour ignores the theme", "dark/none", 440, ".ash-tree__row{--ash-tree-label:var(--color-slate-900)}", "color"),
            new Control("the label does not truncate", "light/none", 440, ".ash-tree__label{text-overflow:clip}", "text-overflow"),
            new Control("the label wraps", "light/none", 440, ".ash-tree__label{white-space:normal}", "white-space"),
            new Control("the label cannot shrink", "light/none", 440, ".ash-tree__label{min-width:auto}", "min-width"),
            new Control("the label does not fill the row", "light/none", 440, ".ash-tree__label{flex:0 0 auto}", "flex-grow"),
            new Control("a link label is underlined", "light/none", 440, "a.ash-tree__label{text-decoration:underline}", "text-decoration-line"),
            new Control("a link label is not a pointer", "light/none", 440, "a.ash-tree__label{cursor:default}", "cursor"),
            new Control("a link label radius is wrong", "light/none", 440, "a.ash-tree__label{border-radius:0}", "border-top-left-radius"),
            new Control("the group indent is wrong", "light/none", 440, ".ash-tree__group{padding-left:0.5rem}", "padding-left"),
            new Control("the group keeps its list marker", "light/none", 440, ".ash-tree__group{list-style:disc}", "list-style-type"),
            new Control("the group keeps a browser margin", "light/none", 440, ".ash-tree__group{margin:8px}", "margin-top"),
            new Control("the panel transition is slower", "light/none", 440, ".ash-tree__panel{transition-duration:300ms}", "transition-duration"),
            new Control("the panel transition animates the wrong property", "light/none", 440, ".ash-tree__panel{transition-property:all}", "transition-property"),
            new Control("the panel easing is wrong", "light/none", 440, ".ash-tree__panel{transition-timing-function:linear}", "transition-timing-function"),
            new Control("a collapsed panel stays open", "light/none", 440, ".ash-tree__panel[inert]{grid-template-rows:1fr}", "grid-template-rows"),
            new Control("an open panel collapses", "light/none", 440, ".ash-tree__panel:not([inert]){grid-template-rows:0fr}", "grid-template-rows"),
            new Control("a panel collapses because an ANCESTOR item is collapsed (descendant selector)", "light/none", 440, ".ash-tree__item[aria-expanded=\"false\"] .ash-tree__panel{grid-template-rows:0fr}", "grid-template-rows"),
            new Control("the panel is not a grid", "light/none", 440, ".ash-tree__panel{display:block}", "display"),
            new Control("the clip does not clip", "light/none", 440, ".ash-tree__clip{overflow:visible}", "overflow-x"),
            new Control("the clip may not shrink", "light/none", 440, ".ash-tree__clip{min-height:auto}", "min-height"));

    // ---- running ----

    /** The frames of a run. The reduced-motion run compares the legacy and the migrated rendering with Tailwind present only, so it needs those two phases at 440px. */
    private static String framesFor(String environment, String migrated, String legacyMarkup, boolean reducedMotionRun) {
        List<String> phases = reducedMotionRun ? List.of("L", "P") : List.of("L", "P", "R", "S");
        StringBuilder frames = new StringBuilder();
        for (ParityConfigs.Config c : CONFIGS) {
            for (int width : WIDTHS) {
                for (String phase : phases) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
        }
        for (ParityConfigs.Config c : reducedMotionRun ? List.<ParityConfigs.Config>of() : EDGE_CONFIGS) {
            for (int width : EDGE_WIDTHS) {
                for (String phase : phases) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
        }
        return frames.toString();
    }

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("tree-parity");
        treeLegacy = new TreeLegacyMarkup();
        iconLegacy = new IconLegacyMarkup();
        String environment = ParityFrames.environment(session.generatedDir());
        String migrated = markup(false);
        String legacyMarkup = markup(true);
        migratedForCounting = migrated;
        List<Group> controlGroups = GROUPS.stream().filter(g -> CONTROL_GROUPS.contains(g.name())).toList();
        String controlMigrated = markup(false, controlGroups);
        String controlLegacy = markup(true, controlGroups);

        // One configuration at a time: the measurement of a configuration is compared and then let go of, so the test heap holds a few frames, not the whole matrix. Only the two
        // configurations the value assertions read (light and dark, no skin) are kept.
        for (ParityConfigs.Config c : CONFIGS) {
            StringBuilder frames = new StringBuilder();
            for (String phase : List.of("L", "P", "R", "S")) {
                frames.append(frameFor(c, phase, 440, environment, migrated, legacyMarkup));
            }
            ParityProbe.Probe measured = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "tree frames " + c.id());
            compareAll(measured, c, 440);
            if (RETAINED_IDS.contains(c.id())) {
                RETAINED.put(c.id(), measured);
            }
        }
        for (ParityConfigs.Config c : EDGE_CONFIGS) {
            StringBuilder frames = new StringBuilder();
            for (int width : EDGE_WIDTHS) {
                for (String phase : List.of("L", "P", "R", "S")) {
                    frames.append(frameFor(c, phase, width, environment, migrated, legacyMarkup));
                }
            }
            ParityProbe.Probe measured = ParityFrames.measure(session, frames.toString(), EXTRA_PROPS, "tree edge frames " + c.id());
            for (int width : EDGE_WIDTHS) {
                compareAll(measured, c, width);
            }
        }
        StringBuilder controlFrames = new StringBuilder();
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            controlFrames.append(sized(ParityFrames.frame("ctl:" + control.name() + "|P", c, ParityFrames.with(ParityFrames.APP, ParityConfigs.SKIN_LINKS), control.css(), controlMigrated), control.width()));
        }
        for (String id : List.of("light/none", "dark/none")) {
            controlFrames.append(sized(ParityFrames.frame("ctlL:" + id, ParityConfigs.get(id), ParityFrames.with(ORACLE, ParityConfigs.SKIN_LINKS), "", controlLegacy), 440));
        }
        ParityProbe.Probe controls = ParityFrames.measure(session, controlFrames.toString(), EXTRA_PROPS, "tree control frames");
        for (Control control : CONTROL_LIST) {
            ParityConfigs.Config c = ParityConfigs.get(control.config());
            CONTROLS.put(control.name(), ParityProbe.compare("control: " + control.name(), controls.props(), controls.phase("ctlL:" + c.id()), controls.phase("ctl:" + control.name() + "|P"), new TreeMap<>()));
        }

        // The same measurement with Chrome forced to prefers-reduced-motion: reduce: the legacy motion-reduce:transition-none and the migrated media query must agree.
        reduced = ParityFrames.measure(session, framesFor(environment, migrated, legacyMarkup, true), EXTRA_PROPS, List.of("--force-prefers-reduced-motion"), "tree reduced-motion frames");
        for (ParityConfigs.Config c : CONFIGS) {
            String at = c.id() + " @440 (reduced motion)";
            REDUCED.put(at, ParityProbe.compare(at + " (Tailwind present)", reduced.props(), reduced.phase(key(c, "L", 440)), reduced.phase(key(c, "P", 440)), APPLIED_REDUCED));
        }
    }

    private static void compareAll(ParityProbe.Probe probe, ParityConfigs.Config c, int width) {
        List<String> props = probe.props();
        String at = c.id() + " @" + width;
        List<ParityProbe.Row> legacyRows = probe.phase(key(c, "L", width));
        ROWS.put(key(c, "L", width), legacyRows.size());
        PRESENT.put(at, ParityProbe.compare(at + " (Tailwind present)", props, legacyRows, probe.phase(key(c, "P", width)), APPLIED_PRESENT));
        REMOVED_SCOPED.put(at, ParityProbe.compare(at + " (Tailwind removed vs scoped legacy)", props, probe.phase(key(c, "S", width)), probe.phase(key(c, "R", width)), APPLIED_REMOVED));
        REMOVED_FULL.put(at, ParityProbe.compare(at + " (Tailwind removed vs full legacy, the components' own elements)", props, legacyRows, probe.phase(key(c, "R", width)), APPLIED_REMOVED));
        SCOPED_FIDELITY.put(at, ParityProbe.compare(at + " (scoped legacy vs full legacy, the components' own elements)", props, legacyRows, probe.phase(key(c, "S", width)), APPLIED_SCOPED));
    }

    /** The measurements are tens of thousands of rows; every suite in this JVM is run one after another, so this one lets go of them when it is done. */
    @AfterAll
    static void clear() {
        RETAINED.clear();
        reduced = null;
        migratedForCounting = null;
        List.of(PRESENT, REMOVED_SCOPED, REMOVED_FULL, SCOPED_FIDELITY, REDUCED, CONTROLS).forEach(Map::clear);
        List.of(APPLIED_PRESENT, APPLIED_REMOVED, APPLIED_SCOPED, APPLIED_REDUCED, ROWS).forEach(Map::clear);
    }

    private static List<String> lines(Map<String, List<ParityProbe.Finding>> findings) {
        // A broken stylesheet makes tens of thousands of findings; the first few say what moved, and building a message from all of them runs out of heap.
        List<String> out = new ArrayList<>();
        long total = 0;
        for (List<ParityProbe.Finding> list : findings.values()) {
            for (ParityProbe.Finding f : list) {
                if (out.size() < 20) {
                    out.add(f.line());
                }
                total++;
            }
        }
        if (total > out.size()) {
            out.add("(" + (total - out.size()) + " more findings not shown)");
        }
        return out;
    }

    private static String layoutOf(String groupName) {
        return GROUPS.stream().filter(g -> g.name().equals(groupName)).findFirst().orElseThrow().layout();
    }

    /** One measured value: kind, group, node id (or null for the tree), property. */
    private static String one(ParityProbe.Probe from, String phase, String kind, String group, String nodeId, String property) {
        String caseName = nodeId == null ? group : group + "-" + nodeId;
        List<ParityProbe.Row> found = from.phase(phase).stream().filter(r -> r.kind().equals(kind) && r.variant().equals(caseName + "/" + layoutOf(group))).toList();
        assertEquals(1, found.size(), phase + " " + kind + " " + caseName);
        return from.value(found.get(0), property);
    }

    private static String one(String phase, String kind, String group, String nodeId, String property) {
        return one(RETAINED.get(phase.substring(0, phase.indexOf('|'))), phase, kind, group, nodeId, property);
    }

    private static double number(String px) {
        return Double.parseDouble(px.replace("px", ""));
    }

    private static final List<String> RENDERINGS = List.of("light/none|L|440", "light/none|P|440", "light/none|R|440", "light/none|S|440");

    // ---- tests ----

    @Test
    void theConfigurationsAreTheTwentyTwoThemeAndSkinCombinationsAndTheWidthsAreMeasured() {
        assertEquals(22, CONFIGS.size());
        assertEquals(4, EDGE_CONFIGS.size());
        assertEquals(22 * WIDTHS.size() + 4 * EDGE_WIDTHS.size(), PRESENT.size());
        assertEquals(22, REDUCED.size());
    }

    @Test
    void controlledOracleWithTailwindPresentMatchesTheLegacyTreeInEveryConfigurationCaseAndWidth() {
        assertEquals(List.of(), lines(PRESENT));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheLegacyTreeGivenTheSameSurroundings() {
        assertEquals(List.of(), lines(REMOVED_SCOPED));
    }

    @Test
    void controlledOracleWithTailwindRemovedMatchesTheFullLegacyRendering() {
        assertEquals(List.of(), lines(REMOVED_FULL));
    }

    @Test
    void theScopedLegacyStylesheetRendersTheLegacyTreeExactlyAsTheFullOneDoes() {
        assertEquals(List.of(), lines(SCOPED_FIDELITY));
    }

    @Test
    void everyNegativeControlIsDetectedInTheProperty() {
        for (Control control : CONTROL_LIST) {
            List<ParityProbe.Finding> findings = CONTROLS.get(control.name());
            assertFalse(findings.isEmpty(), "the control was NOT detected: " + control.name());
            assertTrue(findings.stream().anyMatch(f -> f.property().equals(control.expectedProperty())),
                    control.name() + " should be detected in " + control.expectedProperty() + " but was detected in "
                            + findings.stream().map(ParityProbe.Finding::property).collect(Collectors.toCollection(TreeSet::new)));
        }
    }

    @Test
    void underReducedMotionNeitherTheChevronNorThePanelTransitionsInEitherRenderingAndTheyAgree() {
        assertEquals(List.of(), lines(REDUCED));
        for (String phase : List.of("light/none|L|440", "light/none|P|440")) {
            for (String kind : List.of("chevron", "panel")) {
                assertEquals("none", one(reduced, phase, kind, "open", "src", "transition-property"), phase + " " + kind + " must not transition under reduced motion");
            }
        }
        // The legacy motion-reduce:transition-none sets the PROPERTY to none and leaves the duration and easing, and so does the migrated rule.
        assertEquals("0.15s", one(reduced, "light/none|P|440", "chevron", "open", "src", "transition-duration"));
        assertEquals("cubic-bezier(0, 0, 0.2, 1)", one(reduced, "light/none|P|440", "panel", "open", "src", "transition-timing-function"));
        // And the same pages DO transition without it, so the reduced run is not vacuous.
        assertNotEquals("none", one("light/none|P|440", "chevron", "open", "src", "transition-property"));
        assertNotEquals("none", one("light/none|L|440", "panel", "open", "src", "transition-property"));
    }

    @Test
    void aRowIsTheMeasuredThirtyTwoPixelRowAndTheTreeStacksTopLevelItemsWithATwoPixelGap() {
        for (String phase : RENDERINGS) {
            assertEquals("flex", one(phase, "row", "flat", "one", "display"), phase);
            assertEquals("8px", one(phase, "row", "flat", "one", "padding-left"), phase);
            assertEquals("6px", one(phase, "row", "flat", "one", "padding-top"), phase);
            assertEquals("4px", one(phase, "row", "flat", "one", "column-gap"), phase);
            assertEquals("6px", one(phase, "row", "flat", "one", "border-top-left-radius"), phase);
            assertEquals("14px", one(phase, "row", "flat", "one", "font-size"), phase);
            assertEquals("20px", one(phase, "row", "flat", "one", "line-height"), phase);
            assertEquals("32", one(phase, "row", "flat", "one", "@h"), phase + ": 6 + 20 + 6");
            assertEquals("flex", one(phase, "tree", "flat", null, "display"), phase);
            assertEquals("column", one(phase, "tree", "flat", null, "flex-direction"), phase);
            assertEquals("2px", one(phase, "tree", "flat", null, "row-gap"), phase);
            assertEquals("100", one(phase, "tree", "flat", null, "@h"), phase + ": three 32px rows and two 2px gaps");
            assertEquals("none", one(phase, "tree", "flat", null, "list-style-type"), phase);
            assertEquals("0px", one(phase, "tree", "flat", null, "padding-left"), phase);
            assertEquals("0px", one(phase, "tree", "flat", null, "margin-top"), phase);
        }
    }

    @Test
    void theToggleAndTheSpacerAreTwentyPixelBoxesAndEveryIconIsSixteenPixelsWithoutTailwindsSizeClass() {
        for (String phase : RENDERINGS) {
            assertEquals("20", one(phase, "toggle", "closed", "src", "@w"), phase);
            assertEquals("20", one(phase, "toggle", "closed", "src", "@h"), phase);
            assertEquals("20", one(phase, "spacer", "flat", "one", "@w"), phase);
            assertEquals("20", one(phase, "spacer", "flat", "one", "@h"), phase);
            assertEquals("0", one(phase, "toggle", "closed", "src", "flex-shrink"), phase);
            assertEquals("pointer", one(phase, "toggle", "closed", "src", "cursor"), phase);
            assertEquals("button", one(phase, "toggle", "closed", "src", "appearance"), phase);
            assertEquals("0px", one(phase, "toggle", "closed", "src", "border-top-width"), phase);
            assertEquals("rgba(0, 0, 0, 0)", one(phase, "toggle", "closed", "src", "background-color"), phase);
            assertEquals("0px", one(phase, "toggle", "closed", "src", "padding-top"), phase);
            assertEquals("4px", one(phase, "toggle", "closed", "src", "border-top-left-radius"), phase);
            assertEquals("14px", one(phase, "toggle", "closed", "src", "font-size"), phase + ": the button inherits its font");
            assertEquals("16", one(phase, "chevron-svg", "closed", "src", "@w"), phase);
            assertEquals("16", one(phase, "chevron-svg", "closed", "src", "@h"), phase);
            assertEquals("16", one(phase, "icon-svg", "closed", "src", "@w"), phase);
            assertEquals("16", one(phase, "icon-svg", "closed", "src", "@h"), phase);
            assertEquals("0", one(phase, "icon", "closed", "src", "flex-shrink"), phase);
        }
    }

    @Test
    void eachLevelIsIndentedTwentyPixelsAndNestedItemsSitFlushWithNoGap() {
        for (String phase : RENDERINGS) {
            double a = number(one(phase, "row", "deep", "a", "@w"));
            double b = number(one(phase, "row", "deep", "b", "@w"));
            double c = number(one(phase, "row", "deep", "c", "@w"));
            double d = number(one(phase, "row", "deep", "d", "@w"));
            assertEquals(20.0, a - b, 0.01, phase + ": level 2 is 20px in");
            assertEquals(20.0, b - c, 0.01, phase + ": level 3 is 20px in");
            assertEquals(20.0, c - d, 0.01, phase + ": level 4 is 20px in");
            assertEquals("20px", one(phase, "group", "deep", "a", "padding-left"), phase);
            assertEquals("none", one(phase, "group", "deep", "a", "list-style-type"), phase);
            assertEquals("0px", one(phase, "group", "deep", "a", "margin-top"), phase);
            // An open panel is as tall as its children laid flush: Beta (32 + its panel) + Zeta (32) under Alpha.
            double beta = number(one(phase, "item", "deep", "b", "@h"));
            double zeta = number(one(phase, "item", "deep", "f", "@h"));
            assertEquals(beta + zeta, number(one(phase, "panel", "deep", "a", "@h")), 0.01, phase + ": no gap between nested siblings");
            assertEquals("list-item", one(phase, "item", "deep", "a", "display"), phase);
            assertEquals("none", one(phase, "item", "deep", "a", "list-style-type"), phase);
        }
    }

    @Test
    void aChevronTurnsOnlyForItsOwnExpandedItemEvenWhenAnAncestorIsExpandedOrCollapsed() {
        for (String phase : RENDERINGS) {
            assertEquals("90deg", one(phase, "chevron", "leaka", "a", "rotate"), phase + ": the expanded parent turns");
            assertEquals("none", one(phase, "chevron", "leaka", "b", "rotate"), phase + ": its collapsed child does NOT turn with it");
            assertEquals("none", one(phase, "chevron", "leakb", "a", "rotate"), phase + ": a collapsed parent does not turn");
            assertEquals("90deg", one(phase, "chevron", "leakb", "b", "rotate"), phase + ": its expanded child turns although the parent is collapsed");
            assertEquals("90deg", one(phase, "chevron", "leakb", "c", "rotate"), phase + ": and so does the grandchild");
            assertEquals("none", one(phase, "chevron", "allclosed", "a", "rotate"), phase);
            assertEquals("none", one(phase, "chevron", "allclosed", "b", "rotate"), phase);
            assertEquals("90deg", one(phase, "chevron", "deep", "c", "rotate"), phase);
        }
    }

    @Test
    void anOpenPanelIsAsTallAsItsContentAndACollapsedPanelIsAZeroRowEvenWithAnExpandedDescendant() {
        for (String phase : RENDERINGS) {
            assertEquals("grid", one(phase, "panel", "open", "src", "display"), phase);
            assertTrue(number(one(phase, "panel", "open", "src", "@h")) > 60, phase + ": an expanded panel is as tall as its children");
            assertEquals("0", one(phase, "panel", "closed", "src", "@h"), phase + ": a collapsed panel has no height");
            assertEquals("0", one(phase, "panel", "open", "pages", "@h"), phase + ": a collapsed child inside an open parent");
            assertEquals("0", one(phase, "panel", "leakb", "a", "@h"), phase + ": a collapsed parent hides its expanded child");
            assertTrue(number(one(phase, "panel", "leakb", "b", "@h")) > 0, phase + ": the expanded child keeps its own open row inside the collapsed parent");
            assertEquals("hidden", one(phase, "clip", "closed", "src", "overflow-y"), phase);
            assertEquals("0px", one(phase, "clip", "closed", "src", "min-height"), phase);
        }
    }

    @Test
    void theTransitionsAreOneHundredFiftyMillisecondsEaseOutOnTheExactPropertiesInBothRenderings() {
        for (String phase : RENDERINGS) {
            assertEquals("transform, translate, scale, rotate", one(phase, "chevron", "closed", "src", "transition-property"), phase);
            assertEquals("0.15s", one(phase, "chevron", "closed", "src", "transition-duration"), phase);
            assertEquals("cubic-bezier(0, 0, 0.2, 1)", one(phase, "chevron", "closed", "src", "transition-timing-function"), phase);
            assertEquals("grid-template-rows", one(phase, "panel", "closed", "src", "transition-property"), phase);
            assertEquals("0.15s", one(phase, "panel", "closed", "src", "transition-duration"), phase);
            assertEquals("cubic-bezier(0, 0, 0.2, 1)", one(phase, "panel", "closed", "src", "transition-timing-function"), phase);
        }
    }

    @Test
    void aNarrowColumnTruncatesLongLabelsWithAnEllipsisAndKeepsEveryRowOneLineHigh() {
        for (String phase : RENDERINGS) {
            assertEquals("220", one(phase, "tree", "narrow", null, "@w"), phase);
            for (String node : List.of("a", "b", "c", "d")) {
                assertEquals("32", one(phase, "row", "narrow", node, "@h"), phase + ": " + node + " does not wrap");
            }
            assertEquals("ellipsis", one(phase, "label", "narrow", "a", "text-overflow"), phase);
            assertEquals("nowrap", one(phase, "label", "narrow", "a", "white-space"), phase);
            assertEquals("hidden", one(phase, "label", "narrow", "a", "overflow-x"), phase);
            assertEquals("0px", one(phase, "label", "narrow", "a", "min-width"), phase);
            assertEquals("1", one(phase, "label", "narrow", "a", "flex-grow"), phase);
            assertEquals("16", one(phase, "chevron-svg", "narrow", "a", "@w"), phase + ": the chevron does not shrink");
            assertEquals("20", one(phase, "toggle", "narrow", "a", "@w"), phase + ": and neither does its button");
        }
    }

    @Test
    void aLinkLabelIsAnUnderlinedFreeInheritingAnchorAndAPlainLabelIsASpan() {
        for (String phase : RENDERINGS) {
            assertEquals("none", one(phase, "label", "links", "e", "text-decoration-line"), phase);
            assertEquals("pointer", one(phase, "label", "links", "e", "cursor"), phase);
            assertEquals("4px", one(phase, "label", "links", "e", "border-top-left-radius"), phase);
            assertEquals("500", one(phase, "label", "links", "e", "font-weight"), phase);
            assertEquals("500", one(phase, "label", "flat", "one", "font-weight"), phase);
            assertEquals(one(phase, "label", "links", "e", "color"), one(phase, "label", "flat", "one", "color"), phase + ": a link and a span have one colour");
            assertNotEquals(one(phase, "row", "flat", "one", "color"), one(phase, "label", "flat", "one", "color"), phase + ": the label has its own colour, the row has another");
        }
    }

    @Test
    void darkModeUsesTailwindsDarkColoursInEveryRendering() {
        for (String render : List.of("L", "P", "R")) {
            String light = "light/none|" + render + "|440";
            String dark = "dark/none|" + render + "|440";
            for (String[] part : new String[][] {{"row", "closed", "src", "color"}, {"toggle", "closed", "src", "color"}, {"icon", "closed", "src", "color"}, {"label", "flat", "one", "color"}}) {
                assertNotEquals(one(light, part[0], part[1], part[2], part[3]), one(dark, part[0], part[1], part[2], part[3]), render + " " + part[0] + " " + part[3]);
            }
        }
    }

    @Test
    void theCasesAreCompleteAndEveryPartIsMeasured() {
        List<ParityProbe.Row> rows = RETAINED.get("light/none").phase("light/none|L|440");
        Map<String, Integer> byKind = new TreeMap<>();
        rows.forEach(r -> byKind.merge(r.kind(), 1, Integer::sum));
        for (String kind : List.of("tree", "item", "row", "toggle", "chevron", "chevron-svg", "spacer", "icon", "icon-svg", "label", "panel", "clip", "group")) {
            int written = count(migratedForCounting, "data-parity=\"" + kind + "\"");
            assertTrue(written > 0, kind);
            assertEquals(written, byKind.get(kind), kind + " rows");
        }
        assertEquals(GROUPS.size(), byKind.get("tree"));
        assertEquals(byKind.get("toggle") + byKind.get("spacer"), byKind.get("item"), "every item has a toggle or a spacer");
        assertEquals(byKind.get("toggle"), byKind.get("panel"), "one panel per parent");
        assertEquals(byKind.get("item"), byKind.get("label"), "one label per item");
        for (String key : ROWS.keySet().stream().filter(k -> k.startsWith("light/none|L|")).toList()) {
            assertEquals(ROWS.get("light/none|L|440"), ROWS.get(key), "every width measures the same elements: " + key);
        }
    }

    @Test
    void theOracleIsNotVacuousEveryLegacyClassIsCompiledInTheCumulativeAndScopedStylesheets() throws IOException {
        Path dir = Path.of(System.getProperty("ashlar.cascade.dir"));
        String oracle = Files.readString(dir.resolve("tree-oracle.css"), StandardCharsets.UTF_8);
        String scoped = Files.readString(dir.resolve("tree-legacy-scoped.css"), StandardCharsets.UTF_8);
        List<String> tokens = new ArrayList<>(treeLegacy.allTokens());
        // ParityOracle recognises a class selector followed by a block, a pseudo-class or a comma; Tailwind writes the variants and arbitrary values differently, so those
        // are looked up by their escaped selector below.
        List<String> plain = tokens.stream().filter(t -> !t.contains(":") && !t.contains("[") && !t.contains("/") && !t.contains(".")).toList();
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, plain), "the oracle does not compile these");
        assertEquals(List.of(), ParityOracle.uncompiled(scoped, plain), "the scoped stylesheet does not compile these");
        for (String escaped : List.of("gap-0\\.5", "py-1\\.5", "hover\\:bg-slate-100", "hover\\:text-slate-600", "hover\\:underline", "focus\\:outline-none", "motion-reduce\\:transition-none",
                "transition-\\[grid-template-rows\\]", "dark\\:text-slate-300", "dark\\:text-slate-500", "dark\\:text-slate-100", "dark\\:hover\\:bg-slate-800", "dark\\:hover\\:text-slate-300",
                "li\\:focus-visible\\>\\&", "bg-blue-50", "ring-inset", "ring-blue-500", "rotate-90", "duration-150")) {
            assertTrue(oracle.contains(escaped) && scoped.contains(escaped), "not compiled: " + escaped);
        }
        List<String> earlier = new ArrayList<>();
        earlier.addAll(new AccordionLegacyMarkup().allTokens().stream().filter(t -> !t.contains(".") && !t.startsWith("divide-")).toList());
        earlier.addAll(new TabsLegacyMarkup().allTokens().stream().filter(t -> !t.contains(".")).toList());
        earlier.addAll(new NavigationLegacyMarkup().allTokens().stream().filter(t -> !t.contains("[")).toList());
        earlier.addAll(new CardLegacyMarkup().allTokens());
        earlier.addAll(new IconLegacyMarkup().allTokens());
        assertEquals(List.of(), ParityOracle.uncompiled(oracle, earlier.stream().filter(t -> !t.contains(":") && !t.contains("/")).toList()), "the oracle lost earlier legacy classes");
        assertFalse(scoped.contains("html,:host"), "the scoped legacy stylesheet must not carry Tailwind's page-wide base");
    }

    @Test
    void normalizationsStayWithinTheApprovedRules() {
        assertEquals(Map.of(), APPLIED_PRESENT, "Tailwind present needs no normalization: nothing here is a pill or a Tailwind radius value");
        assertEquals(Map.of(), APPLIED_SCOPED, "the scoped legacy rendering must match the full one without any normalization");
        assertEquals(Map.of(), APPLIED_REDUCED, "reduced motion needs no normalization");
        assertTrue(Set.of("H2").containsAll(APPLIED_REMOVED.keySet()), "unexpected normalization rule used: " + APPLIED_REMOVED);
    }

    // ---- what the real pages serve ----

    private static final Pattern TAILWIND_ON_A_HOOK = Pattern.compile("class=\"[^\"]*(?:flex-col|rotate-90|min-h-0|justify-center|rounded-md|truncate)[^\"]*\"[^>]*data-tree");

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            n++;
        }
        return n;
    }

    @Test
    void everyTreeTheRealPagesServeKeepsItsNativeStructureRolesAndHooksAndNoInlineStyleOrStateClass() throws Exception {
        ParitySession session = ParitySession.open("tree-parity");
        HttpClient client = HttpClient.newHttpClient();
        List<String> paths = new ArrayList<>(List.of("/ashlar/", "/ashlar/components"));
        for (ComponentPage page : ComponentCatalog.PAGES) {
            paths.add("/ashlar/components/" + page.slug());
        }
        Map<String, Integer> totals = new TreeMap<>();
        List<String> problems = new ArrayList<>();
        try (RunningShowcase app = RunningShowcase.start(session.bootJar())) {
            for (String path : paths) {
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(200, response.statusCode(), path);
                String body = response.body();
                for (String role : TreeLegacyMarkup.MIGRATED.keySet()) {
                    totals.merge(role, TreeLegacyMarkup.count(body, role), Integer::sum);
                }
                treeLegacy.rewrite(body);
                int roots = TreeLegacyMarkup.count(body, "tree");
                int items = TreeLegacyMarkup.count(body, "tree-item");
                int rows = TreeLegacyMarkup.count(body, "tree-row");
                int toggles = TreeLegacyMarkup.count(body, "tree-toggle");
                int spacers = TreeLegacyMarkup.count(body, "tree-spacer");
                int panels = TreeLegacyMarkup.count(body, "tree-panel");
                int labels = TreeLegacyMarkup.count(body, "tree-label");
                // Counted as tags: the documentation also mentions these attributes in prose.
                long rootTags = Pattern.compile("<ul role=\"tree\" data-tree class=\"ash-tree\">").matcher(body).results().count();
                long itemTags = Pattern.compile("<li\\s+role=\"treeitem\"[^>]*\\bdata-tree-item\\s+class=\"ash-tree__item\"\\s*>").matcher(body).results().count();
                long toggleTags = Pattern.compile("<button\\b[^>]*\\bdata-tree-toggle\\s+class=\"ash-tree__toggle\"").matcher(body).results().count();
                long panelTags = Pattern.compile("<div\\b[^>]*\\bdata-tree-children\\b[^>]*class=\"ash-tree__panel\"").matcher(body).results().count();
                long chevronTags = Pattern.compile("<span data-tree-chevron class=\"ash-tree__chevron\">").matcher(body).results().count();
                if (rootTags != roots || itemTags != items || toggleTags != toggles || panelTags != panels || chevronTags != toggles || rows != items || toggles + spacers != items || labels != items || panels != toggles) {
                    problems.add(path + ": hooks/classes differ: roots " + rootTags + "/" + roots + ", items " + itemTags + "/" + items + ", rows " + rows + ", toggles " + toggleTags + "/" + toggles
                            + ", spacers " + spacers + ", panels " + panelTags + "/" + panels + ", chevrons " + chevronTags + ", labels " + labels);
                }
                if (Pattern.compile("<div\\b[^>]*\\bdata-tree-children\\b[^>]*\\bstyle=").matcher(body).find()) {
                    problems.add(path + ": a panel still renders an inline style");
                }
                if (Pattern.compile("<span data-tree-chevron[^>]*rotate-90").matcher(body).find()) {
                    problems.add(path + ": a chevron still renders rotate-90");
                }
                long expanded = Pattern.compile("<li\\b[^>]*>").matcher(body).results().map(r -> r.group()).filter(t -> t.contains("data-tree-item") && t.contains("aria-expanded=\"true\"")).count();
                long open = Pattern.compile("<div\\b(?![^>]*\\binert\\b)[^>]*\\bdata-tree-children\\b").matcher(body).results().count();
                if (expanded != open) {
                    problems.add(path + ": aria-expanded=true (" + expanded + ") and not-inert panels (" + open + ") disagree");
                }
                if (TAILWIND_ON_A_HOOK.matcher(body).find()) {
                    problems.add(path + ": a tree still renders its Tailwind classes");
                }
            }
        }
        assertEquals(List.of(), problems);
        for (String role : TreeLegacyMarkup.MIGRATED.keySet()) {
            assertTrue(totals.getOrDefault(role, 0) > 0, "no " + role + " rendered on any page: " + totals);
        }
        assertEquals(totals.get("tree-item"), totals.get("tree-row"));
        assertEquals(totals.get("tree-toggle"), totals.get("tree-panel"));
        assertEquals(totals.get("tree-panel"), totals.get("tree-clip"));
        assertEquals(totals.get("tree-panel"), totals.get("tree-group"));
    }
}
