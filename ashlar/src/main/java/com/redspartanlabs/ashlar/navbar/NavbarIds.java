package com.redspartanlabs.ashlar.navbar;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates the one id a Navbar instance needs internally - the mobile menu
 * Drawer it composes - without asking the caller to invent and pass one.
 * Same shape as {@code DropdownMenuIds}/{@code PopoverIds}/{@code TooltipIds}:
 * the drawer is rendered by this same component call and never referenced
 * from outside it, so there is nothing for a caller-supplied id to
 * coordinate against.
 *
 * <p>A process-wide {@link AtomicLong} rather than a random/UUID value: this
 * only has to be unique within one rendered page, and a monotonic counter is
 * simpler and just as sufficient - collisions are impossible for the same
 * reason two calls to {@code i++} can't return the same value.
 */
public final class NavbarIds {

    private static final AtomicLong COUNTER = new AtomicLong();

    private NavbarIds() {
    }

    public static String next() {
        return "navbar-menu-" + COUNTER.incrementAndGet();
    }
}
