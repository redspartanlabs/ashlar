@param String title = ""
@param gg.jte.Content body = null
@param gg.jte.Content footer = null

<%-- A content surface, not a layout primitive: border/radius/background
     only - no width, margin, or grid placement, so the surrounding page
     stays entirely responsible for where and how wide this sits.

     No `role="region"`: a generic reusable Card used many times on one
     page (a dashboard of a dozen cards, say) would turn every one of them
     into a landmark, which defeats the point of landmarks as a short list
     of a page's major regions. A page that genuinely needs one particular
     Card to be a landmark can add that itself at the call site.

     Unlike modal.jte's shadow-xl (an overlay floating above the page, which
     needs a shadow to read as detached from it), Card sits in normal
     document flow next to other content and only needs a border to read as
     a distinct surface - a shadow here would suggest floating that isn't
     happening.

     `title` is a plain string, not richer content, and always renders as an
     <h3> when present - matching modal.jte's own title param exactly. A
     page assembling several Cards under its own h1/h2 structure gets a
     consistent one-level-down heading without having to think about it. --%>
!{boolean hasTitle = !title.isEmpty();}

<div class="overflow-hidden rounded-xl border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900" data-card>
    @if (hasTitle)
        <div class="border-b border-slate-200 px-6 py-4 dark:border-slate-700">
            <h3 class="text-base font-semibold text-slate-900 dark:text-slate-100">${title}</h3>
        </div>
    @endif

    @if (body != null)
        <div class="px-6 py-4 text-sm text-slate-700 dark:text-slate-300">
            ${body}
        </div>
    @endif

    @if (footer != null)
        <div class="flex flex-wrap items-center justify-end gap-3 border-t border-slate-200 bg-slate-50 px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
            ${footer}
        </div>
    @endif
</div>
