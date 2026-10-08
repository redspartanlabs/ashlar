@import dev.redspartanlabs.ashlar.toast.ToastVariant
@import dev.redspartanlabs.ashlar.AshlarAssets

@param gg.jte.Content toasts = null

<%-- The page's single toast host. Render it once, near the end of <body>.

     - data-toast-region: where toasts stack (top-right, full width on narrow
       screens). Newest toasts are appended below older ones, so a toast
       someone is reading never shifts when another one arrives.
     - data-toast-announcer: two visually hidden live regions, kept separate
       from the visible toasts. Announcing through these (instead of making
       each toast a live region) means a toast's close button and action are
       never re-read, rapid toasts are each announced once, and only errors
       interrupt.
     - data-toast-template: one pre-rendered toast per variant. toast.js
       clones these for showToast(), so all toast markup and classes still
       come from toast.jte. --%>
<section
        data-toast-region
        aria-label="Notifications"
        class="pointer-events-none fixed inset-x-4 top-4 z-50 flex flex-col gap-3 sm:left-auto sm:w-96"
>
    @if (toasts != null)
        ${toasts}
    @endif
</section>

<div data-toast-announcer="polite" class="sr-only" aria-live="polite" aria-atomic="false"></div>
<div data-toast-announcer="assertive" class="sr-only" aria-live="assertive" aria-atomic="false"></div>

@for (ToastVariant variant : ToastVariant.values())
    <template data-toast-template="${variant.name().toLowerCase()}">
        @template.ashlar.components.toast(variant = variant.name(), visible = false)
    </template>
@endfor

<script type="module" src="${AshlarAssets.js("toast.js")}"></script>
