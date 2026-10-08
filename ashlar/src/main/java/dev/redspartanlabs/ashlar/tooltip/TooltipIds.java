package dev.redspartanlabs.ashlar.tooltip;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates the one id a Tooltip instance needs for its bubble - referenced
 * by the trigger's {@code aria-describedby} once shown - the same reasoning
 * as {@code DropdownMenuIds}: this is internal wiring between two elements
 * rendered by the same component call, so there's nothing for a caller to
 * coordinate and no reason to make them invent and pass one.
 */
public final class TooltipIds {

    private static final AtomicLong COUNTER = new AtomicLong();

    private TooltipIds() {
    }

    public static String next() {
        return "tooltip-" + COUNTER.incrementAndGet();
    }
}
