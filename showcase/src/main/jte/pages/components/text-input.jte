@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 2A documentation page, same grammar as Batch 1. Existing example
     groups kept verbatim as Variants & States - the phone-masking section
     in particular was already an excellent live, hands-on demonstration
     and needed no rewriting.

     Verified against source before writing: text-input.jte's own @param
     list (19 params, the largest in this batch) and its doc comment -
     error styling is pure CSS via `user-invalid:` (not `invalid:`), which
     specifically only matches after the user has interacted with the
     field or a submit was attempted, so an untouched required field never
     renders as an error on first paint. Also read text-input.js in full:
     mask="phone" is one hand-written formatter, not a general masking
     system - it live-formats as "(505) 555-1234" while preserving caret
     position across typing/deleting/pasting, accepts an 11-digit paste
     starting with a US "1" country code and strips it, and swaps the
     field's value to the canonical 10 raw digits only on form submit,
     after the browser's own validation has already passed. --%>
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

@template.layout.page(
contextPath = contextPath,
title = "Text Input",
content = @`
    @template.layout.component-nav(currentSlug = "text-input", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Text Input</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A native &lt;input&gt; underneath - required/pattern/minlength validation and focus behavior are all the browser's own. The one exception is Phone below, which opts into a small formatter via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask="phone"</code>.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.text-input(name = "quickStartFullName", label = "Full name", autocomplete = "name")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Text Input for any single-line free-text value - names, emails, identifiers, anything that isn't genuinely multi-line. For multi-line free text, use <a href="${contextPath}/components/textarea" class="text-blue-700 hover:underline dark:text-blue-400">Textarea</a>. For a value that's actually numeric and might be incremented, use <a href="${contextPath}/components/number-input" class="text-blue-700 hover:underline dark:text-blue-400">Number Input</a> instead - Text Input with <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">inputmode="numeric"</code> (see "real-world examples" below) is for values that only look numeric, like a ZIP code. For the one specific case of searching rather than entering data, <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> adds a built-in clear button this component doesn't have.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic states: empty, placeholder, preselected, disabled, readonly</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Phone masking: type, validate, and select/replace/paste-test here</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Type digits and watch them format live. Try deleting from the middle, selecting part of the number and typing over it, and pasting a full number - formatted (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">(505) 555-1234</code>), unformatted (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">5055551234</code>), or with a leading country code (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">15055551234</code>) all normalize the same way. The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> below validates the finished display shape; submit the form further down to see the normalized, digits-only value that's actually sent.</p>
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Real-world examples: help text, patterns, and input modes</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Leading &amp; trailing icons</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Server-reported error</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Forced via the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">error</code> param - the same way a controller would re-render a field after a failed server-side check.</p>
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
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Required validation, keyboard/Tab order &amp; form submission</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Three independent required fields, including the masked Phone from above. Tab through them, try submitting empty - the browser's own validation UI takes over and each field's border turns red once you've interacted with it - then fill them in and submit to see all three values (Phone normalized to digits) reflected in the URL's query string.</p>
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
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">leadingIcon</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">trailingIcon</code> render a real Icon from the shared enum - purely decorative, never a clickable affordance on their own. Search Input reuses this exact slot internally for its own leading search glyph.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A masked field submits digits, not the formatted display.</strong> <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask="phone"</code> swaps the field's value to its canonical 10 digits right at form submit, after the browser's own validation has already passed - your server receives <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"5055551234"</code>, never <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"(505) 555-1234"</code>.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> validates the display shape, not the submitted shape.</strong> With masking active, write <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">pattern</code> against what's visibly typed (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"(505) 555-1234"</code>), not the normalized digits that eventually get sent.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
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
                </div>

                <div>
                    @template.layout.api-reference(apiParams = textInputApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/number-input" class="text-blue-700 hover:underline dark:text-blue-400">Number Input</a> for genuinely numeric values &middot;
                        <a href="${contextPath}/components/textarea" class="text-blue-700 hover:underline dark:text-blue-400">Textarea</a> for multi-line text &middot;
                        <a href="${contextPath}/components/search-input" class="text-blue-700 hover:underline dark:text-blue-400">Search Input</a> for search fields with a built-in clear button
                    </p>
                </div>
            </div>
        </section>
`
)
