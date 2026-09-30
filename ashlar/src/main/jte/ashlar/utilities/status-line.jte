@param gg.jte.Content content

<%-- A single inline status/informational message - "Last updated...",
     "Showing 24 of 128 records", "No results found" - not a replacement for
     alert.jte or badge.jte, which already own persistent contextual
     messaging and small status indicators respectively. Status Line owns
     only typography and spacing for a line of text that happens to be
     informational, nothing else.

     A plain `<p>`, not a `<div>` wrapper: every example this utility exists
     for is genuinely one line of prose (plain text, a link, an inline
     Badge), and `<p>` says exactly that with no extra element. A caller
     needing more structure than a single line of phrasing content is
     describing something other than a status line.

     No `role="status"`/`aria-live`: matching alert.jte's own reasoning,
     those are live-region semantics for content that changes and needs to
     announce itself - this is server-rendered, already part of the page on
     first paint, and has no JavaScript updating it after the fact. Adding a
     live-region role to already-static content doesn't make it more
     accessible; it risks an unwanted announcement on page load for content
     that was never "live" to begin with. A caller who dynamically swaps
     this in via their own JavaScript after page load is introducing the
     behavior that would need the role, and should add it themselves at
     that point - this utility can't know in advance whether that applies.

     `content` is `gg.jte.Content`, not `String`, so a message can carry a
     link or a small inline component (a Badge, say) without this template
     ever needing to understand what it is - the same reasoning
     form-actions.jte and key-value.jte already give for their own slots.

     Muted `text-sm text-slate-500 dark:text-slate-400` matches
     page-header.jte's and page-section.jte's own description styling
     exactly, keeping a status line visually subordinate to whatever primary
     content it sits near rather than competing with it. --%>
<p class="text-sm text-slate-500 dark:text-slate-400">${content}</p>
