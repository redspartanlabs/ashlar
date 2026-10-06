@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 8 documentation page. Existing "Basic" / "Multiple completed
     steps" / "Longer labels" / "Composed above a form" / "Multiple
     instances stay independent" examples kept verbatim, moved under
     Variants & States/Composition. Added: Quick Start, formal grammar
     headings, How to Use, Accessibility, two API Reference tables,
     Related Components.

     THE ONE THING THIS PAGE HAS TO GET RIGHT, per this batch's own
     explicit caution: a visually numbered Stepper does NOT automatically
     mean wizard/navigation semantics. Verified directly, not assumed
     from the visual: stepper-item.jte has no link, no button, no click
     handler anywhere, and stepper.jte has no JS file at all - confirmed
     by both files' own doc comments ("nothing ever changes after the
     server renders this once"). This is a pure, read-only progress
     indicator; clicking a step does nothing because there is nothing
     to click. The existing showcase page already states this
     correctly in its own intro ("not a wizard... non-interactive") -
     this page keeps that framing and makes it the How to Use section's
     first sentence rather than something a reader could miss.

     Verified against source before writing:
     - stepper.jte's own single @param. Confirmed step numbering is a
       pure CSS counter ([counter-reset:step]/[counter-increment:step]),
       not Java/JS bookkeeping - position needs no coordination the way
       Accordion's "which one wins" genuinely does.
     - Confirmed orientation is NOT a parameter at all: the
       horizontal/vertical switch is a CSS container query (`@container`
       plus `@sm:`, not a viewport `sm:` breakpoint) on the stepper's own
       rendered width, so a Stepper in a narrow sidebar on a wide desktop
       stacks vertically just as a phone-width one would - something a
       viewport media query alone could never do, confirmed by reading
       the exact class names used.
     - stepper-item.jte's own 2 @params (label, status - both required)
       and its doc comment. StepperStatus.java confirmed: exactly 3
       states (Completed/Current/Upcoming), falling back to UPCOMING
       (never COMPLETED) on an unrecognized string, specifically so a
       typo can never overstate progress. CONFIRMED there is no
       disabled state - the enum has no fourth value, and nothing in
       stepper-item.jte checks for one.
     - Confirmed the checkmark/number glyph is aria-hidden on purpose:
       CSS counter-generated content isn't reliably exposed to
       assistive tech across browsers, so the real state is announced
       instead via StepperStyles.statusText() as sr-only text after the
       label ("Completed"/"Current step"/"Not started") - never from the
       glyph alone. Confirmed aria-current="step" lives directly on the
       <li>, since there's no link/button here for it to attach to the
       way breadcrumb-item.jte's does. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.stepper(\n"
        + "    items = @" + backtick + "\n"
        + "        @template.ashlar.components.stepper-item(label = \"Account\", status = \"Completed\")\n"
        + "        @template.ashlar.components.stepper-item(label = \"Details\", status = \"Current\")\n"
        + "    " + backtick + "\n"
        + ")";}
!{List<ApiParam> stepperApi = List.of(
    ApiParam.required("items", "gg.jte.Content", "One or more stepper-item(...) calls, in order - that order is also the step numbering, via a CSS counter.")
);}
!{List<ApiParam> stepperItemApi = List.of(
    ApiParam.required("label", "String", "The step's visible text."),
    ApiParam.required("status", "String", "Completed/Current/Upcoming. Unrecognized values fall back to Upcoming - never Completed, so a typo can't overstate progress. There is no disabled state.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Stepper",
content = @`
    @template.layout.component-nav(currentSlug = "stepper", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Stepper</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Shows progress through a fixed, ordered sequence - not a wizard. The application decides which step is current and which are completed; the component only renders that state. Non-interactive: no clicks, no client-side navigation.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-md">
                        @template.ashlar.components.stepper(
                        items = @`
                            @template.ashlar.components.stepper-item(label = "Account", status = "Completed")
                            @template.ashlar.components.stepper-item(label = "Details", status = "Current")
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Stepper is a progress indicator, not a navigation control - there is no click handler anywhere in it, by design, so a visitor can never jump to a step by clicking it. Use it to show where someone is in a fixed sequence you already control elsewhere (a multi-page form, an onboarding flow); the actual step-to-step navigation - the Next/Back buttons, the URL changes - belongs to your own page, with Stepper simply reflecting whatever step is current. For a chronological record of what already happened rather than a fixed sequence someone is progressing through, use <a href="${contextPath}/components/timeline" class="text-blue-700 hover:underline dark:text-blue-400">Timeline</a> instead.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                            <div class="mt-4 max-w-2xl">
                                @template.ashlar.components.stepper(
                                items = @`
                                    @template.ashlar.components.stepper-item(label = "Account", status = "Completed")
                                    @template.ashlar.components.stepper-item(label = "Details", status = "Current")
                                    @template.ashlar.components.stepper-item(label = "Review", status = "Upcoming")
                                    @template.ashlar.components.stepper-item(label = "Complete", status = "Upcoming")
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple completed steps</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A five-step sequence further along - two completed, one current, two upcoming.</p>
                            <div class="mt-4 max-w-3xl">
                                @template.ashlar.components.stepper(
                                items = @`
                                    @template.ashlar.components.stepper-item(label = "Company Info", status = "Completed")
                                    @template.ashlar.components.stepper-item(label = "Billing", status = "Completed")
                                    @template.ashlar.components.stepper-item(label = "Team Members", status = "Current")
                                    @template.ashlar.components.stepper-item(label = "Integrations", status = "Upcoming")
                                    @template.ashlar.components.stepper-item(label = "Confirmation", status = "Upcoming")
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Longer labels</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Labels wrap in place rather than overflowing the page - resize the window to see it.</p>
                            <div class="mt-4 max-w-2xl">
                                @template.ashlar.components.stepper(
                                items = @`
                                    @template.ashlar.components.stepper-item(label = "Business Information", status = "Completed")
                                    @template.ashlar.components.stepper-item(label = "Verify Your Identity", status = "Current")
                                    @template.ashlar.components.stepper-item(label = "Complete Your Application", status = "Upcoming")
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Multiple instances stay independent</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">No JavaScript, no shared state - each Stepper is just its own rendered markup.</p>
                            <div class="mt-4 grid gap-6 sm:grid-cols-2">
                                <div class="max-w-sm">
                                    <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Onboarding</p>
                                    @template.ashlar.components.stepper(
                                    items = @`
                                        @template.ashlar.components.stepper-item(label = "Sign up", status = "Completed")
                                        @template.ashlar.components.stepper-item(label = "Verify", status = "Current")
                                        @template.ashlar.components.stepper-item(label = "Finish", status = "Upcoming")
                                    `
                                    )
                                </div>
                                <div class="max-w-sm">
                                    <p class="mb-2 text-sm font-medium text-slate-700 dark:text-slate-300">Checkout</p>
                                    @template.ashlar.components.stepper(
                                    items = @`
                                        @template.ashlar.components.stepper-item(label = "Cart", status = "Completed")
                                        @template.ashlar.components.stepper-item(label = "Shipping", status = "Completed")
                                        @template.ashlar.components.stepper-item(label = "Payment", status = "Current")
                                        @template.ashlar.components.stepper-item(label = "Confirm", status = "Upcoming")
                                    `
                                    )
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Stepper is the wrapper half of a wrapper/item pair, the same split List and Timeline use. <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">stepper-item</code> is not a separate catalog component; it only ever exists composed into Stepper's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">items</code> slot, documented here in the table below. A realistic placement puts Stepper above the current step's own content - it has no knowledge of, or opinion about, what that content is.</p>
                    <div class="mt-3 max-w-2xl">
                        @template.ashlar.components.stepper(
                        items = @`
                            @template.ashlar.components.stepper-item(label = "Account", status = "Completed")
                            @template.ashlar.components.stepper-item(label = "Details", status = "Current")
                            @template.ashlar.components.stepper-item(label = "Review", status = "Upcoming")
                        `
                        )
                        <div class="mt-6">
                            @template.ashlar.components.card(
                            title = "Your details",
                            body = @`
                                <p>Form fields for the current step would render here - Stepper has no knowledge of, or opinion about, what a step actually contains.</p>
                            `
                            )
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A real &lt;ol&gt;, so a screen reader announces "item 2 of 4" on its own with no extra ARIA for position.</li>
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-current="step"</code> on the current step's own &lt;li&gt;.</li>
                                <li>Each step's state announced as real sr-only text ("Completed", "Current step", "Not started") rather than relying on the decorative checkmark/number glyph, which CSS-generated content can't reliably expose to assistive tech across browsers anyway.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>The actual step-to-step navigation, if your flow has any - Stepper never provides it, so there is nothing to make keyboard-accessible here beyond the real controls your own page supplies elsewhere.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">stepper</p>
                        @template.layout.api-reference(apiParams = stepperApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">stepper-item</p>
                        @template.layout.api-reference(apiParams = stepperItemApi)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/timeline" class="text-blue-700 hover:underline dark:text-blue-400">Timeline</a> for a chronological record instead of a fixed sequence in progress &middot;
                        <a href="${contextPath}/components/progress" class="text-blue-700 hover:underline dark:text-blue-400">Progress</a> for a continuous percentage instead of discrete named steps &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a common host for the current step's own content
                    </p>
                </div>
            </div>
        </section>
`
)
