package dev.redspartanlabs.ashlar.dropdownmenu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The id a Dropdown Menu's trigger (aria-controls) and menu (id) share: its format, and that every call gives a new one, from any thread. */
class DropdownMenuIdsTest {

    @Test
    void anIdIsDropdownMenuFollowedByANumber() {
        assertTrue(DropdownMenuIds.next().matches("dropdown-menu-[1-9][0-9]*"));
    }

    @Test
    void successiveIdsIncrease() {
        long a = Long.parseLong(DropdownMenuIds.next().substring("dropdown-menu-".length()));
        long b = Long.parseLong(DropdownMenuIds.next().substring("dropdown-menu-".length()));
        assertTrue(b > a, a + " then " + b);
    }

    @Test
    void idsAreUniqueAcrossThreads() {
        Set<String> seen = ConcurrentHashMap.newKeySet();
        List<String> all = IntStream.range(0, 2000).parallel().mapToObj(i -> DropdownMenuIds.next()).toList();
        seen.addAll(all);
        assertEquals(all.size(), seen.size());
    }
}
