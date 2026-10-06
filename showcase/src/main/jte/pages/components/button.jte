@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.icon.Icon
@import java.util.List

@param String contextPath

<%-- Batch 2B documentation page (catalog-completion orphan, same grammar as
     Batches 1 and 2A). Button had zero catalog presence despite being used
     in 64 other files - this is net-new documentation, not a rewrite.

     Verified against source before writing: button.jte's own @param list
     (16 params), ButtonVariant.java/ButtonSize.java (both fall back silently
     on an unrecognized string rather than erroring - PRIMARY/MEDIUM), and
     button.js in full (setButtonLoading, initToggleButton,
     initLoadingDemoButton - the last of which is showcase-only wiring, not
     part of the public component). Two real, source-verified Pitfalls below
     (icon-only with no text/ariaLabel, and buttonType silently normalizing)
     came out of reading button.jte's own attribute-building lines closely,
     not from assumption. --%>
!{String quickStartCode = """
    @template.ashlar.components.button(
        text = "Save",
        type = "Primary"
    )""";}
!{List<ApiParam> buttonApi = List.of(
    ApiParam.optional("text", "String", "\"\"", "The visible label. Omit it entirely for an icon-only button."),
    ApiParam.optional("type", "String", "\"Primary\"", "Primary/Secondary/Success/Warning/Danger/Info/Ghost/Link. Unrecognized values silently fall back to Primary."),
    ApiParam.optional("size", "String", "\"Medium\"", "XSmall/Small/Medium/Large/XLarge. Unrecognized values silently fall back to Medium."),
    ApiParam.optional("buttonType", "String", "\"button\"", "The native button type attribute. Only \"submit\"/\"reset\" are honored - anything else, including an invalid string, normalizes to \"button\"."),
    ApiParam.optional("icon", "Icon", "null", "Renders via the Icon component at a size already paired to size."),
    ApiParam.optional("iconPosition", "String", "\"leading\"", "\"leading\" or \"trailing\". Ignored when iconOnly is true (or implied) - the icon is always centered then."),
    ApiParam.optional("iconOnly", "boolean", "false", "Hides the text visually (kept for screen readers) and switches to a square size. Also implied automatically whenever icon is set and text is empty."),
    ApiParam.optional("ariaLabel", "String", "\"\"", "The accessible name for an icon-only button. See Pitfalls - this is not optional in practice."),
    ApiParam.optional("disabled", "boolean", "false", "Native disabled - not focusable, not clickable."),
    ApiParam.optional("loading", "boolean", "false", "Forces the disabled look and a spinner in place of the icon, independent of disabled itself."),
    ApiParam.optional("fullWidth", "boolean", "false", "Stretches the button to its container's width."),
    ApiParam.optional("toggle", "boolean", "false", "Turns this into a two-state toggle button (see Variants & States) rather than a one-shot action."),
    ApiParam.optional("pressed", "boolean", "false", "The toggle button's initial aria-pressed state. Has no effect unless toggle is true."),
    ApiParam.optional("pressedText", "String", "\"\"", "Label shown while pressed. Defaults to text itself when left empty."),
    ApiParam.optional("id", "String", "\"\"", "Rendered only when non-empty."),
    ApiParam.optional("attributes", "String", "\"\"", "Raw extra HTML attributes, inserted unescaped via $unsafe{} - the same escape hatch form-field.jte and others already use for one-off wiring.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Button",
content = @`
    @template.layout.component-nav(currentSlug = "button", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Button</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A real &lt;button&gt;, never an &lt;a&gt; - it has no href and never navigates. For a link styled as a nav item, use <a href="${contextPath}/components/nav-link" class="text-blue-700 hover:underline dark:text-blue-400">NavLink</a> instead, or a plain anchor.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.button(text = "Save", type = "Primary")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Button for any one-shot action - submitting a form, opening a modal, triggering a request. It is the single most-composed primitive in Ashlar after Icon itself: Navbar, Modal, Drawer, Popover, and most form examples on this site already use it internally. For navigation (changing the URL), use a real link - <a href="${contextPath}/components/nav-link" class="text-blue-700 hover:underline dark:text-blue-400">NavLink</a> inside a nav list, or a plain &lt;a&gt; elsewhere.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Variants: type</p>
                            <div class="mt-3 flex flex-wrap gap-3">
                                @template.ashlar.components.button(text = "Primary", type = "Primary")
                                @template.ashlar.components.button(text = "Secondary", type = "Secondary")
                                @template.ashlar.components.button(text = "Success", type = "Success")
                                @template.ashlar.components.button(text = "Warning", type = "Warning")
                                @template.ashlar.components.button(text = "Danger", type = "Danger")
                                @template.ashlar.components.button(text = "Info", type = "Info")
                                @template.ashlar.components.button(text = "Ghost", type = "Ghost")
                                @template.ashlar.components.button(text = "Link", type = "Link")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Sizes: size</p>
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.button(text = "XSmall", type = "Secondary", size = "XSmall")
                                @template.ashlar.components.button(text = "Small", type = "Secondary", size = "Small")
                                @template.ashlar.components.button(text = "Medium", type = "Secondary", size = "Medium")
                                @template.ashlar.components.button(text = "Large", type = "Secondary", size = "Large")
                                @template.ashlar.components.button(text = "XLarge", type = "Secondary", size = "XLarge")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Icons: leading, trailing, icon-only</p>
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.button(text = "Download", type = "Secondary", icon = Icon.DOWNLOAD, iconPosition = "leading")
                                @template.ashlar.components.button(text = "Next", type = "Secondary", icon = Icon.ARROW_RIGHT, iconPosition = "trailing")
                                @template.ashlar.components.button(text = "Edit", type = "Ghost", icon = Icon.EDIT, iconOnly = true)
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">States: disabled, loading, full width</p>
                            <div class="mt-3 flex flex-wrap items-center gap-3">
                                @template.ashlar.components.button(text = "Disabled", type = "Primary", disabled = true)
                                @template.ashlar.components.button(text = "Save", type = "Primary", attributes = "data-btn-loading-demo")
                            </div>
                            <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">Click "Save" above - it sets its own loading state for 1.6s via the same <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">setButtonLoading()</code> helper a real page would call around its own fetch/submit. The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">data-btn-loading-demo</code> hook is this showcase's own wiring to simulate that without a backend, not a public Button attribute.</p>
                            <div class="mt-3 max-w-xs">
                                @template.ashlar.components.button(text = "Full width", type = "Secondary", fullWidth = true)
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Toggle button</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A self-contained two-state button - aria-pressed and its own classes/label flip on click, no page JavaScript required. For a value that submits with a form, use <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> instead; this is presentational state only.</p>
                            <div class="mt-3">
                                @template.ashlar.components.button(text = "Follow", pressedText = "Following", type = "Secondary", toggle = true)
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Button renders its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">icon</code> through the <a href="${contextPath}/components/icon" class="text-blue-700 hover:underline dark:text-blue-400">Icon</a> component internally, at a size already paired to size - you never size the icon yourself. Button is itself composed inside Navbar, Modal, Drawer, Popover, and most form examples on this site, always through its own public params, never a special internal variant.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Reserve <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Danger</code> for destructive, hard-to-undo actions so it keeps its meaning.</li>
                                <li>Pass <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code> explicitly on every icon-only button - don't rely on the fallback (see Pitfalls).</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Using Button for navigation - it has no href and never changes the URL. Use NavLink or a plain anchor.</li>
                                <li>Setting both <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">disabled</code> and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">loading</code> and expecting them to mean different things visually - loading already implies the disabled look.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">An icon-only button with no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">text</code> and no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code> gets an empty accessible name.</strong> The fallback is <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel.isEmpty() ? text : ariaLabel</code> - if both are empty, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-label=""</code> is still rendered, which is worse to a screen reader than no attribute at all. Always set one of the two on an icon-only button.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300"><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">buttonType</code> silently normalizes.</strong> Only exactly "submit" or "reset" pass through; anything else - including a typo - becomes "button" rather than erroring, so a misspelled <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">buttonType = "submti"</code> will quietly fail to submit the form instead of failing loudly.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">An unrecognized <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">type</code> or <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">size</code> doesn't error.</strong> Both fall back silently (to Primary/Medium), so a typo renders a valid-looking button with the wrong style instead of surfacing the mistake.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real native &lt;button&gt;, so Space/Enter activation, focus, and disabled are all the browser's own.</li>
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-busy</code> and an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live="polite"</code> "Loading" status while loading is true.</li>
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-pressed</code> kept in sync on every click when toggle is true.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code> on every icon-only button - see Pitfalls for what happens without it.</li>
                                <li>Text that names the actual action ("Delete account"), not a generic "Submit" or "Click here".</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = buttonApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/icon" class="text-blue-700 hover:underline dark:text-blue-400">Icon</a> for the icon slot's own API &middot;
                        <a href="${contextPath}/components/nav-link" class="text-blue-700 hover:underline dark:text-blue-400">NavLink</a> for navigation instead of action &middot;
                        <a href="${contextPath}/components/toggle" class="text-blue-700 hover:underline dark:text-blue-400">Toggle</a> for a form-submitted on/off value
                    </p>
                </div>
            </div>
        </section>
`
)
