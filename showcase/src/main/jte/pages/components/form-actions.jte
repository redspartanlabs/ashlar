@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Form Actions",
content = @`
    @template.layout.component-nav(currentSlug = "form-actions", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Form Actions</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A layout utility for arranging a form's own action controls - end-aligned spacing that wraps at narrow widths. No surface, no JavaScript, no opinion about what the actions are or do.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Two actions</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Three actions</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Single action</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Long labels / wrapping</h3>
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

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">In context - a Page Section with a form</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Form Actions owns only its own spacing/alignment - it doesn't disturb Page Section's own heading-to-content rhythm around it.</p>
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
                </div>
            </div>
        </section>
`
)
