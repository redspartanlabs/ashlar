@import com.redspartanlabs.ashlar.showcase.catalog.ComponentCatalog
@import com.redspartanlabs.ashlar.showcase.catalog.ComponentPage
@import java.util.Optional

@param String currentSlug
@param String contextPath

<%-- Every showcase page's only navigation: back to the index, plus
     previous/next through ComponentCatalog's own order - so adding a page is
     one entry in that one list, not a hand-maintained link on its
     neighbours. --%>
!{Optional<ComponentPage> prev = ComponentCatalog.previous(currentSlug);}
!{Optional<ComponentPage> next = ComponentCatalog.next(currentSlug);}

<div class="flex flex-wrap items-center justify-between gap-3 text-sm">
    <a href="${contextPath}/" class="font-medium text-slate-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">&larr; Components</a>

    <div class="flex items-center gap-4">
        @if (prev.isPresent())
            <a href="${contextPath}/components/${prev.get().slug()}" class="font-medium text-slate-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">&larr; ${prev.get().name()}</a>
        @endif
        @if (next.isPresent())
            <a href="${contextPath}/components/${next.get().slug()}" class="font-medium text-slate-600 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500">${next.get().name()} &rarr;</a>
        @endif
    </div>
</div>
