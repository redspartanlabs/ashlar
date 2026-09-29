@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

@param String id = ""

!{String idAttr = id.isEmpty() ? null : id;}

<%-- Rendered assuming light mode, since the server has no way to know the
     client's stored preference or OS setting. theme.js corrects this (icon,
     label, aria-pressed) for every toggle on the page immediately at init,
     using the data-theme attribute the inline head script already set
     before first paint - see home.jte for that part. --%>
<button
        type="button"
        id="${idAttr}"
        data-theme-toggle
        aria-pressed="false"
        class="inline-flex items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
>
    <span data-theme-toggle-icon-light>
        @template.ashlar.components.icon(icon = Icon.SUN, classes = "h-4 w-4")
    </span>
    <span data-theme-toggle-icon-dark class="hidden">
        @template.ashlar.components.icon(icon = Icon.MOON, classes = "h-4 w-4")
    </span>
    <span data-theme-toggle-label>Light</span>
</button>

<script type="module" src="${AshlarAssets.js("theme.js")}"></script>
