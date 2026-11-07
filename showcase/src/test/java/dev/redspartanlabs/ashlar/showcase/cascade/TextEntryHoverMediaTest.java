package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hover on a touch device. Tailwind's {@code hover:} variant is wrapped in {@code @media (hover: hover)}, so a tap on a phone never leaves a field's border or the clear button
 * stuck in its hover style. The migrated rules must sit under the same condition, and EVERY hover rule the Text Entry stylesheets contain must (a hover rule that escaped the media
 * query would look right on a desktop and stick after a tap). The parity suites state the device as one that can hover, so they cannot see that; this test reads the rules and the
 * condition from the browser's own style sheets, and shows the condition is false on a device that cannot hover.
 */
class TextEntryHoverMediaTest {

    private static final String ORACLE = ParityFrames.generated("text-entry-oracle.css");
    private static final String CAN_HOVER = "--blink-settings=primaryHoverType=2,availableHoverTypes=2";
    private static final String CANNOT_HOVER = "--blink-settings=primaryHoverType=1,availableHoverTypes=1";

    /** Every output line is {@code kind<TAB>selector<TAB>conditions}, then {@code matches<TAB>true|false} for the hover query. */
    private static final String SCRIPT = "(function(){var out=[];"
            + "function walk(rules,conds,sheet){[].forEach.call(rules,function(r){"
            + "if(r.selectorText!==undefined){"
            + "if((r.selectorText.indexOf('ash-field')>=0||r.selectorText.indexOf('ash-search-input')>=0||r.selectorText.indexOf('ash-textarea')>=0)&&r.selectorText.indexOf(':hover')>=0){out.push(['migrated',r.selectorText,conds.join(' && ')].join('\\t'))}"
            + "['hover\\\\:border-slate-400:hover','read-only\\\\:hover\\\\:border-slate-300','user-invalid\\\\:hover\\\\:border-red-600','aria-invalid\\\\:hover\\\\:border-red-600','dark\\\\:hover\\\\:border-slate-500','dark\\\\:read-only\\\\:hover\\\\:border-slate-600','hover\\\\:text-slate-600:hover','dark\\\\:hover\\\\:text-slate-300'].forEach(function(w){"
            + "if(sheet==='oracle'&&r.selectorText.indexOf(w)>=0){out.push(['legacy',r.selectorText,conds.join(' && ')].join('\\t'))}})}"
            + "else if(r.cssRules){walk(r.cssRules,r.conditionText!==undefined?conds.concat([r.conditionText]):conds,sheet)}})}"
            + "[].forEach.call(document.styleSheets,function(s){try{walk(s.cssRules,[],(s.href||'').indexOf('text-entry-oracle')>=0?'oracle':'other')}catch(e){out.push(['error',String(e),''].join('\\t'))}});"
            + "out.push(['matches',String(matchMedia('(hover: hover)').matches),''].join('\\t'));"
            + "var pre=document.createElement('pre');pre.id='ash-text-entry-hover-media';pre.textContent=out.join('\\n');document.body.appendChild(pre)})();";

    private static List<String> canHover;
    private static List<String> touchOnly;

    @BeforeAll
    static void measure() throws Exception {
        ParitySession session = ParitySession.open("text-entry-hover-media");
        String page = "<!doctype html><html><head><link rel=\"stylesheet\" href=\"" + ParityFrames.ASHLAR + "\"><link rel=\"stylesheet\" href=\"" + ORACLE
                + "\"></head><body><p>hover media</p><script>window.addEventListener('load',function(){" + SCRIPT + "});</script></body></html>";
        try (CascadeFixtureServer server = new CascadeFixtureServer(session.generatedDir())) {
            String url = server.registerRaw(page);
            canHover = ParityProbe.lines(session.chrome().dumpDom(url, List.of(CAN_HOVER)), "ash-text-entry-hover-media");
            touchOnly = ParityProbe.lines(session.chrome().dumpDom(url, List.of(CANNOT_HOVER)), "ash-text-entry-hover-media");
        }
    }

    private static List<String[]> rows(List<String> lines, String kind) {
        return lines.stream().map(l -> l.split("\t", -1)).filter(f -> f[0].equals(kind)).toList();
    }

    @Test
    void everyMigratedHoverRuleIsFoundAndEveryOneSitsUnderTheHoverCapabilityCondition() {
        List<String[]> migrated = rows(canHover, "migrated");
        assertEquals(8, migrated.size(), "the six control hover rules (resting, invalid, read-only, aria-invalid, and the two dark ones) and the two clear-button ones: " + canHover);
        for (String[] rule : migrated) {
            assertEquals("(hover: hover)", rule[2], rule[1] + " must be under the hover capability condition");
        }
        assertTrue(rows(canHover, "error").isEmpty(), "every stylesheet was readable: " + canHover);
    }

    @Test
    void theLegacyTailwindHoverRulesSitUnderTheSameCondition() {
        List<String[]> legacy = rows(canHover, "legacy");
        assertTrue(legacy.size() >= 8, "the legacy control and clear-button hover rules (light and dark): " + canHover);
        for (String[] rule : legacy) {
            assertEquals("(hover: hover)", rule[2], "the legacy " + rule[1] + " is under the same condition as the migrated rules");
        }
    }

    @Test
    void theConditionMatchesADeviceThatCanHoverAndDoesNotMatchATouchScreen() {
        assertEquals("true", rows(canHover, "matches").get(0)[1], "a device that is told it can hover");
        assertEquals("false", rows(touchOnly, "matches").get(0)[1], "a touch screen without hover: neither the legacy rule nor the migrated one applies");
        assertEquals(rows(canHover, "migrated").stream().map(f -> f[1] + "|" + f[2]).toList(), rows(touchOnly, "migrated").stream().map(f -> f[1] + "|" + f[2]).toList());
        assertFalse(rows(touchOnly, "legacy").isEmpty());
    }
}
