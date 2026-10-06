@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 3 documentation page. Existing "Semantic variants, with a title" /
     "Body without a title" examples are kept verbatim, moved under
     Variants & States. Added: Quick Start, formal grammar headings,
     Accessibility split, API Reference, Related Components, Pitfalls.

     Per this batch's lean grammar for simple/presentational components, no
     How to Use, Best Practices, or Common Patterns section - the Overview
     paragraph and the two Variants & States groups already say everything
     a caller needs, and forcing those headings here would just restate it.

     Verified against source before writing:
     - alert.jte's own 3 @params and its own doc comment in full.
       Confirmed, not assumed: there is deliberately NO role="alert" or
       role="status" - the comment explains this precisely, because every
       instance is server-rendered and already part of the page on first
       load, so a live-region role would make screen readers announce
       every alert already on the page the instant it loads, which is the
       disruptive behavior live regions exist to avoid for non-live
       content. This is the one fact the whole page has to get right.
     - AlertVariant.java: exactly 4 variants (INFO/SUCCESS/WARNING/ERROR),
       falling back silently to INFO on an unrecognized string. Confirmed
       the fourth is spelled ERROR, not DANGER like Button/Toast/Badge's
       equivalent variant - a real, deliberate naming difference (the
       enum's own doc comment explains why) worth flagging since it's an
       easy copy-paste mistake between components.
     - Confirmed title renders as a real <h3> only when non-empty, the
       same treatment Card gives its own title - not Toast's plain <p>,
       since an alert is lasting page structure and a toast isn't.
     - Confirmed a sr-only severity label always precedes the title/body,
       so meaning never depends on color or the decorative icon alone. --%>
!{String backtick = "`";}
!{String quickStartCode = "@template.ashlar.components.alert(\n"
        + "    variant = \"Warning\",\n"
        + "    title = \"Approaching seat limit\",\n"
        + "    body = @" + backtick + "<p>This account is using 9 of 10 available seats.</p>" + backtick + "\n"
        + ")";}
!{List<ApiParam> alertApi = List.of(
    ApiParam.optional("variant", "String", "\"Info\"", "Info/Success/Warning/Error. Unrecognized values silently fall back to Info. Note: Error, not Danger, unlike Button/Toast/Badge."),
    ApiParam.optional("title", "String", "\"\"", "Rendered as a real <h3> when non-empty. Omit it for a short notice that doesn't need one."),
    ApiParam.optional("body", "gg.jte.Content", "null", "The message itself. Omitted entirely from the DOM when null, rather than rendering an empty container.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Alert",
content = @`
    @template.layout.component-nav(currentSlug = "alert", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Alert</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A persistent, in-flow contextual message - not a Toast. No JavaScript, no dismiss button, no timer; it's part of the page for as long as the condition it describes is true.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-sm">
                        @template.ashlar.components.alert(
                        variant = "Warning",
                        title = "Approaching seat limit",
                        body = @`<p>This account is using 9 of 10 available seats.</p>`
                        )
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Semantic variants, with a title</p>
                            <div class="mt-3 flex flex-col gap-4">
                                @template.ashlar.components.alert(
                                variant = "Info",
                                title = "Scheduled maintenance",
                                body = @`
                                    <p>The billing service will be unavailable Saturday 10:00 PM - 2:00 AM ET for planned maintenance.</p>
                                `
                                )
                                @template.ashlar.components.alert(
                                variant = "Success",
                                title = "Changes saved",
                                body = @`
                                    <p>Your account settings were updated successfully.</p>
                                `
                                )
                                @template.ashlar.components.alert(
                                variant = "Warning",
                                title = "Approaching seat limit",
                                body = @`
                                    <p>This account is using 9 of 10 available seats. Add seats before inviting more teammates.</p>
                                `
                                )
                                @template.ashlar.components.alert(
                                variant = "Error",
                                title = "Unable to process payment",
                                body = @`
                                    <p>The card on file was declined. Update your payment method to avoid a service interruption.</p>
                                `
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Body without a title</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The title is optional - a short notice doesn't need one.</p>
                            <div class="mt-3 flex flex-col gap-4">
                                @template.ashlar.components.alert(
                                variant = "Info",
                                body = @`
                                    <p>Reports are generated nightly at 1:00 AM ET.</p>
                                `
                                )
                                @template.ashlar.components.alert(
                                variant = "Warning",
                                body = @`
                                    <p>You have unsaved changes. Leaving this page will discard them.</p>
                                `
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">An Alert injected after page load is not automatically announced.</strong> It has no live-region role by design (see Accessibility) - if your application adds one dynamically via JavaScript rather than on initial render, wrap it in your own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="status"</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-live</code> region, or reach for Toast instead.</li>
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">The fourth variant is <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"Error"</code>, not <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">"Danger"</code>.</strong> Button, Toast, and Badge all spell their equivalent variant Danger - passing that string here silently falls back to Info rather than erroring.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A sr-only severity label ("Warning:", etc.) read before the title/body, so meaning never depends on color or the decorative icon alone.</li>
                                <li>A real &lt;h3&gt; for the title when one is given, so it participates in the page's own heading structure rather than being invisible to it.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Your own live-region wiring if an alert is ever inserted dynamically after the initial page render - this component intentionally has none.</li>
                                <li>A heading level for the page that makes sense with Alert's title landing at &lt;h3&gt;.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = alertApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/toast" class="text-blue-700 hover:underline dark:text-blue-400">Toast</a> for a transient, JS-managed notification instead of a persistent one &middot;
                        <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> for a smaller inline severity indicator &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, which shares this component's real-&lt;h3&gt; title treatment
                    </p>
                </div>
            </div>
        </section>
`
)
