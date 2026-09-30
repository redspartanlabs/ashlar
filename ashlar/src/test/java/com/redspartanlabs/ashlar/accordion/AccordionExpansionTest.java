package com.redspartanlabs.ashlar.accordion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccordionExpansionTest {

    // Backed by a static ThreadLocal "claimed" flag - reset() after every
    // test so one test's claim can never leak into the next on a reused
    // thread, the same discipline accordion.jte itself follows before
    // rendering its own items.
    @AfterEach
    void resetContext() {
        AccordionExpansion.reset();
    }

    @Test
    void resolveFalseNeverClaimsAndAlwaysReturnsFalse() {
        AccordionExpansion.reset();

        assertFalse(AccordionExpansion.resolve(false));
        assertFalse(AccordionExpansion.resolve(false));
    }

    @Test
    void firstItemRequestingExpandedWinsTheSlot() {
        AccordionExpansion.reset();

        assertTrue(AccordionExpansion.resolve(true));
    }

    @Test
    void secondItemRequestingExpandedLosesOnceTheSlotIsClaimed() {
        AccordionExpansion.reset();
        AccordionExpansion.resolve(true);

        assertFalse(AccordionExpansion.resolve(true));
    }

    @Test
    void anItemThatDidNotAskNeverClaimsTheSlotForALaterOne() {
        AccordionExpansion.reset();

        assertFalse(AccordionExpansion.resolve(false));
        // The slot is still open - a later item that does ask still wins.
        assertTrue(AccordionExpansion.resolve(true));
    }

    @Test
    void resetClearsAnEarlierClaimSoTheNextAccordionStartsFresh() {
        AccordionExpansion.reset();
        AccordionExpansion.resolve(true);

        AccordionExpansion.reset();

        assertTrue(AccordionExpansion.resolve(true));
    }
}
