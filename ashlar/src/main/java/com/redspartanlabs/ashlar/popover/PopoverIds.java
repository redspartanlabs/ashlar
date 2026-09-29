package com.redspartanlabs.ashlar.popover;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates the id a Popover instance needs for its surface - referenced by
 * the trigger's {@code aria-controls} - the same reasoning as
 * {@code TooltipIds} and {@code DropdownMenuIds}: this is internal wiring
 * between two elements rendered by the same component call, so there is
 * nothing for a caller-supplied id to coordinate with.
 */
public final class PopoverIds {

    private static final AtomicLong COUNTER = new AtomicLong();

    private PopoverIds() {
    }

    public static String next() {
        return "popover-" + COUNTER.incrementAndGet();
    }
}
