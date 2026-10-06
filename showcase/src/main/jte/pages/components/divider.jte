@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 5 documentation page. Existing "Horizontal, between content
     sections" / "Vertical, inside a toolbar" examples kept verbatim,
     moved under Variants & States. Added: Quick Start, formal grammar
     headings, Accessibility, API Reference, Related Components.

     DELIBERATE SLIMMER TREATMENT, per this batch's explicit instruction:
     no How to Use, Composition, Best Practices, Common Patterns, or
     Pitfalls section. This is a one-param visual separator with no real
     behavior to document beyond what Overview/Variants & States/
     Accessibility already say - adding those headings here would be
     exactly the checklist-filling this project has avoided since Batch 3.

     Verified against source before writing:
     - divider.jte's own single @param and its own doc comment. Confirmed
       Horizontal renders a real native <hr> (implicit separator role,
       default orientation per the HTML-AAM, no ARIA added); Vertical has
       no native element to reach for - <hr> is a paragraph-level break in
       the flow of content, so forcing it vertical via CSS would
       misrepresent that semantics even though it could be made to look
       right - so Vertical uses the WAI-ARIA APG's own toolbar-separator
       pattern instead: <div role="separator" aria-orientation="vertical">.
     - DividerOrientation.java confirmed (not shown in full here since the
       single value pair is already stated completely by the param
       description): falls back to Horizontal on an unrecognized string,
       the same silent-fallback shape every other enum-backed param in
       this library uses. --%>
!{String quickStartCode = """
    @template.ashlar.components.divider()""";}
!{List<ApiParam> dividerApi = List.of(
    ApiParam.optional("orientation", "String", "\"Horizontal\"", "Horizontal or Vertical. Unrecognized values silently fall back to Horizontal. Horizontal renders a real <hr>; Vertical renders role=\"separator\" aria-orientation=\"vertical\" - there is no native vertical <hr>.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Divider",
content = @`
    @template.layout.component-nav(currentSlug = "divider", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Divider</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A visual separator - a real &lt;hr&gt; horizontally, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="separator"</code> vertically. The surrounding layout owns all spacing; the divider only draws the line.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.divider()
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Horizontal, between content sections</p>
                            <div class="mt-3 max-w-md">
                                <div>
                                    <p class="text-sm font-medium text-slate-900 dark:text-slate-100">Account details</p>
                                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Acme Manufacturing - ACC-1001</p>
                                </div>
                                <div class="my-4">
                                    @template.ashlar.components.divider()
                                </div>
                                <div>
                                    <p class="text-sm font-medium text-slate-900 dark:text-slate-100">Billing contact</p>
                                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Dana Whitfield - dana.whitfield@acme.example</p>
                                </div>
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Vertical, inside a toolbar</p>
                            <div class="mt-3 flex h-10 items-center gap-3 rounded-md border border-slate-200 bg-slate-50 px-3 dark:border-slate-700 dark:bg-slate-800/50">
                                @template.ashlar.components.button(text = "Save", type = "Ghost", size = "Small", icon = Icon.SAVE, iconOnly = true)
                                @template.ashlar.components.button(text = "Edit", type = "Ghost", size = "Small", icon = Icon.EDIT, iconOnly = true)
                                @template.ashlar.components.divider(orientation = "Vertical")
                                @template.ashlar.components.button(text = "Delete", type = "Ghost", size = "Small", icon = Icon.DELETE, iconOnly = true)
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real native &lt;hr&gt; for Horizontal - no ARIA needed, since its separator role and orientation are already implicit.</li>
                                <li>The WAI-ARIA APG's own toolbar-separator pattern for Vertical, since no native element expresses that orientation.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>All spacing around it - margin, gap, padding - the divider only draws the line itself.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = dividerApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a common surface to use a horizontal Divider inside &middot;
                        <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, the usual neighbor for a vertical Divider in a toolbar
                    </p>
                </div>
            </div>
        </section>
`
)
