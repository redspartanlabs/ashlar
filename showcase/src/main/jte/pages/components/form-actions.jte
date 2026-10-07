@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Batch 12 (final) documentation page. Existing "Two actions" / "Three
     actions" / "Single action" / "Long labels / wrapping" / "In context -
     a Page Section with a form" examples kept verbatim. Added: Quick
     Start, formal grammar headings, How to Use, Composition,
     Accessibility, API Reference, Related Components.

     Verified against source before writing:
     - form-actions.jte's own single @param (content) and doc comment.
       Confirmed this is layout-only - it never creates a button, never
       inspects what it was given, and never reorders it: content renders
       in exactly the DOM order the caller supplied, since action order
       can itself carry meaning this utility has no business changing.
     - Confirmed a plain <div>, not <form>/<fieldset> - it doesn't own
       submission or field grouping, only visual arrangement.
     - Confirmed no role="group" - there is no concrete accessibility
       requirement this satisfies that plain DOM order and each child's
       own native semantics don't already cover. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.utilities.form-actions(\n"
        + "    content = @" + backtick + "\n"
        + "        @template.ashlar.components.button(text = \"Cancel\", type = \"Secondary\")\n"
        + "        @template.ashlar.components.button(text = \"Save\", type = \"Primary\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> formActionsApi = List.of(
    ApiParam.required("content", "gg.jte.Content", "The action controls, rendered in exactly the order supplied - never reordered, inspected, or defaulted to anything.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Form Actions",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "form-actions", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for arranging a form's own action controls - end-aligned spacing that wraps at narrow widths. No surface, no JavaScript, no opinion about what the actions are or do.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.form-actions(
                    content = @`
                        @template.ashlar.components.button(text = "Cancel", type = "Secondary")
                        @template.ashlar.components.button(text = "Save", type = "Primary")
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Form Actions specifically for a form's own end-aligned button row, most often composed into a <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>'s <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> slot. For a row of mixed filter controls instead of a form's own buttons, that's <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a>'s job, not this one's.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Two actions</p>
                        <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.form-actions(
                            content = @`
                                @template.ashlar.components.button(text = "Cancel", type = "Secondary")
                                @template.ashlar.components.button(text = "Save Changes", type = "Primary")
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Three actions</p>
                        <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.form-actions(
                            content = @`
                                @template.ashlar.components.button(text = "Back", type = "Ghost")
                                @template.ashlar.components.button(text = "Save Draft", type = "Secondary")
                                @template.ashlar.components.button(text = "Publish", type = "Primary")
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Single action</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No special-casing - a single child stays end-aligned like any other.</p>
                        <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.form-actions(
                            content = @`
                                @template.ashlar.components.button(text = "Done", type = "Primary")
                            `
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long labels / wrapping</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">At narrow widths, actions wrap onto their own line instead of overflowing.</p>
                        <div class="mt-3 max-w-xs rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                            @template.ashlar.utilities.form-actions(
                            content = @`
                                @template.ashlar.components.button(text = "Discard changes and go back", type = "Secondary")
                                @template.ashlar.components.button(text = "Save and continue editing", type = "Primary")
                            `
                            )
                        </div>
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">In context, composed into a <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>'s own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> slot beneath a form's fields, inside a <a href="${contextPath}/components/page-section" class="text-blue-700 hover:underline dark:text-blue-400">Page Section</a>. Form Actions owns only its own spacing/alignment - it doesn't disturb Page Section's own heading-to-content rhythm around it.</p>
                <div class="mt-3 rounded-lg border border-dashed border-slate-200 p-4 dark:border-slate-700">
                    @template.ashlar.utilities.page-section(
                    title = "Account Information",
                    description = "Manage the account details for this user.",
                    content = @`
                        @template.ashlar.components.card(
                        body = @`
                            <div class="flex flex-col gap-4">
                                @template.ashlar.components.text-input(name = "fullName", label = "Full name", value = "Dana Whitfield")
                                @template.ashlar.components.text-input(name = "email", label = "Email", value = "dana@acme.example")
                            </div>
                        `,
                        footer = @`
                            @template.ashlar.utilities.form-actions(
                                content = @`
                                    @template.ashlar.components.button(text = "Cancel", type = "Secondary")
                                    @template.ashlar.components.button(text = "Save Changes", type = "Primary")
                                `
                            )
                        `
                        )
                    `
                    )
                </div>
            `
            )
        </section>

        <section id="accessibility" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Accessibility",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>DOM order that always matches what you composed - nothing here reorders your actions, which matters since their order can itself be meaningful (e.g. "Cancel" before "Save").</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Each action's own accessible name and behavior - this utility supplies no semantics of its own beyond plain layout.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "API Reference",
            content = @`
                @template.layout.api-reference(apiParams = formActionsApi)
            `
            )
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/filter-bar" class="text-blue-700 hover:underline dark:text-blue-400">Filter Bar</a> for a row of mixed filter controls instead of a form's own buttons &middot;
                    <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, the usual host &middot;
                    <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, the usual content
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "form-actions", contextPath = contextPath)
    </div>
`
)
