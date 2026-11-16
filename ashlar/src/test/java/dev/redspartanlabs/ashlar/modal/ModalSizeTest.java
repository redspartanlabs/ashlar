package dev.redspartanlabs.ashlar.modal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModalSizeTest {

    @Test
    void fromAcceptsTheThreeSizesInAnyCaseAndFallsBackToMedium() {
        assertEquals(ModalSize.SMALL, ModalSize.from("Small"));
        assertEquals(ModalSize.LARGE, ModalSize.from(" large "));
        assertEquals(ModalSize.MEDIUM, ModalSize.from("Medium"));
        assertEquals(ModalSize.MEDIUM, ModalSize.from("huge"));
        assertEquals(ModalSize.MEDIUM, ModalSize.from(null));
    }

    @Test
    void maxWidthClassKeepsItsNameAndReturnsTheDialogsOwnSizeModifier() {
        assertEquals("ash-modal__dialog--sm", ModalSize.SMALL.maxWidthClass());
        assertEquals("ash-modal__dialog--md", ModalSize.MEDIUM.maxWidthClass());
        assertEquals("ash-modal__dialog--lg", ModalSize.LARGE.maxWidthClass());
    }
}
