@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.tabs.Tab
@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 9 documentation page. Existing "Basic Tabs" / "Tabs with Icons" /
     "Tabs with Badges" / "Disabled tab & initially selected tab" examples
     kept verbatim, moved under Variants & States. Added: Quick Start,
     formal grammar headings, How to Use, Composition, Accessibility,
     three API Reference tables (tabs, tab-panel, and the Tab Java
     builder), Related Components, Pitfalls.

     Verified against source before writing - including what "accessible
     APG Tabs" actually means here rather than asserting it generically:
     - tabs.jte's own 4 @params and doc comment. CONFIRMED this is NOT
       the gg.jte.Content wrapper/item pattern the rest of the library
       uses: `tabs` is a List<Tab>, a plain Java builder object (Tab.java),
       because JTE content blocks can't be nested inside a Java method
       chain - panel markup can't live on a Tab object, so it's supplied
       as a separate tab-panel(...) call instead.
     - tab-panel.jte's own 4 @params. CONFIRMED every panel is fully
       server-rendered at all times, for every tab, not conditionally
       rendered - `active` only controls a CSS class (hidden vs. not) on
       first paint; the inactive panels' real content is already in the
       DOM.
     - Tab.java and TabsSupport.java. Confirmed Tab's builder carries
       id/label/icon/badge/disabled. Confirmed the initial-active
       resolution order precisely: the requested activeTab if it exists
       and isn't disabled, otherwise the first enabled tab, otherwise
       just the first tab of any kind (so an all-disabled group still
       renders one coherent state rather than none).
     - tabs.js in full. CONFIRMED the exact interaction model, by its own
       top comment: WAI-ARIA "automatic activation" - moving focus with
       Arrow Left/Right/Home/End immediately activates that tab and shows
       its panel, with no separate Enter/Space confirmation step. Real
       roving tabindex (active tab = 0, every other = -1). Arrow Left/
       Right cycle through ENABLED tabs only, wrapping at the ends;
       Home/End jump to the first/last enabled tab. Enter/Space work
       purely because it's a native <button> - not custom-handled.
     - A GENUINE ARCHITECTURAL EXCEPTION, confirmed directly in tabs.js:
       every other interactive component in this library (Tree, Accordion,
       Combobox, Dropdown Menu...) scopes its own DOM queries to its own
       root so multiple instances never interfere. Tabs' panels cannot be
       root-scoped the same way, because they are NOT nested inside the
       tabs root at all - the caller places each tab-panel(...) wherever
       it belongs on the page, so tabs.js finds them with a document-wide
       query matched by a shared data-tabs-id string. This is a real,
       source-confirmed difference from the rest of the library's usual
       per-instance isolation, not an oversight - reported in this
       batch's final report. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.tabs(\n"
        + "    id = \"account-tabs\",\n"
        + "    ariaLabel = \"Account\",\n"
        + "    tabs = List.of(\n"
        + "        Tab.of(\"profile\", \"Profile\"),\n"
        + "        Tab.of(\"settings\", \"Settings\")\n"
        + "    )\n"
        + ")\n"
        + "@template.ashlar.components.tab-panel(\n"
        + "    tabsId = \"account-tabs\",\n"
        + "    tabId = \"profile\",\n"
        + "    active = true,\n"
        + "    content = @" + backtick + "<p>Profile content.</p>" + backtick + "\n"
        + ")\n"
        + "@template.ashlar.components.tab-panel(\n"
        + "    tabsId = \"account-tabs\",\n"
        + "    tabId = \"settings\",\n"
        + "    content = @" + backtick + "<p>Settings content.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> tabsApi = List.of(
    ApiParam.required("id", "String", "A unique id for this instance - generated tab button/panel ids are derived from it, and it's what pairs a tabs() call to its tab-panel(...) calls elsewhere on the page."),
    ApiParam.required("tabs", "List<Tab>", "The tab buttons, as Tab.of(id, label) builder calls - see the Tab table below."),
    ApiParam.optional("activeTab", "String", "\"\"", "The tab id to render active initially. Falls back to the first enabled tab if omitted, unmatched, or disabled; falls back further to the first tab of any kind if every tab is disabled."),
    ApiParam.optional("ariaLabel", "String", "\"\"", "Accessible name for the tablist, since unlike Breadcrumbs/Pagination there's no single APG-assigned name for every possible set of tabs.")
);}
!{List<ApiParam> tabPanelApi = List.of(
    ApiParam.required("tabsId", "String", "Must match the paired tabs() call's own id."),
    ApiParam.required("tabId", "String", "Must match one Tab's own id from that tabs() call."),
    ApiParam.optional("active", "boolean", "false", "Only controls visibility on first paint. tabs.js re-derives the true active tab from the tablist's own aria-selected at init and reconciles every panel to match - see Pitfalls."),
    ApiParam.optional("content", "gg.jte.Content", "null", "The panel's content - always fully rendered, never conditionally, regardless of active.")
);}
!{List<ApiParam> tabBuilderApi = List.of(
    ApiParam.required("id", "String", "Tab.of(id, label) - this tab's identity, matched against tab-panel's own tabId."),
    ApiParam.required("label", "String", "Tab.of(id, label) - the visible tab text."),
    ApiParam.optional(".icon(Icon)", "Icon", "none", "Adds a leading icon."),
    ApiParam.optional(".badge(String)", "String", "none", "Adds a small count/status pill after the label."),
    ApiParam.optional(".disabled(boolean)", "boolean", "false", "Renders a native disabled <button> - skipped by Arrow Left/Right/Home/End, and never eligible as the resolved initial active tab.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Tabs",
content = @`
    @template.layout.component-nav(currentSlug = "tabs", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Tabs</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Switches between peer panels of equal importance - distinct from Accordion's stacked disclosure. Uses the WAI-ARIA "automatic activation" model: Arrow Left/Right/Home/End move focus and immediately activate that tab's panel, with no separate confirmation step.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.tabs(
                        id = "quickStartTabs",
                        ariaLabel = "Account",
                        tabs = List.of(
                        Tab.of("profile", "Profile"),
                        Tab.of("settings", "Settings")
                        )
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "quickStartTabs",
                        tabId = "profile",
                        active = true,
                        content = @`<p>Profile content.</p>`
                        )
                        @template.ashlar.components.tab-panel(
                        tabsId = "quickStartTabs",
                        tabId = "settings",
                        content = @`<p>Settings content.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Reach for Tabs when several views are equally important and a visitor switches between them, not progresses through them in order - for a fixed sequence someone moves forward through, see <a href="${contextPath}/components/stepper" class="text-blue-700 hover:underline dark:text-blue-400">Stepper</a>; for independently collapsible sections rather than mutually exclusive views, see <a href="${contextPath}/components/accordion" class="text-blue-700 hover:underline dark:text-blue-400">Accordion</a>. Because every panel is server-rendered up front regardless of which tab is active, avoid putting anything expensive or load-bearing only on inactive panels - all of it ships and exists in the DOM on every page load.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Four independent tab groups below - switching one never affects the others. Keyboard: <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&#8594;</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">&#8592;</kbd> moves and activates the next/previous enabled tab, <kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">Home</kbd>/<kbd class="rounded border border-slate-300 bg-slate-100 px-1 py-0.5 text-xs dark:border-slate-600 dark:bg-slate-800">End</kbd> jump to the first/last enabled tab, and disabled tabs are skipped entirely.</p>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic Tabs</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Tabs with Icons</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Tabs with Badges</p>
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
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Disabled tab &amp; initially selected tab</p>
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
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Tabs is the one component in this library that doesn't use the gg.jte.Content wrapper/item pattern Accordion/List/Tree/Breadcrumbs all share. A JTE content block can't be nested inside a Java method chain, so a panel's markup can't live on a <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Tab</code> object - instead, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabs()</code> only renders the tablist from a plain <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">List&lt;Tab&gt;</code>, and each panel is a separate <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tab-panel(...)</code> call placed wherever the caller wants, paired to its tab purely by matching <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabsId</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabId</code> strings - not by any shared object or DOM nesting. This is also why Tabs is the one exception to this library's usual per-instance root-scoping: since a panel doesn't live inside the tablist's own root, tabs.js has to find panels with a document-wide query instead.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">A tab-panel's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">active</code> only matters for the very first paint.</strong> Once JS runs, it re-derives the real active tab from the tablist's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-selected</code> and reconciles every panel to match - so a panel whose <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">active</code> disagrees with the paired <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabs()</code> call's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">activeTab</code> self-heals after load rather than staying stuck mismatched - but can still show the wrong panel very briefly before JS runs.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Every panel is in the DOM on every load, regardless of which tab is active.</strong> There is no conditional rendering - an inactive panel is only CSS-hidden, so its content is already shipped and present.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">Panels are matched by id across the whole page, not by nesting.</strong> A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabsId</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabId</code> pair that accidentally matches a different Tabs instance's own ids would pair to the wrong panel - unlike every other wrapper/item component here, nothing about DOM nesting protects against that.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>The full WAI-ARIA tablist pattern: <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="tablist"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="tab"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="tabpanel"</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-selected</code>, <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-controls</code>, and <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-labelledby</code>, with automatic activation and real roving tabindex (exactly one tab is ever a Tab stop at a time).</li>
                                <li>A safe initial state even when every tab is disabled - the resolution always lands on some tab rather than none.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>An <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code> naming this particular set of tabs - there's no single fixed APG name to fall back to the way Breadcrumbs/Pagination have.</li>
                                <li>Matching, unique <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabsId</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">tabId</code> pairs - see Pitfalls.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div class="flex flex-col gap-6">
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">tabs</p>
                        @template.layout.api-reference(apiParams = tabsApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">tab-panel</p>
                        @template.layout.api-reference(apiParams = tabPanelApi)
                    </div>
                    <div>
                        <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Tab (Java builder, passed to tabs' own tabs param)</p>
                        @template.layout.api-reference(apiParams = tabBuilderApi)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/accordion" class="text-blue-700 hover:underline dark:text-blue-400">Accordion</a> for independently collapsible sections instead of mutually exclusive views &middot;
                        <a href="${contextPath}/components/stepper" class="text-blue-700 hover:underline dark:text-blue-400">Stepper</a> for a fixed sequence someone progresses through, rather than switches between
                    </p>
                </div>
            </div>
        </section>
`
)
