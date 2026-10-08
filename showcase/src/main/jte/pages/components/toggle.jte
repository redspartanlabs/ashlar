@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- No Composition section - Toggle has no distinctive composition
     pattern beyond ordinary form use. --%>
!{String quickStartCode = """
    @template.ashlar.components.toggle(
        name = "autoSave",
        label = "Auto-save changes"
    )""";}
!{String disabledCode = """
    @template.ashlar.components.toggle(
        name = "disabledOff",
        label = "Not available on your plan",
        disabled = true
    )
    @template.ashlar.components.toggle(
        name = "disabledOn",
        label = "Always enabled for admins",
        checked = true,
        disabled = true
    )""";}
!{String labelPositionCode = """
    @template.ashlar.components.toggle(
        name = "labelEnd",
        label = "Switch, then label (default)"
    )
    @template.ashlar.components.toggle(
        name = "labelStart",
        label = "Label, then switch",
        labelPosition = "start"
    )""";}
!{String errorCode = """
    @template.ashlar.components.toggle(
        name = "flaggedAcknowledge",
        label = "I acknowledge the maintenance window",
        error = true,
        errorMessage = "You must acknowledge the maintenance window to continue."
    )""";}
!{String requiredCode = """
    <form action="" method="get">
        @template.ashlar.components.toggle(
            name = "acceptTermsToggle",
            label = "I accept the terms and conditions",
            required = true
        )
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
        @template.ashlar.components.button(text = "Reset", type = "Ghost", buttonType = "reset")
    </form>""";}
!{List<ApiParam> toggleApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered - part of the one <label> that wraps the whole switch."),
    ApiParam.optional("value", "String", "\"on\"", "The value submitted when checked - the HTML default, rarely overridden."),
    ApiParam.optional("checked", "boolean", "false", "Initial on/off state."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the control. Not focusable or submitted."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation - the switch must be on to submit."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the label."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true."),
    ApiParam.optional("labelPosition", "String", "\"end\"", "\"end\" (switch, then label) or \"start\" (label, then switch).")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Toggle / Switch",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "toggle", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;input type="checkbox"&gt; underneath - visually hidden, styled entirely via CSS <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">peer-*</code> variants. Zero JavaScript; toggling, focus, and form behavior are all native.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.toggle(name = "quickStartAutoSave", label = "Auto-save changes")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Toggle for one setting with an immediate, binary effect - the convention a track-and-thumb switch communicates to a visitor. For a choice inside a form that's submitted together with other fields, plain <a href="${contextPath}/components/checkbox" class="text-blue-700 hover:underline dark:text-blue-400">Checkbox</a> communicates that better, even though both render a real checkbox underneath. For more than two states, neither applies - use <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> or <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a>.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic &amp; checked</p>
                        <div class="mt-3">
                            @template.ashlar.components.toggle(
                            name = "marketingEmailsToggle",
                            label = "Send me marketing emails"
                            )
                            @template.ashlar.components.toggle(
                            name = "autoSave",
                            label = "Auto-save changes",
                            checked = true
                            )
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Label + description</p>
                        <div class="mt-3">
                            @template.ashlar.components.toggle(
                            name = "notificationsToggle",
                            label = "Enable notifications",
                            helpText = "Receive alerts when important events occur.",
                            checked = true
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "Disabled (off & on)",
                    code = disabledCode,
                    content = @`
                        <div class="flex flex-col gap-2">
                            @template.ashlar.components.toggle(
                            name = "disabledOff",
                            label = "Not available on your plan",
                            disabled = true
                            )
                            @template.ashlar.components.toggle(
                            name = "disabledOn",
                            label = "Always enabled for admins",
                            checked = true,
                            disabled = true
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Label position</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Same switch, label text on either side.</p>
                        @template.layout.example(
                        code = labelPositionCode,
                        content = @`
                            <div class="mt-3 flex flex-col gap-4">
                                @template.ashlar.components.toggle(
                                name = "labelEnd",
                                label = "Switch, then label (default)"
                                )
                                @template.ashlar.components.toggle(
                                name = "labelStart",
                                label = "Label, then switch",
                                labelPosition = "start"
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple independent switches</p>
                        <div class="mt-3 flex flex-col gap-3">
                            @template.ashlar.components.toggle(name = "enableNotifications", label = "Enable notifications", checked = true)
                            @template.ashlar.components.toggle(name = "autoSaveChanges", label = "Auto-save changes", checked = true)
                            @template.ashlar.components.toggle(name = "enableTelemetry", label = "Enable telemetry")
                            @template.ashlar.components.toggle(name = "maintenanceMode", label = "Maintenance mode")
                        </div>
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.toggle(
                                name = "flaggedAcknowledge",
                                label = "I acknowledge the maintenance window",
                                error = true,
                                errorMessage = "You must acknowledge the maintenance window to continue."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required, keyboard/Tab order &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tab to the switch (the disabled one above is skipped entirely), toggle with <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Space</kbd>, then submit off to see the browser's own validation UI on the required switch. Reset restores the original checked state.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                @template.ashlar.components.toggle(
                                name = "acceptTermsToggle",
                                label = "I accept the terms and conditions",
                                required = true
                                )
                                <div class="flex gap-3">
                                    @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                                    @template.ashlar.components.button(text = "Reset", type = "Ghost", buttonType = "reset")
                                </div>
                            </form>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="best-practices" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Best Practices",
            content = @`
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Phrase the label as the setting's name ("Auto-save changes"), not as a question or instruction.</li>
                            <li>Pick <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">labelPosition = "start"</code> when the switch sits at the right edge of a settings row - the common list-item layout.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Using Toggle for one choice among several submitted together as a set - that reads as Checkbox's job to a visitor, even though the markup is nearly identical.</li>
                            <li>Expecting a three-state switch - there's no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">indeterminate</code> here; see Pitfalls.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="pitfalls" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Pitfalls & Troubleshooting",
            content = @`
                <ul class="list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">No <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">indeterminate</code> param.</strong> Unlike Checkbox, Toggle has no third visual state - a switch represents a definite on/off by design. Use Checkbox if "some, but not all" is a real state your UI needs to show.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Toggle doesn't save anything by itself.</strong> It's a real checkbox that still needs a surrounding form and a submit action (or your own change-handling) - nothing here auto-saves on toggle.</li>
                </ul>
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
                            <li>A real, focusable checkbox kept in the accessibility tree (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">sr-only</code>, not <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">display: none</code>) - native Tab, Space, and label-click all operate on it directly.</li>
                            <li>Track and thumb are purely decorative (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-hidden</code>), driven by CSS reading the real input's state - zero JavaScript.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> naming the setting, read aloud the same as any checkbox's.</li>
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = toggleApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/checkbox" class="text-blue-700 hover:underline dark:text-blue-400">Checkbox</a> for one choice submitted as part of a set &middot;
                    <a href="${contextPath}/components/radio-group" class="text-blue-700 hover:underline dark:text-blue-400">Radio Group</a> for more than two states
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "toggle", contextPath = contextPath)
    </div>
`
)
