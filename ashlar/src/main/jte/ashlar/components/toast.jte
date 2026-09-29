@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.toast.ToastStyles
@import com.redspartanlabs.ashlar.toast.ToastVariant

@param String id = ""
@param String variant = "Info"
@param String title = ""
@param String message = ""
@param Icon icon = null
@param boolean dismissible = true
@param Integer duration = null
@param boolean visible = true
@param gg.jte.Content action = null

<%-- One notification. Place it inside toast-region's `toasts` content, or let
     toast-region render it as a <template> that toast.js clones.

     duration: milliseconds; 0 = stays until dismissed; omitted = the
     variant's default (errors persist, everything else 5s). A toast that
     never auto-dismisses is always given a close button, so it can't get
     stuck on screen. --%>
!{ToastVariant resolvedVariant = ToastVariant.from(variant);}
!{Icon resolvedIcon = icon != null ? icon : resolvedVariant.defaultIcon();}
!{int resolvedDuration = duration != null ? Math.max(0, duration) : resolvedVariant.defaultDurationMs();}
!{boolean resolvedDismissible = dismissible || resolvedDuration == 0;}
!{String idAttr = id.isEmpty() ? null : id;}
!{String closeLabel = title.isEmpty() ? "Dismiss notification" : "Dismiss notification: " + title;}
!{String titleVisibility = title.isEmpty() ? "hidden" : "";}
!{String messageVisibility = message.isEmpty() ? "hidden" : "";}

<div
        id="${idAttr}"
        data-toast
        data-toast-politeness="${resolvedVariant.politeness()}"
        data-toast-duration="${resolvedDuration}"
        data-toast-state="${visible ? "open" : "hidden"}"
        class="${ToastStyles.container(resolvedVariant)}"
>
    @template.ashlar.components.icon(icon = resolvedIcon, classes = "mt-0.5 h-5 w-5 " + ToastStyles.icon(resolvedVariant))

    <div class="flex min-w-0 flex-1 flex-col gap-1">
        <span class="sr-only" data-toast-label>${resolvedVariant.label()}:</span>
        <p data-toast-title class="${titleVisibility} text-sm font-semibold text-slate-900 dark:text-slate-100">${title}</p>
        <p data-toast-message class="${messageVisibility} break-words text-sm text-slate-600 dark:text-slate-300">${message}</p>
        @if (action != null)
            <div class="mt-2 flex flex-wrap items-center gap-3 text-sm">${action}</div>
        @endif
    </div>

    @if (resolvedDismissible)
        <button
                type="button"
                data-toast-dismiss
                data-toast-close
                aria-label="${closeLabel}"
                class="-m-1 shrink-0 rounded-md p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-500 dark:hover:bg-slate-700 dark:hover:text-slate-300"
        >
            @template.ashlar.components.icon(icon = Icon.CLOSE, classes = "h-4 w-4")
        </button>
    @endif
</div>
