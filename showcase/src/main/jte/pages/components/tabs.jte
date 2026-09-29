@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.tabs.Tab
@import java.util.List

@param String contextPath

@template.layout.page(
contextPath = contextPath,
title = "Tabs",
content = @`
    @template.layout.component-nav(currentSlug = "tabs", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Tabs</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Four independent tab groups below - switching one never affects the others.</p>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Keyboard: <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&#8594;</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&#8592;</kbd> moves and activates the next/previous enabled tab, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd> jump to the first/last enabled tab, and disabled tabs are skipped entirely.</p>

            <div class="mt-6 flex flex-col gap-8">
                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Basic Tabs</h3>
                    <div class="mt-3">
                        @template.ashlar.components.tabs(
                        id = "project-tabs",
                        ariaLabel = "Project sections",
                        tabs = List.of(
                        Tab.of("overview", "Overview"),
                        Tab.of("details", "Details"),
                        Tab.of("history", "History")
                        )
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "project-tabs",
                        tabId = "overview",
                        active = true,
                        content = @`
                            <p>Acme Manufacturing has been an active enterprise account since January 2024, currently on the Enterprise plan with 3 seats.</p>
                        `
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "project-tabs",
                        tabId = "details",
                        content = @`
                            <ul class="list-disc space-y-1 pl-5">
                                <li>Account ID: ACC-1001</li>
                                <li>Owner: Dana Whitfield</li>
                                <li>Type: Enterprise</li>
                            </ul>
                        `
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "project-tabs",
                        tabId = "history",
                        content = @`
                            <ul class="list-disc space-y-1 pl-5">
                                <li>2026-06-02 &mdash; Plan upgraded to Enterprise</li>
                                <li>2024-01-12 &mdash; Account created</li>
                            </ul>
                        `
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Tabs with Icons</h3>
                    <div class="mt-3">
                        @template.ashlar.components.tabs(
                        id = "account-tabs",
                        ariaLabel = "Account settings",
                        tabs = List.of(
                        Tab.of("profile", "Profile").icon(Icon.USER),
                        Tab.of("settings", "Settings").icon(Icon.SETTINGS),
                        Tab.of("security", "Security").icon(Icon.SHIELD)
                        )
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "account-tabs",
                        tabId = "profile",
                        active = true,
                        content = @`<p>Name, title, and contact details shown to teammates.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "account-tabs",
                        tabId = "settings",
                        content = @`<p>Notification preferences, language, and time zone.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "account-tabs",
                        tabId = "security",
                        content = @`<p>Password, two-factor authentication, and active sessions.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Tabs with Badges</h3>
                    <div class="mt-3">
                        @template.ashlar.components.tabs(
                        id = "inbox-tabs",
                        ariaLabel = "Inbox",
                        tabs = List.of(
                        Tab.of("messages", "Messages").badge("4"),
                        Tab.of("orders", "Orders").badge("12"),
                        Tab.of("alerts", "Alerts").badge("2")
                        )
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "inbox-tabs",
                        tabId = "messages",
                        active = true,
                        content = @`<p>4 unread messages from teammates and customers.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "inbox-tabs",
                        tabId = "orders",
                        content = @`<p>12 orders awaiting fulfillment.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "inbox-tabs",
                        tabId = "alerts",
                        content = @`<p>2 alerts require attention.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Disabled tab &amp; initially selected tab</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Opens on "Usage" (not the first tab); "Invoices" is disabled and is skipped by keyboard navigation.</p>
                    <div class="mt-3">
                        @template.ashlar.components.tabs(
                        id = "billing-tabs",
                        activeTab = "usage",
                        ariaLabel = "Billing",
                        tabs = List.of(
                        Tab.of("plan", "Plan"),
                        Tab.of("usage", "Usage"),
                        Tab.of("invoices", "Invoices").disabled(true)
                        )
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "billing-tabs",
                        tabId = "plan",
                        content = @`<p>Enterprise plan, billed annually.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "billing-tabs",
                        tabId = "usage",
                        active = true,
                        content = @`<p>3 of 5 seats used this billing period.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "billing-tabs",
                        tabId = "invoices",
                        content = @`<p>Invoice history is unavailable for this account.</p>`
                        )
                    </div>
                </div>
            </div>
        </section>
`
)
