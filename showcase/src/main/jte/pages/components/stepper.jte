@param String contextPath

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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple completed steps</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Longer labels</h3>
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composed above a form</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A realistic placement: the Stepper sits above the current step's own content, unaware of what that content is.</p>
                    <div class="mt-4 max-w-2xl">
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
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Multiple instances stay independent</h3>
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
        </section>
`
)
