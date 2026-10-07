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
          complete, working set of anchor links. --%>
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
    <nav aria-label="On this page" data-section-nav class="mt-4 flex flex-col gap-2 border-y border-slate-200 bg-slate-50 py-3 text-sm dark:border-slate-800 dark:bg-slate-950 sm:sticky sm:top-0 sm:z-10">
        @if (!startSections.isEmpty())
            <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <span class="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Start</span>
                @for (PageAnchor anchor : startSections)
                    <a href="#${anchor.id()}" data-section-link="${anchor.id()}" class="text-slate-500 underline-offset-2 hover:text-blue-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400">${anchor.label()}</a>
                @endfor
            </div>
        @endif
        @if (!exploreSections.isEmpty())
            <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <span class="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Explore</span>
                @for (PageAnchor anchor : exploreSections)
                    <a href="#${anchor.id()}" data-section-link="${anchor.id()}" class="text-slate-500 underline-offset-2 hover:text-blue-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400">${anchor.label()}</a>
                @endfor
            </div>
        @endif
        @if (!referenceSections.isEmpty())
            <div class="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <span class="text-xs font-semibold uppercase tracking-wide text-slate-400 dark:text-slate-500">Reference</span>
                @for (PageAnchor anchor : referenceSections)
                    <a href="#${anchor.id()}" data-section-link="${anchor.id()}" class="text-slate-500 underline-offset-2 hover:text-blue-700 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400">${anchor.label()}</a>
                @endfor
            </div>
        @endif
    </nav>
    <script>
        (function () {
            var nav = document.currentScript.previousElementSibling;
            if (!nav || !nav.hasAttribute("data-section-nav") || !("IntersectionObserver" in window)) {
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
            var setActive = function (id) {
                links.forEach(function (link) {
                    var active = link.getAttribute("data-section-link") === id;
                    link.classList.toggle("text-blue-700", active);
                    link.classList.toggle("dark:text-blue-400", active);
                    link.classList.toggle("font-medium", active);
                    if (active) {
                        link.setAttribute("aria-current", "location");
                    } else {
                        link.removeAttribute("aria-current");
                    }
                });
            };
            var observer = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    if (!entry.isIntersecting) {
                        return;
                    }
                    for (var i = 0; i < targets.length; i++) {
                        if (targets[i].section === entry.target) {
                            setActive(targets[i].link.getAttribute("data-section-link"));
                            break;
                        }
                    }
                });
            }, { rootMargin: "-88px 0px -70% 0px", threshold: 0 });
            targets.forEach(function (t) {
                observer.observe(t.section);
            });
        })();
    </script>
@endif
