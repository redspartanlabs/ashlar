package dev.redspartanlabs.ashlar.showcase.cascade;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Each rule is tested twice: that it accepts the one situation it exists for, and that it REFUSES to
 * hide a genuine regression when any precondition fails. The refusals matter more than the acceptances.
 */
class ParityNormalizerTest {

    private static final String RING = "oklab(0.554 -0.0100213 -0.0448951 / 0.2) 0px 0px 0px 1px inset";
    private static final String TW_SHADOW = "rgba(0, 0, 0, 0) 0px 0px 0px 0px, rgba(0, 0, 0, 0) 0px 0px 0px 0px, "
            + "rgba(0, 0, 0, 0) 0px 0px 0px 0px, " + RING + ", rgba(0, 0, 0, 0) 0px 0px 0px 0px";

    private static ParityNormalizer.Side side(String parentDisplay, String... pairs) {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("width", "54px");
        props.put("height", "24px");
        for (int i = 0; i < pairs.length; i += 2) {
            props.put(pairs[i], pairs[i + 1]);
        }
        return new ParityNormalizer.Side(props, parentDisplay);
    }

    private static List<String> remaining(ParityNormalizer.Side expected, ParityNormalizer.Side actual) {
        return ParityNormalizer.compare(expected, actual).remaining().stream().map(ParityNormalizer.Difference::property).toList();
    }

    // ---- the default: exact equality ----

    @Test
    void identicalSidesHaveNoDifferencesAndNoNormalization() {
        ParityNormalizer.Result r = ParityNormalizer.compare(side("block", "color", "red"), side("block", "color", "red"));
        assertEquals(List.of(), r.remaining());
        assertEquals(List.of(), r.applied());
    }

    @Test
    void anyOtherPropertyIsComparedExactly() {
        assertEquals(List.of("color"), remaining(side("block", "color", "red"), side("block", "color", "rgb(255, 0, 1)")));
        assertEquals(List.of("padding-left"), remaining(side("block", "padding-left", "8px"), side("block", "padding-left", "9px")));
        assertEquals(List.of("font-size"), remaining(side("block", "font-size", "12px"), side("block", "font-size", "12.5px")));
    }

    @Test
    void aPropertyPresentOnOnlyOneSideIsADifference() {
        assertEquals(List.of("gap"), remaining(side("block", "gap", "4px"), side("block")));
        assertEquals(List.of("gap"), remaining(side("block"), side("block", "gap", "4px")));
    }

    // ---- H1: box-shadow ----

    @Test
    void h1AcceptsTailwindsRingListAgainstASingleInsetRing() {
        ParityNormalizer.Result r = ParityNormalizer.compare(side("block", "box-shadow", TW_SHADOW), side("block", "box-shadow", RING));
        assertEquals(List.of(), r.remaining());
        assertEquals(List.of("H1 box-shadow"), r.applied());
    }

    @Test
    void h1TreatsNoneAsAnEmptyList() {
        assertEquals(List.of(), remaining(side("block", "box-shadow", "none"), side("block", "box-shadow", "rgba(0, 0, 0, 0) 0px 0px 0px 0px")));
    }

    @Test
    void h1RefusesAMissingRing() {
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", TW_SHADOW), side("block", "box-shadow", "none")));
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", TW_SHADOW),
                side("block", "box-shadow", "rgba(0, 0, 0, 0) 0px 0px 0px 0px")));
    }

    @Test
    void h1RefusesADifferentRingColourSizeOrKind() {
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", TW_SHADOW),
                side("block", "box-shadow", "oklab(0.554 -0.0100213 -0.0448951 / 0.3) 0px 0px 0px 1px inset")));
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", TW_SHADOW),
                side("block", "box-shadow", "oklab(0.554 -0.0100213 -0.0448951 / 0.2) 0px 0px 0px 2px inset")));
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", TW_SHADOW),
                side("block", "box-shadow", "oklab(0.554 -0.0100213 -0.0448951 / 0.2) 0px 0px 0px 1px")));
    }

    @Test
    void h1OnlyDropsLayersThatAreExactlyTransparentAndZeroSized() {
        // A transparent layer that is offset, or blurred, is a real (if invisible-today) layer: not dropped.
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", RING + ", rgba(0, 0, 0, 0) 1px 0px 0px 0px"),
                side("block", "box-shadow", RING)));
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", RING + ", rgba(0, 0, 0, 0) 0px 0px 4px 0px"),
                side("block", "box-shadow", RING)));
    }

    @Test
    void h1RefusesAnExtraRealShadowLayerAndAReorder() {
        String drop = "rgba(0, 0, 0, 0.1) 0px 1px 3px 0px";
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", TW_SHADOW + ", " + drop), side("block", "box-shadow", RING)));
        assertEquals(List.of("box-shadow"), remaining(side("block", "box-shadow", RING + ", " + drop), side("block", "box-shadow", drop + ", " + RING)));
    }

    @Test
    void h1KeepsFunctionalColoursWithCommasIntact() {
        assertEquals(List.of(), remaining(side("block", "box-shadow", "rgb(1, 2, 3) 0px 0px 0px 1px inset"),
                side("block", "box-shadow", "rgba(0, 0, 0, 0) 0px 0px 0px 0px, rgb(1, 2, 3) 0px 0px 0px 1px inset")));
    }

    // ---- H2: border style at zero width ----

    @Test
    void h2AcceptsAStyleDifferenceWhenTheWidthIsZeroOnBothSides() {
        ParityNormalizer.Result r = ParityNormalizer.compare(
                side("block", "border-top-style", "solid", "border-top-width", "0px"),
                side("block", "border-top-style", "none", "border-top-width", "0px"));
        assertEquals(List.of(), r.remaining());
        assertEquals(List.of("H2 border-top-style"), r.applied());
    }

    @Test
    void h2RefusesAStyleDifferenceWhenAnyWidthIsVisible() {
        assertEquals(List.of("border-top-style"), remaining(
                side("block", "border-top-style", "solid", "border-top-width", "1px"),
                side("block", "border-top-style", "none", "border-top-width", "1px")));
        // One side has a visible border: the width itself is a difference, and so is the style (no normalization).
        assertEquals(List.of("border-top-style", "border-top-width"), remaining(
                side("block", "border-top-style", "solid", "border-top-width", "0px"),
                side("block", "border-top-style", "dashed", "border-top-width", "1px")));
    }

    @Test
    void h2RefusesWhenTheWidthIsUnknownOrFromAnotherSide() {
        assertEquals(List.of("border-top-style"), remaining(side("block", "border-top-style", "solid"), side("block", "border-top-style", "none")));
        assertEquals(List.of("border-right-style"), remaining(
                side("block", "border-right-style", "solid", "border-top-width", "0px"),
                side("block", "border-right-style", "none", "border-top-width", "0px")));
    }

    // ---- H3: vertical-align on flex/grid items ----

    @Test
    void h3AcceptsAFlexOrGridItem() {
        for (String display : List.of("flex", "inline-flex", "grid", "inline-grid")) {
            ParityNormalizer.Result r = ParityNormalizer.compare(side(display, "vertical-align", "middle"), side(display, "vertical-align", "baseline"));
            assertEquals(List.of(), r.remaining(), display);
            assertEquals(List.of("H3 vertical-align"), r.applied());
        }
    }

    @Test
    void h3RefusesAnythingThatIsNotAFlexOrGridItemOnBothSides() {
        assertEquals(List.of("vertical-align"), remaining(side("block", "vertical-align", "middle"), side("block", "vertical-align", "baseline")));
        assertEquals(List.of("vertical-align"), remaining(side("table-cell", "vertical-align", "middle"), side("table-cell", "vertical-align", "top")));
        assertEquals(List.of("vertical-align"), remaining(side("inline-flex", "vertical-align", "middle"), side("block", "vertical-align", "baseline")));
        assertEquals(List.of("vertical-align"), remaining(side(null, "vertical-align", "middle"), side(null, "vertical-align", "baseline")));
    }

    // ---- H4: pill radius ----

    @Test
    void h4AcceptsAnyTwoRadiiThatBothFormAFullPill() {
        ParityNormalizer.Result r = ParityNormalizer.compare(
                side("block", "border-top-left-radius", "3.35544e+07px"),
                side("block", "border-top-left-radius", "9999px"));
        assertEquals(List.of(), r.remaining());
        assertEquals(List.of("H4 border-top-left-radius"), r.applied());
    }

    @Test
    void h4AcceptsExactlyHalfTheShorterSide() {
        assertEquals(List.of(), remaining(side("block", "border-top-left-radius", "12px"), side("block", "border-top-left-radius", "9999px")));
    }

    @Test
    void h4RefusesARadiusBelowHalfTheShorterSide() {
        assertEquals(List.of("border-top-left-radius"), remaining(
                side("block", "border-top-left-radius", "3.35544e+07px"), side("block", "border-top-left-radius", "11px")));
        assertEquals(List.of("border-top-left-radius"), remaining(
                side("block", "border-top-left-radius", "0px"), side("block", "border-top-left-radius", "9999px")));
        assertEquals(List.of("border-top-left-radius"), remaining(
                side("block", "border-top-left-radius", "3px"), side("block", "border-top-left-radius", "9999px")));
    }

    @Test
    void h4UsesEachSidesOwnMeasuredBox() {
        // The actual box is 40px high, so 15px is not a pill there even though it is above 12px.
        // (The 40px height is itself a difference and is reported too; the point is that the radius is not excused.)
        ParityNormalizer.Side tall = new ParityNormalizer.Side(Map.of("width", "54px", "height", "40px", "border-top-left-radius", "15px"), "block");
        assertEquals(List.of("height", "border-top-left-radius"), remaining(side("block", "border-top-left-radius", "3.35544e+07px"), tall));
        // Same height on both sides: 15px on a 40px box is still not a pill, and nothing else differs.
        ParityNormalizer.Side tallExpected = new ParityNormalizer.Side(Map.of("width", "54px", "height", "40px", "border-top-left-radius", "3.35544e+07px"), "block");
        assertEquals(List.of("border-top-left-radius"), remaining(tallExpected, tall));
    }

    @Test
    void h4RefusesWhenTheBoxIsUnmeasuredOrTheValueIsNotPixels() {
        ParityNormalizer.Side unmeasured = new ParityNormalizer.Side(Map.of("border-top-left-radius", "9999px"), "block");
        assertEquals(List.of("border-top-left-radius"), remaining(
                new ParityNormalizer.Side(Map.of("border-top-left-radius", "3.35544e+07px"), "block"), unmeasured));
        assertEquals(List.of("border-top-left-radius"), remaining(
                side("block", "border-top-left-radius", "50%"), side("block", "border-top-left-radius", "9999px")));
    }

    @Test
    void h4IsPerCornerSoOneSquareCornerIsStillCaught() {
        assertEquals(List.of("border-bottom-right-radius"), remaining(
                side("block", "border-top-left-radius", "3.35544e+07px", "border-bottom-right-radius", "3.35544e+07px"),
                side("block", "border-top-left-radius", "9999px", "border-bottom-right-radius", "0px")));
    }

    // ---- no rule hides a genuine difference next to it ----

    @Test
    void allFourRulesTogetherStillReportEveryOtherDifference() {
        ParityNormalizer.Side legacy = side("inline-flex", "box-shadow", TW_SHADOW, "border-top-style", "solid", "border-top-width", "0px",
                "vertical-align", "middle", "border-top-left-radius", "3.35544e+07px", "padding-left", "8px", "color", "red");
        ParityNormalizer.Side actual = side("inline-flex", "box-shadow", RING, "border-top-style", "none", "border-top-width", "0px",
                "vertical-align", "baseline", "border-top-left-radius", "9999px", "padding-left", "9px", "color", "red");
        ParityNormalizer.Result r = ParityNormalizer.compare(legacy, actual);
        assertEquals(List.of("padding-left"), r.remaining().stream().map(ParityNormalizer.Difference::property).toList());
        assertEquals(4, r.applied().size());
        assertTrue(r.applied().containsAll(List.of("H1 box-shadow", "H2 border-top-style", "H3 vertical-align", "H4 border-top-left-radius")));
    }

    @Test
    void pixelParsingRejectsNonPixelValues() {
        assertEquals(12.0, ParityNormalizer.px("12px"));
        assertEquals(3.35544e7, ParityNormalizer.px("3.35544e+07px"));
        assertTrue(Double.isNaN(ParityNormalizer.px("auto")));
        assertTrue(Double.isNaN(ParityNormalizer.px("1rem")));
        assertTrue(Double.isNaN(ParityNormalizer.px(null)));
    }
}
