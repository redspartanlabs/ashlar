package dev.redspartanlabs.ashlar.divider;

/**
 * The single place that maps a divider orientation to its CSS classes.
 *
 * <p>The classes are Ashlar's own ({@code ash-divider} plus one {@code ash-divider--<orientation>}
 * modifier), styled by {@code components/divider.css} in the generated {@code ashlar.css}. They no
 * longer depend on Tailwind or its preflight. The line colour is the same slate-200/slate-700
 * border token as every other bordered surface in the library, read through
 * {@code --ash-divider-color}.
 */
public final class DividerStyles {

    private DividerStyles() {
    }

    /** A native <hr>: margin zeroed out so the component has no spacing
     *  opinion of its own - the layout around it owns all spacing. */
    public static String horizontal() {
        return "ash-divider ash-divider--horizontal";
    }

    /** self-stretch fills whatever height a flex row gives it - without it
     *  a 0-height div is invisible. That's the divider making itself visible
     *  inside a flex row, not the component opining on layout spacing.
     *
     *  <p>A 1px left border hairline, not a 1px-wide background-color box: browsers
     *  rasterize a 1px border crisply at any zoom/scale, where a 1px-wide
     *  background box can anti-alias into near-invisibility under fractional
     *  device pixel ratios - confirmed by browser measurement while building
     *  this component. */
    public static String vertical() {
        return "ash-divider ash-divider--vertical";
    }
}
