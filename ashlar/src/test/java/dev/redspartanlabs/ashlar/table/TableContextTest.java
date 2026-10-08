package dev.redspartanlabs.ashlar.table;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableContextTest {

    // TableContext is backed by a static ThreadLocal, and JUnit may reuse the
    // same thread across test methods in this class - exit() after every test
    // so one test's state can never leak into the next, the same discipline
    // table.jte itself follows around every render.
    @AfterEach
    void resetContext() {
        TableContext.exit();
    }

    @Test
    void isSelectableDefaultsToFalse() {
        assertFalse(TableContext.isSelectable());
    }

    @Test
    void enterSelectableMakesIsSelectableTrue() {
        TableContext.enterSelectable();

        assertTrue(TableContext.isSelectable());
    }

    @Test
    void exitRestoresTheFalseDefault() {
        TableContext.enterSelectable();
        TableContext.exit();

        assertFalse(TableContext.isSelectable());
    }
}
