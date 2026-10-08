package dev.redspartanlabs.ashlar.datatable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlignTest {

    @Test
    void startTokenIsLeft() {
        assertEquals("left", Align.START.token());
    }

    @Test
    void endTokenIsRight() {
        assertEquals("right", Align.END.token());
    }

    @Test
    void centerTokenIsCenter() {
        assertEquals("center", Align.CENTER.token());
    }
}
