package com.redspartanlabs.ashlar.tree;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeContextTest {

    // Backed by a static ThreadLocal depth counter - reset() after every test
    // so one test's depth can never leak into the next on a reused thread.
    @AfterEach
    void resetContext() {
        TreeContext.reset();
    }

    @Test
    void currentLevelAfterResetIsOne() {
        TreeContext.reset();

        assertEquals(1, TreeContext.currentLevel());
    }

    @Test
    void enterChildrenIncreasesTheLevel() {
        TreeContext.reset();
        TreeContext.enterChildren();

        assertEquals(2, TreeContext.currentLevel());
    }

    @Test
    void nestedEnterChildrenKeepsIncreasing() {
        TreeContext.reset();
        TreeContext.enterChildren();
        TreeContext.enterChildren();

        assertEquals(3, TreeContext.currentLevel());
    }

    @Test
    void exitChildrenRestoresTheParentLevel() {
        TreeContext.reset();
        TreeContext.enterChildren();
        TreeContext.enterChildren();
        TreeContext.exitChildren();

        assertEquals(2, TreeContext.currentLevel());

        TreeContext.exitChildren();

        assertEquals(1, TreeContext.currentLevel());
    }

    @Test
    void resetReturnsToLevelOneRegardlessOfPriorDepth() {
        TreeContext.enterChildren();
        TreeContext.enterChildren();
        TreeContext.enterChildren();

        TreeContext.reset();

        assertEquals(1, TreeContext.currentLevel());
    }

    @Test
    void siblingsAtTheSameDepthSeeTheSameLevel() {
        TreeContext.reset();
        TreeContext.enterChildren();

        assertEquals(2, TreeContext.currentLevel());

        // A first child's own children come and go...
        TreeContext.enterChildren();
        TreeContext.exitChildren();

        // ...and its next sibling, at the same depth, sees the same level.
        assertEquals(2, TreeContext.currentLevel());
    }
}
