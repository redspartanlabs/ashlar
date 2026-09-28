@import com.redspartanlabs.ashlar.icon.Icon
<%-- The reference implementation also imported tabs.Tab here, but never used
     the type - the page's only "Tab" references are prose about keyboard tab
     order. Dropped rather than dragging Tabs into this batch for a dead
     import. --%>

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Text Input",
content = @`
    @template.layout.component-nav(currentSlug = "text-input", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Text Input</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A native &lt;input&gt; underneath - required/pattern/minlength validation and focus behavior are all the browser's own. The one exception is Phone below, which opts into a small formatter via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">mask="phone"</code>.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic states: empty, placeholder, preselected, disabled, readonly</h3>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Phone masking: type, validate, and select/replace/paste-test here</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Real-world examples: help text, patterns, and input modes</h3>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Leading &amp; trailing icons</h3>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Server-reported error</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Required validation, keyboard/Tab order &amp; form submission</h3>
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
        </section>
`
)
