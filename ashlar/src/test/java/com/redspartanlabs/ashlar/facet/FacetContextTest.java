package com.redspartanlabs.ashlar.facet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FacetContextTest {

    // Backed by a static ThreadLocal - exit() after every test so one test's
    // name can never leak into the next on a reused thread.
    @AfterEach
    void resetContext() {
        FacetContext.exit();
    }

    @Test
    void currentNameDefaultsToEmptyString() {
        assertEquals("", FacetContext.currentName());
    }

    @Test
    void enterMakesCurrentNameTheGivenName() {
        FacetContext.enter("status");

        assertEquals("status", FacetContext.currentName());
    }

    @Test
    void exitRestoresTheEmptyStringDefault() {
        FacetContext.enter("status");
        FacetContext.exit();

        assertEquals("", FacetContext.currentName());
    }

    @Test
    void enteringNullBehavesLikeNoFacetAtAll() {
        FacetContext.enter(null);

        assertEquals("", FacetContext.currentName());
    }

    @Test
    void enteringASecondFacetReplacesThePreviousName() {
        FacetContext.enter("status");
        FacetContext.enter("machine");

        assertEquals("machine", FacetContext.currentName());
    }
}
