package com.redspartanlabs.ashlar.datatable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortTest {

    @Test
    void textTokenIsLowercaseName() {
        assertEquals("text", Sort.TEXT.token());
    }

    @Test
    void numberTokenIsLowercaseName() {
        assertEquals("number", Sort.NUMBER.token());
    }

    @Test
    void dateTokenIsLowercaseName() {
        assertEquals("date", Sort.DATE.token());
    }
}
