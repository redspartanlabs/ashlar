@import dev.redspartanlabs.ashlar.icon.Icon

@param String label = ""
@param String code = ""
@param gg.jte.Content content

<%-- Component-page redesign (proof set). One live specimen, with its own
     exact source one disclosure-click away - the pairing usage-snippet.jte
     alone couldn't give every Variants & States example, since that
     partial has no live-render slot of its own and was only ever called
     once per page, at Quick Start. The old pages showed code exactly
     once and prose-plus-live-render everywhere else; a reader who wanted
     the real call behind the fourth button variant had to infer it. This
     closes that gap without inventing a second infrastructure: source is
     still a Java String built the same safe way usage-snippet.jte's own
     `code` param already requires (see that file's doc comment on the
     `@`+backtick hazard) and still renders through the identical
     `<pre><code>` convention - only the show/hide mechanism is new.

     A real, native <details>/<summary>, not a JS-driven toggle: this
     needs no click handler, is keyboard-operable (Enter/Space on a
     focused <summary>) and screen-reader-operable for free, and degrades
     to "just show the pre" if CSS ever failed to load. `group`/
     `group-open:` is Tailwind's own built-in support for exactly this
     element - no custom JS state to keep in sync with it.

     `code` is optional: a label-plus-render pairing with nothing to copy
     (a narrow/mobile reflow demo reusing an already-shown call, say)
     simply omits the disclosure rather than repeating identical source a
     reader already saw once above. --%>
!{boolean hasLabel = !label.isEmpty();}
!{boolean hasCode = !code.isEmpty();}

<div>
    @if (hasLabel)
        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">${label}</p>
    @endif

    <div class="mt-3">
        ${content}
    </div>

    @if (hasCode)
        <details class="group mt-3">
            <summary class="inline-flex cursor-pointer list-none items-center gap-1 text-xs font-medium text-slate-500 hover:text-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 dark:text-slate-400 dark:hover:text-blue-400 [&::-webkit-details-marker]:hidden">
                <span class="inline-flex transition-transform duration-150 ease-out group-open:rotate-90 motion-reduce:transition-none">
                    @template.ashlar.components.icon(icon = Icon.CHEVRON_RIGHT, classes = "h-3.5 w-3.5")
                </span>
                View source
            </summary>
            <pre class="mt-2 overflow-x-auto rounded bg-slate-900 p-3 text-xs text-slate-100 dark:bg-slate-950"><code>${code}</code></pre>
        </details>
    @endif
</div>
