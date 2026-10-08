@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import dev.redspartanlabs.ashlar.showcase.catalog.PageAnchor
@import java.util.List

@param String contextPath

!{String quickStartCode = """
    @template.ashlar.components.text-input(
        name = "fullName",
        label = "Full name",
        autocomplete = "name"
    )""";}
!{List<ApiParam> textInputApi = List.of(
    ApiParam.required("name", "String", "The field name values submit under."),
    ApiParam.required("label", "String", "Always rendered and associated with the control via a real <label for>."),
    ApiParam.optional("type", "String", "\"text\"", "Any native <input> type - \"email\", \"password\", \"tel\", \"url\", \"search\"..."),
    ApiParam.optional("value", "String", "\"\"", "Initial value."),
    ApiParam.optional("placeholder", "String", "\"\"", "Shown only while the field is empty."),
    ApiParam.optional("required", "boolean", "false", "Enables native constraint validation."),
    ApiParam.optional("disabled", "boolean", "false", "Disables the control. Not focusable or submitted."),
    ApiParam.optional("readonly", "boolean", "false", "Visible and focusable, but not editable. Still submitted."),
    ApiParam.optional("id", "String", "\"\"", "Defaults to name when omitted."),
    ApiParam.optional("helpText", "String", "\"\"", "Supporting text below the field."),
    ApiParam.optional("error", "boolean", "false", "Shows errorMessage and applies error styling."),
    ApiParam.optional("errorMessage", "String", "\"\"", "Rendered with role=\"alert\" when error is true."),
    ApiParam.optional("minlength", "Integer", "null", "Native minlength constraint."),
    ApiParam.optional("maxlength", "Integer", "null", "Native maxlength constraint."),
    ApiParam.optional("pattern", "String", "\"\"", "Native pattern constraint - a regular expression the finished value must match."),
    ApiParam.optional("autocomplete", "String", "\"\"", "Native autocomplete token, e.g. \"email\", \"given-name\", \"new-password\"."),
    ApiParam.optional("inputmode", "String", "\"\"", "Hints the on-screen keyboard shown on touch devices, e.g. \"numeric\"."),
    ApiParam.optional("mask", "String", "\"\"", "Only \"phone\" exists today - live US phone formatting; see Pitfalls."),
    ApiParam.optional("leadingIcon", "Icon", "null", "Decorative icon before the text."),
    ApiParam.optional("trailingIcon", "Icon", "null", "Decorative icon after the text.")
);}
!{List<PageAnchor> sections = List.of(
    new PageAnchor("quick-start", "Quick Start"),
    new PageAnchor("how-to-use", "How to Use"),
    new PageAnchor("variants", "Variants & States"),
    new PageAnchor("composition", "Composition"),
    new PageAnchor("best-practices", "Best Practices"),
    new PageAnchor("pitfalls", "Pitfalls"),
    new PageAnchor("accessibility", "Accessibility"),
    new PageAnchor("api-reference", "API Reference"),
    new PageAnchor("related", "Related Components")
);}
!{String basicStatesCode = """
    @template.ashlar.components.text-input(
        name = "fullName",
        label = "First Name"
    )
    @template.ashlar.components.text-input(
        name = "emailPlaceholder",
        label = "Email Address",
        type = "email",
        placeholder = "you@example.com"
    )
    @template.ashlar.components.text-input(
        name = "company",
        label = "Company",
        value = "Acme Corporation"
    )
    @template.ashlar.components.text-input(
        name = "disabledField",
        label = "Disabled",
        value = "Can't be edited",
        disabled = true
    )
    @template.ashlar.components.text-input(
        name = "readonlyField",
        label = "Readonly",
        value = "ACC-2026-00417",
        readonly = true,
        helpText = "Assigned automatically and can't be changed."
    )""";}
!{String phoneMaskingCode = """
    @template.ashlar.components.text-input(
        name = "phone",
        label = "Phone Number",
        type = "tel",
        mask = "phone",
        placeholder = "(505) 555-0100",
        autocomplete = "tel",
        pattern = "\\\\(\\\\d{3}\\\\) \\\\d{3}-\\\\d{4}",
        helpText = "U.S. phone number, formatted automatically as you type."
    )""";}
!{String realWorldCode = """
    @template.ashlar.components.text-input(
        name = "username",
        label = "Username",
        placeholder = "e.g. jane_doe",
        helpText = "3-20 characters: letters, numbers, and underscores only.",
        pattern = "[A-Za-z0-9_]{3,20}",
        minlength = 3,
        maxlength = 20,
        autocomplete = "username"
    )
    @template.ashlar.components.text-input(
        name = "password",
        label = "Password",
        type = "password",
        autocomplete = "new-password",
        minlength = 8,
        helpText = "At least 8 characters."
    )
    @template.ashlar.components.text-input(
        name = "zipCode",
        label = "ZIP Code",
        placeholder = "12345",
        pattern = "\\\\d{5}(-\\\\d{4})?",
        maxlength = 10,
        inputmode = "numeric",
        autocomplete = "postal-code",
        helpText = "5 digits, or 9 digits with a hyphen."
    )
    @template.ashlar.components.text-input(
        name = "age",
        label = "Age",
        type = "number",
        placeholder = "18",
        inputmode = "numeric"
    )""";}
!{String iconsCode = """
    @template.ashlar.components.text-input(
        name = "accountSearch",
        label = "Search accounts",
        type = "search",
        placeholder = "Search by name or email",
        leadingIcon = Icon.SEARCH
    )
    @template.ashlar.components.text-input(
        name = "website",
        label = "Website",
        type = "url",
        placeholder = "https://example.com",
        trailingIcon = Icon.CHECK
    )""";}
!{String errorCode = """
    @template.ashlar.components.text-input(
        name = "signupEmail",
        label = "Email Address",
        type = "email",
        value = "taken@example.com",
        error = true,
        errorMessage = "This email address is already registered."
    )""";}
!{String requiredValidationCode = """
    <form action="" method="get">
        @template.ashlar.components.text-input(
            name = "requiredFirstName",
            label = "First Name",
            required = true,
            autocomplete = "given-name"
        )
        @template.ashlar.components.text-input(
            name = "requiredLastName",
            label = "Last Name",
            required = true,
            autocomplete = "family-name"
        )
        @template.ashlar.components.text-input(
            name = "requiredPhone",
            label = "Phone Number",
            type = "tel",
            mask = "phone",
            placeholder = "(505) 555-0100",
            required = true,
            pattern = "\\\\(\\\\d{3}\\\\) \\\\d{3}-\\\\d{4}"
        )
        @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
    </form>""";}

@template.layout.page(
contextPath = contextPath,
title = "Text Input",
content = @`
    @template.layout.component-header(contextPath = contextPath, slug = "text-input", sections = sections)

    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A native &lt;input&gt; underneath - required/pattern/minlength validation and focus behavior are all the browser's own. The one exception is Phone below, which opts into a small formatter via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask="phone"</code>.</p>

    <div class="mt-8 flex flex-col gap-10">
        <section id="quick-start" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Quick Start",
            content = @`
                @template.layout.usage-snippet(label = "", code = quickStartCode)
                <div class="mt-3 max-w-sm">
                    @template.ashlar.components.text-input(name = "quickStartFullName", label = "Full name", autocomplete = "name")
                </div>
            `
            )
        </section>

        <section id="how-to-use" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "How to Use",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">Reach for Text Input for any single-line free-text value - names, emails, identifiers, anything that isn't genuinely multi-line. For multi-line free text, use <a href="${contextPath}/components/textarea" class="text-blue-700 hover:underline dark:text-blue-400">Textarea</a>. For a value that's actually numeric and might be incremented, use <a href="${contextPath}/components/number-input" class="text-blue-700 hover:underline dark:text-blue-400">Number Input</a> instead - Text Input with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">inputmode="numeric"</code> (see "real-world examples" below) is for values that only look numeric, like a ZIP code. For the one specific case of searching rather than entering data, <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> adds a built-in clear button this component doesn't have.</p>
            `
            )
        </section>

        <section id="variants" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Variants & States",
            content = @`
                <div class="flex flex-col gap-8">
                    @template.layout.example(
                    label = "Basic states: empty, placeholder, preselected, disabled, readonly",
                    code = basicStatesCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.text-input(
                            name = "fullName",
                            label = "First Name"
                            )
                            @template.ashlar.components.text-input(
                            name = "emailPlaceholder",
                            label = "Email Address",
                            type = "email",
                            placeholder = "you@example.com"
                            )
                            @template.ashlar.components.text-input(
                            name = "company",
                            label = "Company",
                            value = "Acme Corporation"
                            )
                            @template.ashlar.components.text-input(
                            name = "disabledField",
                            label = "Disabled",
                            value = "Can't be edited",
                            disabled = true
                            )
                            @template.ashlar.components.text-input(
                            name = "readonlyField",
                            label = "Readonly",
                            value = "ACC-2026-00417",
                            readonly = true,
                            helpText = "Assigned automatically and can't be changed."
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Phone masking: type, validate, and select/replace/paste-test here</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Type digits and watch them format live. Try deleting from the middle, selecting part of the number and typing over it, and pasting a full number - formatted (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">(505) 555-1234</code>), unformatted (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">5055551234</code>), or with a leading country code (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">15055551234</code>) all normalize the same way. The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> below validates the finished display shape; submit the form further down to see the normalized, digits-only value that's actually sent.</p>
                        @template.layout.example(
                        code = phoneMaskingCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.text-input(
                                name = "phone",
                                label = "Phone Number",
                                type = "tel",
                                mask = "phone",
                                placeholder = "(505) 555-0100",
                                autocomplete = "tel",
                                pattern = "\\(\\d{3}\\) \\d{3}-\\d{4}",
                                helpText = "U.S. phone number, formatted automatically as you type."
                                )
                            </div>
                        `
                        )
                    </div>

                    @template.layout.example(
                    label = "Real-world examples: help text, patterns, and input modes",
                    code = realWorldCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.text-input(
                            name = "username",
                            label = "Username",
                            placeholder = "e.g. jane_doe",
                            helpText = "3-20 characters: letters, numbers, and underscores only.",
                            pattern = "[A-Za-z0-9_]{3,20}",
                            minlength = 3,
                            maxlength = 20,
                            autocomplete = "username"
                            )
                            @template.ashlar.components.text-input(
                            name = "password",
                            label = "Password",
                            type = "password",
                            autocomplete = "new-password",
                            minlength = 8,
                            helpText = "At least 8 characters."
                            )
                            @template.ashlar.components.text-input(
                            name = "zipCode",
                            label = "ZIP Code",
                            placeholder = "12345",
                            pattern = "\\d{5}(-\\d{4})?",
                            maxlength = 10,
                            inputmode = "numeric",
                            autocomplete = "postal-code",
                            helpText = "5 digits, or 9 digits with a hyphen."
                            )
                            @template.ashlar.components.text-input(
                            name = "age",
                            label = "Age",
                            type = "number",
                            placeholder = "18",
                            inputmode = "numeric"
                            )
                        </div>
                    `
                    )

                    @template.layout.example(
                    label = "Leading & trailing icons",
                    code = iconsCode,
                    content = @`
                        <div class="grid gap-4 sm:grid-cols-2">
                            @template.ashlar.components.text-input(
                            name = "accountSearch",
                            label = "Search accounts",
                            type = "search",
                            placeholder = "Search by name or email",
                            leadingIcon = Icon.SEARCH
                            )
                            @template.ashlar.components.text-input(
                            name = "website",
                            label = "Website",
                            type = "url",
                            placeholder = "https://example.com",
                            trailingIcon = Icon.CHECK
                            )
                        </div>
                    `
                    )

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
                        @template.layout.example(
                        code = errorCode,
                        content = @`
                            <div class="mt-3 max-w-sm">
                                @template.ashlar.components.text-input(
                                name = "signupEmail",
                                label = "Email Address",
                                type = "email",
                                value = "taken@example.com",
                                error = true,
                                errorMessage = "This email address is already registered."
                                )
                            </div>
                        `
                        )
                    </div>

                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard/Tab order &amp; form submission</p>
                        <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Three independent required fields, including the masked Phone from above. Tab through them, try submitting empty - the browser's own validation UI takes over and each field's border turns red once you've interacted with it - then fill them in and submit to see all three values (Phone normalized to digits) reflected in the URL's query string.</p>
                        @template.layout.example(
                        code = requiredValidationCode,
                        content = @`
                            <form class="mt-3 flex flex-col gap-4" action="" method="get">
                                <div class="grid gap-4 sm:grid-cols-2">
                                    @template.ashlar.components.text-input(
                                    name = "requiredFirstName",
                                    label = "First Name",
                                    required = true,
                                    autocomplete = "given-name"
                                    )
                                    @template.ashlar.components.text-input(
                                    name = "requiredLastName",
                                    label = "Last Name",
                                    required = true,
                                    autocomplete = "family-name"
                                    )
                                    @template.ashlar.components.text-input(
                                    name = "requiredPhone",
                                    label = "Phone Number",
                                    type = "tel",
                                    mask = "phone",
                                    placeholder = "(505) 555-0100",
                                    required = true,
                                    pattern = "\\(\\d{3}\\) \\d{3}-\\d{4}"
                                    )
                                </div>
                                @template.ashlar.components.button(text = "Submit", type = "Primary", buttonType = "submit")
                            </form>
                        `
                        )
                    </div>
                </div>
            `
            )
        </section>

        <section id="composition" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Composition",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">leadingIcon</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">trailingIcon</code> render a real Icon from the shared enum - purely decorative, never a clickable affordance on their own. Search Input reuses this exact slot internally for its own leading search glyph.</p>
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
                            <li>Set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">autocomplete</code> to a real token whenever one fits - it's what lets a browser or password manager fill the field correctly.</li>
                            <li>Pair <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">helpText</code> that states the rule in words - a regex alone isn't a useful error message to a visitor.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>Expecting <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask</code> to support anything beyond <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"phone"</code> - it's one hand-written formatter today, not a configurable masking language.</li>
                            <li>Reading the field's live value as the submitted value when <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask</code> is set - see Pitfalls.</li>
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
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300">A masked field submits digits, not the formatted display.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask="phone"</code> swaps the field's value to its canonical 10 digits right at form submit, after the browser's own validation has already passed - your server receives <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"5055551234"</code>, never <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"(505) 555-1234"</code>.</li>
                    <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> validates the display shape, not the submitted shape.</strong> With masking active, write <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> against what's visibly typed (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"(505) 555-1234"</code>), not the normalized digits that eventually get sent.</li>
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
                            <li>Pure-CSS error styling via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">user-invalid:</code> (not <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">invalid:</code>) - it only matches after the field has been interacted with or a submit attempted, so an untouched required field never looks like an error on first paint.</li>
                            <li>Native focus, validation, and keyboard behavior - no JavaScript at all unless <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask</code> is set.</li>
                        </ul>
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                        <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                            <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code>, and a real <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">autocomplete</code> token where one genuinely applies.</li>
                            <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">errorMessage</code> worth reading aloud - it renders with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="alert"</code>.</li>
                        </ul>
                    </div>
                </div>
            `
            )
        </section>

        <section id="api-reference" class="scroll-mt-20">
            @template.layout.api-reference(apiParams = textInputApi)
        </section>

        <section id="related" class="scroll-mt-20">
            @template.ashlar.utilities.page-section(
            title = "Related Components",
            content = @`
                <p class="text-sm text-slate-500 dark:text-slate-400">
                    <a href="${contextPath}/components/number-input" class="text-blue-700 hover:underline dark:text-blue-400">Number Input</a> for genuinely numeric values &middot;
                    <a href="${contextPath}/components/textarea" class="text-blue-700 hover:underline dark:text-blue-400">Textarea</a> for multi-line text &middot;
                    <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> for search fields with a built-in clear button
                </p>
            `
            )
        </section>
    </div>

    <div class="mt-10 border-t border-slate-200 pt-6 dark:border-slate-800">
        @template.layout.component-nav(currentSlug = "text-input", contextPath = contextPath)
    </div>
`
)
