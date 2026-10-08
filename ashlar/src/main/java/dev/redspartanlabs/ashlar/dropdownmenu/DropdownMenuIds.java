package dev.redspartanlabs.ashlar.dropdownmenu;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates the one id a Dropdown Menu instance actually needs - shared
 * between the trigger's {@code aria-controls} and the menu's own {@code id} -
 * without asking the caller to invent and pass one. Unlike Accordion, Tabs,
 * or Modal (each of which composes several independently-called templates
 * that must agree on a shared id the caller supplies), a Dropdown Menu's
 * trigger and menu are both rendered by this single component call, so there
 * is nothing for a caller-supplied id to coordinate across.
 *
 * <p>A process-wide {@link AtomicLong} rather than a random/UUID value: this
 * only has to be unique within one rendered page, and a monotonic counter is
 * simpler and just as sufficient - collisions are impossible for the same
 * reason two calls to {@code i++} can't return the same value.
 */
public final class DropdownMenuIds {

    private static final AtomicLong COUNTER = new AtomicLong();

    private DropdownMenuIds() {
    }

    public static String next() {
        return "dropdown-menu-" + COUNTER.incrementAndGet();
    }
}
