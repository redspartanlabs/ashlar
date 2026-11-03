package dev.redspartanlabs.ashlar.button;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The public contract of {@link ButtonStyles}, which a consumer may call (File Upload does): the five public methods and their signatures are
 * unchanged, their output is Ashlar's own classes, and the two pressed methods keep their meaning (the resting classes, and the same classes
 * plus the modifier that draws the pressed look on an element with no {@code aria-pressed}).
 */
class ButtonStylesTest {

    @Test
    void thePublicSurfaceIsExactlyTheFiveMethodsWithTheirOriginalSignatures() {
        Set<String> signatures = Arrays.stream(ButtonStyles.class.getDeclaredMethods()).filter(m -> Modifier.isPublic(m.getModifiers())).map(ButtonStylesTest::signature)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "public static java.lang.String classes(ButtonVariant,ButtonSize,boolean,boolean,boolean)",
                "public static java.lang.String restingClasses(ButtonVariant,ButtonSize,boolean,boolean)",
                "public static java.lang.String pressedClasses(ButtonVariant,ButtonSize,boolean,boolean)",
                "public static java.lang.String iconSizeClasses(ButtonSize)",
                "public static java.lang.String normalizeButtonType(java.lang.String)"), signatures);
        assertTrue(Modifier.isFinal(ButtonStyles.class.getModifiers()));
    }

    private static String signature(Method m) {
        return Modifier.toString(m.getModifiers()) + " " + m.getReturnType().getName() + " " + m.getName() + "("
                + Arrays.stream(m.getParameterTypes()).map(t -> t.getName().startsWith("dev.redspartanlabs") ? t.getSimpleName() : t.getName()).collect(Collectors.joining(",")) + ")";
    }

    @Test
    void classesAreAshlarsOwnInAFixedOrderForEveryVariantAndSize() {
        for (ButtonVariant v : ButtonVariant.values()) {
            for (ButtonSize s : ButtonSize.values()) {
                String size = switch (s) {
                    case XSMALL -> "xs";
                    case SMALL -> "sm";
                    case MEDIUM -> "md";
                    case LARGE -> "lg";
                    case XLARGE -> "xl";
                };
                String name = v.name().toLowerCase();
                assertEquals("ash-btn ash-btn--" + name + " ash-btn--" + size, ButtonStyles.classes(v, s, false, false, false));
                assertEquals("ash-btn ash-btn--" + name + " ash-btn--" + size + " ash-btn--icon", ButtonStyles.classes(v, s, true, false, false));
                assertEquals("ash-btn ash-btn--" + name + " ash-btn--" + size + " ash-btn--block", ButtonStyles.classes(v, s, false, true, false));
                assertEquals("ash-btn ash-btn--" + name + " ash-btn--" + size + " ash-btn--icon ash-btn--block ash-btn--pressed", ButtonStyles.classes(v, s, true, true, true));
            }
        }
        assertEquals(8, ButtonVariant.values().length);
        assertEquals(5, ButtonSize.values().length);
    }

    @Test
    void noTailwindUtilityIsEmittedAnywhere() {
        for (ButtonVariant v : ButtonVariant.values()) {
            for (ButtonSize s : ButtonSize.values()) {
                for (String classes : List.of(ButtonStyles.classes(v, s, true, true, true), ButtonStyles.restingClasses(v, s, false, false), ButtonStyles.pressedClasses(v, s, true, false))) {
                    for (String token : classes.split(" ")) {
                        assertTrue(token.equals("ash-btn") || token.startsWith("ash-btn--"), "not an Ashlar Button class: " + token);
                    }
                }
            }
        }
        for (ButtonSize s : ButtonSize.values()) {
            assertTrue(ButtonStyles.iconSizeClasses(s).startsWith("ash-icon--"), s.toString());
        }
    }

    @Test
    void restingClassesAreExactlyTheUnpressedClassesAndPressedClassesAddOnlyThePressedModifier() {
        for (ButtonVariant v : ButtonVariant.values()) {
            for (ButtonSize s : ButtonSize.values()) {
                for (boolean icon : new boolean[] {false, true}) {
                    for (boolean block : new boolean[] {false, true}) {
                        String resting = ButtonStyles.restingClasses(v, s, icon, block);
                        String pressed = ButtonStyles.pressedClasses(v, s, icon, block);
                        assertEquals(ButtonStyles.classes(v, s, icon, block, false), resting);
                        assertEquals(ButtonStyles.classes(v, s, icon, block, true), pressed);
                        assertNotEquals(resting, pressed, "the two keep different meanings");
                        assertFalse(resting.contains("ash-btn--pressed"));
                        assertEquals(resting + " ash-btn--pressed", pressed, "the pressed look is one stable modifier");
                    }
                }
            }
        }
    }

    @Test
    void theIconSizesAreTheIconComponentsOwnModifiersAtTheSizesTheTailwindClassesWere() {
        assertEquals("ash-icon--xs", ButtonStyles.iconSizeClasses(ButtonSize.XSMALL));
        assertEquals("ash-icon--sm", ButtonStyles.iconSizeClasses(ButtonSize.SMALL));
        assertEquals("ash-icon--sm", ButtonStyles.iconSizeClasses(ButtonSize.MEDIUM));
        assertEquals("ash-icon--md", ButtonStyles.iconSizeClasses(ButtonSize.LARGE));
        assertEquals("ash-icon--lg", ButtonStyles.iconSizeClasses(ButtonSize.XLARGE));
    }

    @Test
    void normalizeButtonTypeIsUnchanged() {
        assertEquals("button", ButtonStyles.normalizeButtonType(null));
        assertEquals("button", ButtonStyles.normalizeButtonType(""));
        assertEquals("button", ButtonStyles.normalizeButtonType("bogus"));
        assertEquals("submit", ButtonStyles.normalizeButtonType(" Submit "));
        assertEquals("reset", ButtonStyles.normalizeButtonType("RESET"));
        assertEquals("button", ButtonStyles.normalizeButtonType("button"));
    }

    @Test
    void unknownVariantsAndSizesStillFallBackAsTheEnumsDo() {
        assertEquals(ButtonVariant.PRIMARY, ButtonVariant.from("nonsense"));
        assertEquals(ButtonSize.MEDIUM, ButtonSize.from("nonsense"));
        assertEquals("ash-btn ash-btn--primary ash-btn--md", ButtonStyles.classes(ButtonVariant.from("nonsense"), ButtonSize.from(null), false, false, false));
    }
}
