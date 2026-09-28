@import com.redspartanlabs.ashlar.modal.ModalSize
@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

@param String id
@param String title
@param String size = "Medium"
@param boolean closeOnBackdrop = true
@param boolean closeOnEscape = true
@param boolean showCloseButton = true
@param gg.jte.Content body = null
@param gg.jte.Content footer = null

!{ModalSize resolvedSize = ModalSize.from(size);}
!{String titleId = id + "-title";}

<%-- Closed by default. JS toggles "hidden" <-> "flex" together (never both
     at once) to show/hide this, the same pattern used by every other
     show/hide element in this project. --%>
<div
        id="${id}"
        data-modal
        data-close-on-backdrop="${closeOnBackdrop}"
        data-close-on-escape="${closeOnEscape}"
        class="fixed inset-0 z-40 hidden items-center justify-center p-4"
>
    <%-- Backdrop/dialog both start in their "closed" visual state (transparent,
         dialog slightly raised and shrunk). JS toggles opacity-*/scale-*/
         translate-* in matched pairs to animate between this and the "open"
         state; motion-reduce: drops the animation to an instant snap without
         any JavaScript needing to know about that preference. --%>
    <div
            data-modal-backdrop
            class="absolute inset-0 bg-slate-900/50 opacity-0 transition-opacity duration-200 ease-out motion-reduce:transition-none"
    ></div>

    <div
            data-modal-dialog
            role="dialog"
            aria-modal="true"
            aria-labelledby="${titleId}"
            tabindex="-1"
            class="relative flex max-h-full w-full ${resolvedSize.maxWidthClass()} -translate-y-1 scale-95 flex-col overflow-hidden rounded-xl bg-white opacity-0 shadow-xl transition duration-200 ease-out focus:outline-none motion-reduce:transition-none dark:bg-slate-900"
    >
        <div class="flex items-center justify-between gap-4 border-b border-slate-200 px-6 py-4 dark:border-slate-700">
            <h2 id="${titleId}" class="text-base font-semibold text-slate-900 dark:text-slate-100">${title}</h2>

            @if (showCloseButton)
                <button
                        type="button"
                        data-modal-close="${id}"
                        aria-label="Close"
                        class="rounded p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:hover:bg-slate-800 dark:hover:text-slate-300"
                >
                    @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "h-5 w-5")
                </button>
            @endif
        </div>

        <div class="flex-1 overflow-y-auto px-6 py-4 text-sm text-slate-700 dark:text-slate-300">
            @if (body != null)
                ${body}
            @endif
        </div>

        @if (footer != null)
            <div class="flex flex-wrap items-center justify-end gap-3 border-t border-slate-200 bg-slate-50 px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
                ${footer}
            </div>
        @endif
    </div>
</div>

<script type="module" src="${AshlarAssets.js("modal.js")}"></script>
