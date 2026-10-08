package dev.redspartanlabs.ashlar.divider;

/**
 * The single place that maps a divider orientation to Tailwind classes.
 * Uses the same slate-200/slate-700 border tokens as every other bordered
 * surface in the library (data-table, modal, card-shaped sections) rather
 * than a separate color choice.
 */
public final class DividerStyles {

    private DividerStyles() {
    }

    /** A native <hr>: margin zeroed out so the component has no spacing
     *  opinion of its own - the layout around it owns all spacing. */
    public static String horizontal() {
        return "m-0 w-full border-0 border-t border-slate-200 dark:border-slate-700";
    }

    /** self-stretch fills whatever height a flex row gives it - without it
     *  a 0-height div is invisible. That's the divider making itself visible
     *  inside a flex row, not the component opining on layout spacing.
     *
     *  <p>A `border-l` hairline, not a `w-px` background-color box: browsers
     *  rasterize a 1px border crisply at any zoom/scale, where a 1px-wide
     *  background box can anti-alias into near-invisibility under fractional
     *  device pixel ratios - confirmed by browser measurement while building
     *  this component. */
    public static String vertical() {
        return "inline-block w-0 self-stretch border-l border-slate-200 dark:border-slate-700";
    }
}
