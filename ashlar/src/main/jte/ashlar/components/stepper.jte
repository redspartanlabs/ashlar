@param gg.jte.Content items

<%-- The wrapper only establishes the ordered-list landmark and the CSS
     counter each item numbers itself from - each step is a separate
     @template.ashlar.components.stepper-item(...) call composed into `items`, the
     same wrapper/item split as accordion.jte and breadcrumbs.jte. Unlike
     either of those, Stepper is non-interactive by design (see
     stepper-item.jte's own comment on why), so there is no JS file here at
     all - nothing ever changes after the server renders this once.

     No `role`/`aria-label`: an `<ol>` already communicates "an ordered
     sequence" on its own, and there's no equivalent of breadcrumbs.jte's
     "Breadcrumb" landmark name for a step sequence to need - a caller
     placing this under its own heading (see the showcase page) already
     gives it context the way any other section of a page does.

     `[counter-reset:step]` starts the CSS counter each stepper-item.jte
     increments and displays via `content: counter(step)` - this numbers
     steps automatically in document order without any Java/JS coordination
     (unlike accordion.jte's AccordionExpansion, which exists because
     "which one wins" genuinely needs a decision; "what position is this
     item at" doesn't - a plain running count is exactly what CSS counters
     were designed for).

     `@container` plus `@sm:` (not a plain `sm:` viewport breakpoint) on the
     horizontal/vertical switch below and in stepper-item.jte: a Stepper
     embedded in a narrow sidebar on an otherwise wide desktop window needs
     to stack just as much as one on an actually narrow phone does - a
     viewport media query only ever sees the latter. Querying this
     element's own rendered width instead of the window's makes the
     reflow correct regardless of where the caller places it.

     The `@container` marker sits on this wrapping `<div>`, one level above
     the `<ol>` it measures - a container can't answer its own query (the
     containment CSS establishes would make "how wide am I" depend on "what
     did I just decide to look like," which the spec doesn't allow), so the
     element being measured and the element reacting to that measurement
     have to be two different elements. --%>

<div class="@container">
    <ol data-stepper class="flex flex-col gap-6 @sm:flex-row @sm:gap-0 [counter-reset:step]">
        ${items}
    </ol>
</div>
