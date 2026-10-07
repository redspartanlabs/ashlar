@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog
@import com.redspartanlabs.ashlar.showcase.catalog.ComponentPage
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath
@param String slug
@param List<PageAnchor> sections

<%-- Component-page redesign. Three real pieces of context, each an
     existing Ashlar component rather than new chrome invented for this:

       1. Breadcrumbs - Components > {category} > {name}. The category
          crumb has no href on purpose: there is no /components?category=
          route, so a fake link to nowhere would be worse than honest
          plain text - a crumb with no href already renders that way per
          breadcrumb-item.jte's own contract.
       2. Page Header - the page's own real <h1>. No description passed
          here: each page's own rich intro paragraph (often containing
          inline <code>/<a> markup Page Header's plain-String description
          can't carry) stays exactly as it is, rendered as its own
          paragraph directly below this partial's own output.
       3. The "On this page" outline - grouped by what a reader is
          actually trying to do, not left as nine equal peers in document
          order. PageAnchor.groupFor(id) sorts every section into Start
          (orient yourself), Explore (see what it can do), or Reference
          (look up one fact you already know you need) - a real reading
          of the documentation grammar, not a cosmetic reshuffle. On
          sm+ viewports the outline also goes sticky once scrolled past
          the header, so picking the next section never requires
          scrolling back to the top first; below sm it stays in normal
          flow; the section links themselves are unchanged real anchors,
          so every deep link still works exactly as before. A small
          scoped IntersectionObserver highlights whichever section is
          currently in view (aria-current="location") - a progressive
          enhancement only; with JavaScript disabled the nav is still a
          complete, working set of anchor links.

     DOCUMENTATION-SITE POLISH PASS, ROUND 1: each group's links stack in a
     vertical list (`flex flex-col`) instead of wrapping horizontally, so
     the active link can carry a small `border-l-2` indicator that reads as
     a real left-edge marker rather than a stray border on an inline tag.
     The indicator reuses the exact `border-blue-700`/`dark:border-blue-400`
     pair already paired with this nav's own active text color - no new
     token, no new class, toggled by the same setActive() below that
     already toggles the text color. It is additive only: the active link
     still carries its own font-weight change and `aria-current="location"`
     exactly as before, so the active state remains legible with the
     indicator, color, or either CSS layer removed.

     ROUND 2 - UNIFIED EXPAND/CONTRACT: the three groups used to render
     directly inside <nav>, always visible. They now render inside one
     <div data-section-nav-panel>, toggled as a single unit by one
     <button data-section-nav-toggle> - not three independent disclosures,
     and not tabs (every anchor stays a real same-page #id link; nothing
     here hides content behind a tab that would stop browser Find or a
     direct hash link from reaching it while collapsed is what matters,
     and neither does - a collapsed panel still exists in the DOM, just
     with `hidden` applied, same as this library's other disclosure
     surfaces: Select's own listbox, Dropdown Menu, Modal, Drawer are all
     server-rendered closed by default and opened entirely by their own
     JS, and this nav panel follows that same existing Ashlar convention
     rather than inventing a different default. The toggle button's
     accessible name is dynamic ("Show page navigation" / "Hide page
     navigation", set in the script below) rather than drawn from its
     visible content, because that content is the Start/Explore/Reference
     GROUP labels (marked `aria-hidden` on the button itself for exactly
     this reason) - informational, not a description of what the button
     does. `aria-expanded` and `aria-controls` are real, kept in sync with
     the one `hidden` class toggle on the panel; no `role="tab"` or similar
     is introduced anywhere.

     ROUND 3 FIX - never combine a bare `hidden` toggle with a responsive
     display utility (`sm:grid`) on the SAME element. The first cut of this
     panel put `hidden ... sm:grid ...` together on one <div>, which looked
     fine in source but was actually broken: confirmed directly in the
     compiled stylesheet that `.sm\:grid{display:grid}` (inside its own
     `@media` block) sits LATER than `.hidden{display:none}` - equal
     specificity, later source position wins, so at any viewport >= the sm
     breakpoint `sm:grid` permanently overrode `hidden` regardless of which
     the toggle below set, leaving the panel stuck visibly open above that
     width with the toggle doing nothing. The fix splits the concerns onto
     two elements: the outer `[data-section-nav-panel]` only ever carries
     `hidden` (toggled, never any responsive display utility of its own),
     and a plain inner <div> carries the actual `flex flex-col sm:grid
     sm:grid-cols-3 sm:gap-6` layout permanently, with no `hidden` class
     ever applied to it. The two can no longer compete in the cascade.

     EXPANSION STATE IS INDEPENDENT OF ACTIVE-SECTION STATE, ON PURPOSE.
     The IntersectionObserver below still runs unconditionally and still
     calls the same setActive(id) regardless of whether the panel is
     expanded or collapsed - toggling classes on elements that happen to be
     `hidden` is entirely safe and has no visible effect until the panel is
     later opened, at which point the already-correct active link and its
     border-l-2 indicator are simply already there. Scrolling never calls
     the expand/collapse toggle, and the toggle never touches setActive's
     own classes - two separate pieces of state, two separate functions,
     deliberately not wired to each other. --%>
!{ComponentPage page = ComponentCatalog.bySlug(slug);}
!{List<PageAnchor> startSections = sections.stream().filter(a -> "Start".equals(PageAnchor.groupFor(a.id()))).toList();}
!{List<PageAnchor> exploreSections = sections.stream().filter(a -> "Explore".equals(PageAnchor.groupFor(a.id()))).toList();}
!{List<PageAnchor> referenceSections = sections.stream().filter(a -> "Reference".equals(PageAnchor.groupFor(a.id()))).toList();}

@template.ashlar.components.breadcrumbs(
items = @`
    @template.ashlar.components.breadcrumb-item(label = "Components", href = contextPath + "/components")
    @template.ashlar.components.breadcrumb-item(label = page.category())
    @template.ashlar.components.breadcrumb-item(label = page.name(), current = true)
`
)

<div class="mt-3">
    @template.ashlar.utilities.page-header(title = page.name())
</div>

@if (!sections.isEmpty())
    <nav aria-label="On this page" data-section-nav class="mt-4 border-y border-slate-200 bg-slate-50 text-sm dark:border-slate-800 dark:bg-slate-950 sm:sticky sm:top-0 sm:z-10">
        <button type="button" data-section-nav-toggle aria-expanded="false" aria-controls="section-nav-panel" aria-label="Show page navigation" class="flex w-full items-center justify-between gap-4 px-3 py-3 text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">
            <span class="flex flex-wrap items-baseline gap-x-4 gap-y-1" aria-hidden="true">
                @if (!startSections.isEmpty())
                    <span class="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Start</span>
                @endif
                @if (!exploreSections.isEmpty())
                    <span class="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Explore</span>
                @endif
                @if (!referenceSections.isEmpty())
                    <span class="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Reference</span>
                @endif
            </span>
            <span data-section-nav-chevron class="shrink-0 text-slate-400 transition-transform duration-150 ease-out motion-reduce:transition-none dark:text-slate-500">
                @template.ashlar.components.icon(icon = Icon.CHEVRON_DOWN, classes = "h-4 w-4")
            </span>
        </button>

        <div id="section-nav-panel" data-section-nav-panel class="hidden border-t border-slate-200 px-3 pb-3 pt-3 dark:border-slate-800">
            <div class="flex flex-col gap-4 sm:grid sm:grid-cols-3 sm:gap-6">
                @if (!startSections.isEmpty())
                    <div>
                        <span class="block text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Start</span>
                        <div class="mt-1 flex flex-col gap-1">
                            @for (PageAnchor anchor : startSections)
                                <a href="#${anchor.id()}" data-section-link="${anchor.id()}" class="border-l-2 border-transparent pl-2 text-slate-500 underline-offset-2 hover:text-blue-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400">${anchor.label()}</a>
                            @endfor
                        </div>
                    </div>
                @endif
                @if (!exploreSections.isEmpty())
                    <div>
                        <span class="block text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Explore</span>
                        <div class="mt-1 flex flex-col gap-1">
                            @for (PageAnchor anchor : exploreSections)
                                <a href="#${anchor.id()}" data-section-link="${anchor.id()}" class="border-l-2 border-transparent pl-2 text-slate-500 underline-offset-2 hover:text-blue-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400">${anchor.label()}</a>
                            @endfor
                        </div>
                    </div>
                @endif
                @if (!referenceSections.isEmpty())
                    <div>
                        <span class="block text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Reference</span>
                        <div class="mt-1 flex flex-col gap-1">
                            @for (PageAnchor anchor : referenceSections)
                                <a href="#${anchor.id()}" data-section-link="${anchor.id()}" class="border-l-2 border-transparent pl-2 text-slate-500 underline-offset-2 hover:text-blue-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400">${anchor.label()}</a>
                            @endfor
                        </div>
                    </div>
                @endif
            </div>
        </div>
    </nav>
    <script>
        (function () {
            var nav = document.currentScript.previousElementSibling;
            if (!nav || !nav.hasAttribute("data-section-nav")) {
                return;
            }

            var toggle = nav.querySelector("[data-section-nav-toggle]");
            var panel = nav.querySelector("[data-section-nav-panel]");
            var chevron = nav.querySelector("[data-section-nav-chevron]");

            // Expand/contract: entirely user-driven, via this one toggle.
            // Deliberately never touched by the IntersectionObserver below -
            // scrolling changes which link looks active, never whether the
            // panel is open.
            if (toggle && panel) {
                toggle.addEventListener("click", function () {
                    var expanded = toggle.getAttribute("aria-expanded") === "true";
                    var next = !expanded;
                    toggle.setAttribute("aria-expanded", String(next));
                    toggle.setAttribute("aria-label", next ? "Hide page navigation" : "Show page navigation");
                    panel.classList.toggle("hidden", !next);
                    if (chevron) {
                        chevron.classList.toggle("rotate-180", next);
                    }
                });
            }

            // Scroll-spy setup is deferred until the document has finished
            // parsing, unlike the toggle wiring above. Reason, confirmed by
            // instrumenting the real page in a headless browser rather than
            // assumed: this <script> sits right after <nav>, but the actual
            // <section id="..."> elements it needs to find live much later
            // in the same page, inside <main>, after this script tag in
            // document order. A plain synchronous <script> executes the
            // instant the parser reaches it - before any later element has
            // been parsed - so every document.getElementById(id) call below
            // returned null, `targets` stayed empty, and the function
            // returned before ever constructing an IntersectionObserver.
            // Verified directly: instrumenting window.IntersectionObserver
            // showed it was never invoked at all. <nav> itself (and
            // everything the toggle above touches) is already fully parsed
            // by this point, which is why expand/contract never showed this
            // problem - only the section lookups, which reach forward into
            // content this script hasn't been parsed past yet, needed to
            // wait.
            function initScrollSpy() {
                if (!("IntersectionObserver" in window)) {
                    return;
                }
                var links = nav.querySelectorAll("[data-section-link]");
                var targets = [];
                links.forEach(function (link) {
                    var section = document.getElementById(link.getAttribute("data-section-link"));
                    if (section) {
                        targets.push({ link: link, section: section });
                    }
                });
                if (!targets.length) {
                    return;
                }
                // Document order, not nav order - the nav groups links by
                // Start/Explore/Reference (reader intent), which does not
                // match where sections actually sit on the page (e.g. Data
                // Table's "Common Patterns" renders after "Best Practices"
                // but is grouped before it). The furthest-passed-heading
                // model below depends on iterating sections top-to-bottom,
                // so they are re-sorted here by their real position,
                // computed once before any scrolling happens.
                targets.sort(function (a, b) {
                    return a.section.getBoundingClientRect().top - b.section.getBoundingClientRect().top;
                });
                var setActive = function (id) {
                    links.forEach(function (link) {
                        var active = link.getAttribute("data-section-link") === id;
                        // Each active/inactive pair below is toggled in BOTH
                        // directions, not only added - "text-blue-700" and
                        // "border-blue-700" alone are not enough, because the
                        // link's own base markup keeps "text-slate-500" and
                        // "border-transparent" permanently in its class list.
                        // Confirmed directly in the compiled stylesheet that
                        // both base utilities compile LATER than their active
                        // counterparts (equal specificity, later source wins),
                        // so simply adding the active class left it silently
                        // overridden the whole time - correct in the DOM,
                        // invisible on screen. Removing the base class when
                        // active (and restoring it when not) makes the two
                        // mutually exclusive, which is the only way to make
                        // this reliable regardless of utility registration
                        // order.
                        link.classList.toggle("text-blue-700", active);
                        link.classList.toggle("dark:text-blue-400", active);
                        link.classList.toggle("text-slate-500", !active);
                        link.classList.toggle("dark:text-slate-400", !active);
                        link.classList.toggle("font-medium", active);
                        link.classList.toggle("border-blue-700", active);
                        link.classList.toggle("dark:border-blue-400", active);
                        link.classList.toggle("border-transparent", !active);
                        if (active) {
                            link.setAttribute("aria-current", "location");
                        } else {
                            link.removeAttribute("aria-current");
                        }
                    });
                };
                // ACTIVE-SECTION MODEL, confirmed against the real built
                // pages in a headless browser, not assumed: a reader's
                // "current section" is the furthest one (in document
                // order) whose heading has already scrolled past a fixed
                // reading line - not "whichever section currently overlaps
                // a band of the screen", which is what per-entry
                // isIntersecting checks below used to decide. That area-
                // overlap model had two confirmed, reproducible problems:
                // (1) a section near the very end of the document can sit
                // geometrically below the band for every reachable scroll
                // position - the page simply runs out of room to scroll it
                // up far enough - so it could never activate, no matter
                // what rootMargin value was chosen (verified: no fixed
                // percentage/pixel band fixes this in general, since the
                // minimum achievable on-screen position of a trailing
                // section's top is fixed by document length, not by the
                // band). (2) a direct nav-link click (an instant jump, not
                // a gradual scroll) can land two sections in the band in
                // the SAME callback batch at once - confirmed directly:
                // clicking "How to Use" produced one batch with
                // {how-to-use: ratio 0.92} and {variants: ratio 0.02} both
                // isIntersecting - and the old per-entry loop simply let
                // whichever entry was processed last win, which was the
                // 2%-overlap one, not the 92%-overlap one the reader
                // actually clicked. Recomputing fresh from live geometry on
                // every tick - rather than trusting a single entry's own
                // isIntersecting flag - has no batch-ordering question to
                // get wrong in the first place, and fixes both problems
                // from one rule instead of special-casing either.
                var readLine = 88;
                var computeActiveId = function () {
                    var maxScrollY = document.documentElement.scrollHeight - window.innerHeight;
                    // At the bottom of the document there is nothing
                    // further to read, so the last section is the answer
                    // regardless of where the reading line falls - not a
                    // bolted-on exception, just the boundary case of a
                    // model defined in terms of real scroll position: the
                    // page cannot be scrolled past maxScrollY, so "passed"
                    // for the final section can only ever be evaluated
                    // against the scroll range that actually exists.
                    if (window.scrollY >= maxScrollY - 1) {
                        return targets[targets.length - 1].link.getAttribute("data-section-link");
                    }
                    var activeId = null;
                    for (var i = 0; i < targets.length; i++) {
                        if (targets[i].section.getBoundingClientRect().top <= readLine) {
                            activeId = targets[i].link.getAttribute("data-section-link");
                        }
                    }
                    return activeId;
                };
                var update = function () {
                    setActive(computeActiveId());
                };
                var observer = new IntersectionObserver(update, { rootMargin: "-88px 0px -70% 0px", threshold: 0 });
                targets.forEach(function (t) {
                    observer.observe(t.section);
                });
                // The observer above still does most of the work - it is
                // the trigger for every ordinary scroll-driven change, the
                // same as before. It is not sufficient entirely on its own
                // for the exact "reached the bottom" instant, though:
                // confirmed by measurement that on some pages the last
                // threshold crossing the observer ever reports can land a
                // few pixels short of the document's true maximum scroll
                // position (nothing further crosses the band after that,
                // so the observer simply never fires again) - which would
                // leave the second-to-last section stuck active for that
                // last stretch instead of the true last one. This passive
                // scroll listener recomputes from the same geometry and is
                // what actually guarantees the "at maximum scroll, the
                // final section is active" case always holds exactly, not
                // only almost. It does not replace the observer above or
                // change what it watches - every ordinary scroll position
                // is already handled correctly by the observer's own
                // crossings; this only closes the specific gap at the
                // very end that the observer's crossing-based nature can
                // leave on some pages.
                var scrollUpdateQueued = false;
                window.addEventListener("scroll", function () {
                    if (scrollUpdateQueued) {
                        return;
                    }
                    scrollUpdateQueued = true;
                    window.requestAnimationFrame(function () {
                        scrollUpdateQueued = false;
                        update();
                    });
                }, { passive: true });
                update();
            }

            if (document.readyState === "loading") {
                document.addEventListener("DOMContentLoaded", initScrollSpy);
            } else {
                initScrollSpy();
            }
        })();
    </script>
@endif
