@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

<%-- Deliberate omission: no Variants & States section. Form Field has no
     visual variants or states of its own - it is pure label/help/error/
     spacing scaffolding around whatever control the caller provides, and
     every example below varies the control being wired in, not Form
     Field itself, so they live under Composition instead. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.form-field(\n"
        + "    fieldId = \"brand-colour\",\n"
        + "    label = \"Brand colour\",\n"
        + "    control = @" + backtick + "<input type=\"color\" id=\"brand-colour\" value=\"#2563eb\">" + backtick + "\n"
        + ")";}
!{String withHelpCode = "@template.ashlar.components.form-field(\n"
        + "    fieldId = \"timezone\",\n"
        + "    label = \"Reporting timezone\",\n"
        + "    helpText = \"Scheduled exports use this timezone.\",\n"
        + "    control = @" + backtick + "\n"
        + "        <select id=\"timezone\" aria-describedby=\"timezone-help\" class=\"block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100\">\n"
        + "            <option>UTC</option>\n"
        + "            <option>Europe/London</option>\n"
        + "            <option>America/Denver</option>\n"
        + "        </select>\n"
        + "    " + backtick + "\n"
        + ")";}
!{String requiredCode = "@template.ashlar.components.form-field(\n"
        + "    fieldId = \"invoice-ref\",\n"
        + "    label = \"Invoice reference\",\n"
        + "    required = true,\n"
        + "    helpText = \"Printed at the top of every invoice.\",\n"
        + "    control = @" + backtick + "<input type=\"text\" id=\"invoice-ref\" required aria-describedby=\"invoice-ref-help\" class=\"block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100\">" + backtick + "\n"
        + ")";}
!{String validationErrorCode = "@template.ashlar.components.form-field(\n"
        + "    fieldId = \"seat-count\",\n"
        + "    label = \"Seats\",\n"
        + "    required = true,\n"
        + "    helpText = \"Between 1 and 500.\",\n"
        + "    errorMessage = \"Enter a number between 1 and 500.\",\n"
        + "    control = @" + backtick + "<input type=\"number\" id=\"seat-count\" value=\"0\" required aria-invalid=\"true\" aria-describedby=\"seat-count-help seat-count-error\" class=\"block w-full max-w-sm rounded-md border border-red-500 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 dark:border-red-500 dark:bg-slate-800 dark:text-slate-100\">" + backtick + "\n"
        + ")";}
!{String composedControlCode = "@template.ashlar.components.form-field(\n"
        + "    fieldId = \"card-expiry-month\",\n"
        + "    label = \"Card expiry\",\n"
        + "    helpText = \"Month and year as printed on the card.\",\n"
        + "    control = @" + backtick + "\n"
        + "        <div class=\"flex max-w-sm items-center gap-2\">\n"
        + "            <input type=\"text\" id=\"card-expiry-month\" inputmode=\"numeric\" placeholder=\"MM\" maxlength=\"2\" aria-label=\"Expiry month\" aria-describedby=\"card-expiry-month-help\" class=\"w-16 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100\">\n"
        + "            <span class=\"text-slate-400\" aria-hidden=\"true\">/</span>\n"
        + "            <input type=\"text\" id=\"card-expiry-year\" inputmode=\"numeric\" placeholder=\"YY\" maxlength=\"2\" aria-label=\"Expiry year\" aria-describedby=\"card-expiry-month-help\" class=\"w-16 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100\">\n"
        + "        </div>\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> formFieldApi = List.of(
    ApiParam.required("fieldId", "String", "The id of the control rendered inside control. Joins the <label> and the help/error paragraphs to it - never generated, since only the caller can also put it on their own markup."),
    ApiParam.required("label", "String", "The field's visible label text."),
    ApiParam.required("control", "gg.jte.Content", "The already-rendered control markup. Form Field cannot add attributes to it - see the wiring contract below."),
    ApiParam.optional("helpText", "String", "\"\"", "Rendered at {fieldId}-help. Stays visible alongside an error rather than being replaced by one."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" at {fieldId}-error."),
    ApiParam.optional("required", "boolean", "false", "Shows a decorative, aria-hidden asterisk only. The real required semantics must still be set on the control itself.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}

@template.layout.page(
contextPath = contextPath,
title = "Form Field",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "form-field", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Label, help text, error text and required marker around a control this library doesn't already wrap - a native element, or something bespoke.</p>

    <div class="mt-4 rounded-md border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-700/60 dark:bg-amber-950/40 dark:text-amber-200">
        <p class="font-medium">Don't wrap the library's own form controls in this.</p>
        <p class="mt-1">Text Input, Textarea, Select, Checkbox, Radio Group, Toggle, Number Input, Combobox, Date Picker, Date Range Picker, File Upload and Search Input already render their own label, help text, error text and field spacing. Wrapping one here gives you two labels and two copies of the help text. Use their own <code class="rounded bg-amber-100 px-1 py-0.5 text-xs dark:bg-amber-900/50">label</code> / <code class="rounded bg-amber-100 px-1 py-0.5 text-xs dark:bg-amber-900/50">helpText</code> parameters instead.</p>
    </div>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3">
                    @template.ashlar.components.form-field(
                    fieldId = "quickStartColour",
                    label = "Brand colour",
                    control = @`<input type="color" id="quickStartColour" value="#2563eb" class="h-9 w-16 cursor-pointer rounded-md border border-slate-300 bg-white p-1 dark:border-slate-600 dark:bg-slate-800">`
                    )
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Form Field only for a control this library doesn't already give its own label/help/error - a native &lt;select&gt;, an &lt;input type="color"&gt;, a bespoke multi-input control like a card expiry field. If what you're wiring up is Text Input, Select, Checkbox, or any of the other eleven controls named above, use that component's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">helpText</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> params instead - never both.</p>
                <div class="mt-4 rounded-md border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700 dark:border-slate-700 dark:bg-slate-800/50 dark:text-slate-300">
                    <p class="font-medium text-slate-900 dark:text-slate-100">The wiring contract</p>
                    <p class="mt-1">Form Field names the control and renders the supporting text with predictable ids. It cannot reach into your control to add attributes, so you add these yourself:</p>
                    <pre class="mt-2 overflow-x-auto rounded bg-slate-900 p-3 text-xs text-slate-100 dark:bg-slate-950"><code>id="{fieldId}"
aria-describedby="{fieldId}-help {fieldId}-error"
aria-invalid="true"   <%-- when an error is showing --%>
required              <%-- when the field is required --%></code></pre>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">This is what Form Field is actually for: the same scaffolding wired onto a different real control each time, with the caller applying the wiring contract above to whatever markup they pass as <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">control</code>.</p>
                <div class="mt-4 flex flex-col gap-8">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                        <div class="mt-3">
                            @template.ashlar.components.form-field(
                            fieldId = "brand-colour",
                            label = "Brand colour",
                            control = @`<input type="color" id="brand-colour" value="#2563eb" class="h-9 w-16 cursor-pointer rounded-md border border-slate-300 bg-white p-1 dark:border-slate-600 dark:bg-slate-800">`
                            )
                        </div>
                    </div>

                    @template.layout.example(
                    label = "With help text",
                    code = withHelpCode,
                    content = @`
                        <div>
                            @template.ashlar.components.form-field(
                            fieldId = "timezone",
                            label = "Reporting timezone",
                            helpText = "Scheduled exports use this timezone.",
                            control = @`
                                <select id="timezone" aria-describedby="timezone-help" class="block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
                                    <option>UTC</option>
                                    <option>Europe/London</option>
                                    <option>America/Denver</option>
                                </select>
                            `
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The asterisk is decorative; the semantics come from <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> on the control itself.</p>
                        @template.layout.example(
                        code = requiredCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.form-field(
                                fieldId = "invoice-ref",
                                label = "Invoice reference",
                                required = true,
                                helpText = "Printed at the top of every invoice.",
                                control = @`<input type="text" id="invoice-ref" required aria-describedby="invoice-ref-help" class="block w-full max-w-sm rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">`
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Validation error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Help text stays visible alongside the error - it's usually what explains how to fix it - so the control lists both ids.</p>
                        @template.layout.example(
                        code = validationErrorCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.form-field(
                                fieldId = "seat-count",
                                label = "Seats",
                                required = true,
                                helpText = "Between 1 and 500.",
                                errorMessage = "Enter a number between 1 and 500.",
                                control = @`<input type="number" id="seat-count" value="0" required aria-invalid="true" aria-describedby="seat-count-help seat-count-error" class="block w-full max-w-sm rounded-md border border-red-500 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 dark:border-red-500 dark:bg-slate-800 dark:text-slate-100">`
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">A composed control</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">One field, several inputs. The label points at the first one.</p>
                        @template.layout.example(
                        code = composedControlCode,
                        content = @`
                            <div class="mt-3">
                                @template.ashlar.components.form-field(
                                fieldId = "card-expiry-month",
                                label = "Card expiry",
                                helpText = "Month and year as printed on the card.",
                                control = @`
                                    <div class="flex max-w-sm items-center gap-2">
                                        <input type="text" id="card-expiry-month" inputmode="numeric" placeholder="MM" maxlength="2" aria-label="Expiry month" aria-describedby="card-expiry-month-help" class="w-16 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
                                        <span class="text-slate-400" aria-hidden="true">/</span>
                                        <input type="text" id="card-expiry-year" inputmode="numeric" placeholder="YY" maxlength="2" aria-label="Expiry year" aria-describedby="card-expiry-month-help" class="w-16 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:border-slate-600 dark:bg-slate-800 dark:text-slate-100">
                                    </div>
                                `
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long label and long messages</p>
                        <div class="mt-3 max-w-sm">
                            @template.ashlar.components.form-field(
                            fieldId = "retention",
                            label = "Default retention period applied to newly created records",
                            required = true,
                            helpText = "Records older than this are moved to the archive automatically, and archived records stop counting toward your plan's storage limit once the trash is emptied.",
                            errorMessage = "Choose a retention period of at least 30 days - shorter periods conflict with the compliance policy on this account.",
                            control = @`<input type="text" id="retention" value="7 days" required aria-invalid="true" aria-describedby="retention-help retention-error" class="block w-full rounded-md border border-red-500 bg-white px-3 py-2 text-sm text-slate-900 shadow-sm focus:border-red-500 focus:outline-none focus:ring-2 focus:ring-red-500 dark:border-red-500 dark:bg-slate-800 dark:text-slate-100">`
                            )
                        </div>
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
                            <li>Apply every line of the wiring contract above whenever it's relevant - a missing <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-describedby</code> leaves help/error text visible but unannounced.</li>
                            <li>Point <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">fieldId</code> and the control's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">id</code> at the exact same string - the &lt;label&gt; only works if they match.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Wrapping any of this library's own 12 self-contained controls in Form Field - see the warning above.</li>
                            <li>Expecting the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> param to do anything functional - it only draws an asterisk.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Setting <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required = true</code> alone does nothing functional.</strong> It only shows the decorative asterisk - the control itself still needs its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> attribute for any actual constraint validation to happen.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Form Field can't add attributes to your control.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">control</code> arrives as already-rendered markup - there's no template-level way to reach in afterward, which is exactly why the wiring contract exists as something you apply yourself rather than something this component could do for you.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">Wrapping one of the library's own inputs doubles everything.</strong> Two labels, two copies of help text, doubled vertical spacing - not a rendering bug, just two field-scaffolding systems stacked on top of each other.</li>
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
                            <li>A real &lt;label for&gt; pointing at <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">fieldId</code>.</li>
                            <li>Predictable, stable ids for the help and error paragraphs (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">{fieldId}-help</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">{fieldId}-error</code>) so they're always referenceable even though this component can't reference them for you.</li>
                            <li>An error paragraph with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>The matching <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">id</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-describedby</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-invalid</code>, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">required</code> on the control itself - the entire wiring contract above.</li>
                            <li>For a composed control (multiple inputs), your own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-label</code> on every input past the first, since the &lt;label&gt; can only point at one of them.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = formFieldApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/text-input" class="text-blue-700 hover:underline dark:text-blue-400">Text Input</a> and the other eleven self-contained controls that should never be wrapped in this &middot;
                    <a href="${contextPath}/components/select" class="text-blue-700 hover:underline dark:text-blue-400">Select</a> for the one case this page itself uses a native alternative to
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "form-field", contextPath = contextPath)
    </div>
`
)
