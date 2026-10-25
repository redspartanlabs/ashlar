package dev.redspartanlabs.ashlar.showcase.cascade;

import java.util.ArrayList;
import java.util.List;

/**
 * The page configurations a parity suite measures in: a theme (light or dark) with no skin or one of the ten
 * showcase skins (22 in all, which is what the per-component suites measure). {@link #reduced()} is the four that
 * matter for a component with no colour of its own: both themes, with no skin and with Foundry (the only skin
 * whose rules reach an svg). A suite states which set it uses and why.
 */
final class ParityConfigs {

    static final List<String> SKINS = List.of("axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian");

    /** Stylesheet links for every skin, so a frame can switch skin with only a data attribute (as the real pages do). */
    static final List<String> SKIN_LINKS = SKINS.stream().map(s -> "/css/theme-" + s + ".css").toList();

    record Config(String id, String theme, String skin) {
    }

    private static final List<Config> ALL = all22();

    private ParityConfigs() {
    }

    private static List<Config> all22() {
        List<Config> all = new ArrayList<>();
        for (String theme : List.of("light", "dark")) {
            all.add(new Config(theme + "/none", theme, null));
            for (String skin : SKINS) {
                all.add(new Config(theme + "/" + skin, theme, skin));
            }
        }
        return List.copyOf(all);
    }

    static List<Config> reduced() {
        return List.of(get("light/none"), get("dark/none"), get("light/foundry"), get("dark/foundry"));
    }

    static Config get(String id) {
        return ALL.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("no configuration " + id));
    }
}
