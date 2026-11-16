@import dev.redspartanlabs.ashlar.modal.ModalSize
@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

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

<%-- Closed by default: the root carries `hidden` (not rendered at all) and
     data-state="closed". modal.js removes `hidden` to show it, flips
     data-state to "open" a frame later so the entry transition runs, and on
     close flips it back and restores `hidden` once the exit has finished.
     The styling is Ashlar's own: the shared overlay primitives
     (components/overlay.css) plus the Modal's centring, size and motion
     (components/modal.css). --%>
<div
        id="${id}"
        data-modal
        data-close-on-backdrop="${closeOnBackdrop}"
        data-close-on-escape="${closeOnEscape}"
        data-state="closed"
        class="ash-overlay ash-modal"
        hidden
>
    <%-- Backdrop and dialog both start in their "closed" visual state
         (transparent, the dialog slightly raised and shrunk); data-state on the
         root selects the "open" one, and the CSS transitions between the two.
         Under prefers-reduced-motion the transitions are off, so the change is an
         instant snap without any JavaScript needing to know that preference. --%>
    <div data-modal-backdrop class="ash-overlay__backdrop"></div>

    <div
            data-modal-dialog
            role="dialog"
            aria-modal="true"
            aria-labelledby="${titleId}"
            tabindex="-1"
            class="ash-overlay__surface ash-modal__dialog ${resolvedSize.maxWidthClass()}"
    >
        <div class="ash-overlay__header">
            <h2 id="${titleId}" class="ash-overlay__title">${title}</h2>

            @if (showCloseButton)
                <button
                        type="button"
                        data-modal-close="${id}"
                        aria-label="Close"
                        class="ash-overlay__close"
                >
                    @template.ashlar.components.icon(icon = Icon.CLOSE)
                </button>
            @endif
        </div>

        <div class="ash-overlay__body">
            @if (body != null)
                ${body}
            @endif
        </div>

        @if (footer != null)
            <div class="ash-overlay__footer">
                ${footer}
            </div>
        @endif
    </div>
</div>

<script type="module" src="${AshlarAssets.js("modal.js")}"></script>
