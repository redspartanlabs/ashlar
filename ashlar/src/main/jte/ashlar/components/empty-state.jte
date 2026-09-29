@import com.redspartanlabs.ashlar.icon.Icon

@param String title
@param gg.jte.Content body = null
@param Icon icon = null
@param gg.jte.Content action = null

<%-- A semantic "nothing to show" message, not a loading indicator - the
     content HAS loaded, and there is currently none of it. Distinct from
     skeleton.jte (shape of not-yet-loaded content) the same way progress.jte
     is distinct from spinner.jte: different truth about the world, not a
     different visual treatment of the same one.

     `title` is required, unlike card.jte's and alert.jte's own optional
     title: this component's entire reason to exist IS its message, so a
     titleless instance would be a contradiction, not a valid minimal use -
     the same reasoning badge.jte's required `text` already applies.

     `title` renders as an <h3>, matching card.jte's and alert.jte's own
     choice for the identical reason: this is exactly the kind of component
     that commonly nests inside a Card (see the showcase page) or a Data
     Table's empty region rather than standing directly under a page's own
     h1/h2, so it takes the same one-level-down heading its siblings do.

     `action` is `gg.jte.Content`, not an `actionText`/`actionUrl`/
     `actionHandler` trio: this component has no idea what "an action" is,
     and shouldn't - the caller composes a real @template.ashlar.components.button
     (or a link, or several) into it, exactly as card.jte's own `footer`
     lets a caller compose real Buttons rather than Card inventing its own
     button API.

     The icon, when present, is decorative (icon.jte's own default - no
     `label` is passed here) because the title is already real text that
     says the same thing; an icon-only accessible name here would be
     redundant with, not additional to, the heading right below it. --%>
<div class="flex flex-col items-center px-6 py-12 text-center" data-empty-state>
    @if (icon != null)
        <div class="mb-3 text-slate-400 dark:text-slate-500">
            @template.ashlar.components.icon(icon = icon, classes = "h-10 w-10")
        </div>
    @endif

    <h3 class="text-base font-semibold text-slate-900 dark:text-slate-100">${title}</h3>

    @if (body != null)
        <div class="mt-1 max-w-sm text-sm text-slate-500 dark:text-slate-400">
            ${body}
        </div>
    @endif

    @if (action != null)
        <div class="mt-4 flex flex-wrap items-center justify-center gap-3">
            ${action}
        </div>
    @endif
</div>
