package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Breadcrumb Item link's hover styling is restricted to devices that can hover, exactly as Tailwind's {@code hover:} variant was.
 *
 * <p>A hover cannot be held in a measurement, so this reads the rule where the browser keeps it: in the style sheet object model of the
 * real, served stylesheets. It finds the {@code :hover} rule of the migrated link in Ashlar's generated stylesheet and the legacy
 * {@code hover:underline} and {@code hover:text-slate-700} rules in the Tailwind oracle, and records the media condition each sits under.
 * The two must be the same condition, and that condition must really be the device's hover capability: Chrome is started twice, once as a
 * device that can hover and once as a touch screen that cannot (a primary pointer without hover), and the condition must match in the first
 * and not in the second. So on a touch screen neither the legacy rule nor the migrated one applies, and a tap leaves no stuck hover.
 *
 * <p>It reads the sheets only; it changes no harness code and makes no assumption about how a page is styled.
 */
class BreadcrumbHoverMediaTest {

    private static final String ORACLE = ParityFrames.generated("breadcrumbs-oracle.css");

    /** Every page tab separated line is {@code kind<TAB>selector<TAB>conditions}, then {@code matches<TAB>true|false} for the hover query. */
    private static final String SCRIPT = "(function(){var out=[];"
            + "function walk(rules,conds,wanted){[].forEach.call(rules,function(r){"
            + "if(r.selectorText!==undefined){wanted.forEach(function(w){if(r.selectorText.indexOf(w[1])>=0){out.push([w[0],r.selectorText,conds.join(' && ')].join('\\t'))}})}"
            + "else if(r.cssRules){walk(r.cssRules,r.conditionText!==undefined?conds.concat([r.conditionText]):conds,wanted)}})}"
            + "var wanted=[['migrated','.ash-breadcrumb-item__link:hover'],['legacy','hover\\\\:underline:hover'],['legacy','hover\\\\:text-slate-700:hover'],"
            + "['legacy','dark\\\\:hover\\\\:text-slate-200']];"
            + "[].forEach.call(document.styleSheets,function(s){try{walk(s.cssRules,[],wanted)}catch(e){out.push(['error',String(e),''].join('\\t'))}});"
            + "out.push(['matches',String(matchMedia('(hover: hover)').matches),''].join('\\t'));"
            + "var pre=document.createElement('pre');pre.id='ash-hover-media';pre.textContent=out.join('\\n');document.body.appendChild(pre)})();";

    private static List<String> canHover;
    private static List<String> touchOnly;

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("breadcrumb-hover-media");
        String page = "<!doctype html><html><head><link rel=\"stylesheet\" href=\"" + ParityFrames.ASHLAR + "\"><link rel=\"stylesheet\" href=\"" + ORACLE
                + "\"></head><body><p>hover media</p><script>window.addEventListener('load',function(){" + SCRIPT + "});</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String url = server.registerRaw(page);
            canHover = ParityProbe.lines(session.chrome().dumpDom(url, List.of()), "ash-hover-media");
            touchOnly = ParityProbe.lines(session.chrome().dumpDom(url, List.of("--blink-settings=primaryHoverType=1,availableHoverTypes=1")), "ash-hover-media");
        }
    }

    private static List<String[]> rows(List<String> lines, String kind) {
        return lines.stream().map(l -> l.split("\t", -1)).filter(f -> f[0].equals(kind)).toList();
    }

    @Test
    void theMigratedHoverRuleIsFoundOnceAndSitsUnderTheHoverCapabilityCondition() {
        List<String[]> migrated = rows(canHover, "migrated");
        assertEquals(1, migrated.size(), "the migrated :hover rule, once: " + canHover);
        assertEquals("(hover: hover)", migrated.get(0)[2]);
        assertTrue(rows(canHover, "error").isEmpty(), "every stylesheet was readable: " + canHover);
    }

    @Test
    void theLegacyTailwindHoverRulesSitUnderTheSameCondition() {
        List<String[]> legacy = rows(canHover, "legacy");
        assertEquals(3, legacy.size(), "the legacy link's three hover rules (hover underline, hover colour, dark hover colour): " + canHover);
        for (String[] rule : legacy) {
            assertEquals(rows(canHover, "migrated").get(0)[2], rule[2], "the migrated hover is under the same condition as the legacy " + rule[1]);
        }
    }

    @Test
    void theConditionMatchesADeviceThatCanHoverAndDoesNotMatchATouchScreen() {
        assertEquals("true", rows(canHover, "matches").get(0)[1], "a device that can hover");
        assertEquals("false", rows(touchOnly, "matches").get(0)[1], "a touch screen without hover: neither the legacy rule nor the migrated one applies");
        // The same rules are found and under the same condition on the touch screen: it is the condition, not the rules, that differs.
        assertEquals(rows(canHover, "migrated").get(0)[2], rows(touchOnly, "migrated").get(0)[2]);
        assertFalse(rows(touchOnly, "legacy").isEmpty());
    }
}
