@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import java.util.List

@param String contextPath

<%-- Batch 5 documentation page. Existing "Basic" / "With supporting text" /
     "With a trend badge" / "Non-numeric value" / "Long label and value" /
     "In a dashboard layout" examples kept verbatim, moved under
     Variants & States. Added: Quick Start, formal grammar headings,
     Composition, Accessibility, API Reference, Related Components,
     Pitfalls.

     Not invented as a generic "metric" abstraction - documented exactly
     as the actual template is: label/value/description/change, nothing
     more, no formatting or calculation of its own.

     Verified against source before writing:
     - stat-card.jte's own 4 @params and its extensive doc comment.
       Confirmed value is a plain String - this component does no
       formatting, parsing, or calculation; the caller has already
       decided how the number should read ("12,481", "$48.2K", "99.9%",
       or a plain word like "Active") and this only ever displays exactly
       that string.
     - Confirmed the real markup is <dl>/<dt>/<dd>, not two <div>s - a
       metric is exactly the "term paired with its value(s)" relationship
       <dl> exists for, so no ARIA is needed for the label/value
       relationship itself. description, when present, is a second <dd>
       for the same <dt> (valid HTML), describing the metric as a whole,
       not the value string specifically.
     - Confirmed change is gg.jte.Content, not a string-plus-variant pair:
       a directional change's actual meaning (is rising good or bad?)
       depends entirely on what the metric is, which only the caller
       knows - this component has no opinion, and the existing "trend
       badge" example already demonstrates composing a real
       @template.ashlar.components.badge(...) into it rather than Stat
       Card inventing a second color vocabulary.
     - Confirmed, by direct comparison with card.jte, that this component
       does NOT reuse Card's title/body/footer slots at all, despite
       sharing its rounded-xl/border/bg surface treatment for visual
       consistency - label never goes through Card's title (which would
       render it at title weight, not the muted secondary weight a
       metric's name needs), and there's no body/footer here at all. A
       real, worth-stating distinction, not an assumption. --%>
!{String quickStartCode = """
    @template.ashlar.components.stat-card(
        label = "Active users",
        value = "2,481"
    )""";}
!{List<ApiParam> statCardApi = List.of(
    ApiParam.required("label", "String", "Rendered as a plain <dt> - never a heading, so a dashboard of several of these doesn't turn heading navigation into a list of near-identical stops."),
    ApiParam.required("value", "String", "Displayed exactly as given - no formatting, parsing, or calculation. The caller decides how the number should read."),
    ApiParam.optional("description", "String", "\"\"", "A second <dd> under the same <dt>, describing the metric as a whole - not a child of the value itself."),
    ApiParam.optional("change", "gg.jte.Content", "null", "Typically a composed Badge showing a directional change. This component has no opinion on whether a given change is good or bad - only the caller knows that.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Stat / Metric Card",
content = @`
    @template.layout.component-nav(currentSlug = "stat-card", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Stat / Metric Card</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">A single KPI-style metric in its own small surface - dashboards, result pages, summary sections. No JavaScript, no formatting or calculation: the caller supplies the value exactly as it should read.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3 max-w-xs">
                        @template.ashlar.components.stat-card(label = "Active users", value = "2,481")
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Basic</p>
                            <div class="mt-3 max-w-xs">
                                @template.ashlar.components.stat-card(
                                label = "Active users",
                                value = "2,481"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With supporting text</p>
                            <div class="mt-3 max-w-xs">
                                @template.ashlar.components.stat-card(
                                label = "Records migrated",
                                value = "12,481",
                                description = "Across the selected date range"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">With a trend badge</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The component draws no conclusions about direction - the caller decides whether a change is good or bad and picks the Badge variant accordingly.</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2">
                                @template.ashlar.components.stat-card(
                                label = "Revenue",
                                value = "$128.4K",
                                description = "vs. $114.7K last month",
                                change = @`@template.ashlar.components.badge(text = "+12%", variant = "Success")`
                                )
                                @template.ashlar.components.stat-card(
                                label = "Avg. response time",
                                value = "142ms",
                                description = "vs. 118ms last month",
                                change = @`@template.ashlar.components.badge(text = "+24ms", variant = "Danger")`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Non-numeric value</p>
                            <div class="mt-3 max-w-xs">
                                @template.ashlar.components.stat-card(
                                label = "System status",
                                value = "Operational",
                                change = @`@template.ashlar.components.badge(text = "All systems normal", variant = "Success")`
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Long label and value</p>
                            <div class="mt-3 max-w-xs">
                                @template.ashlar.components.stat-card(
                                label = "Quarterly compliance reports generated for enterprise accounts",
                                value = "1,204,981",
                                description = "Includes archived and superseded reports"
                                )
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">In a dashboard layout</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Several stat cards side by side, the way they'd appear at the top of a real dashboard.</p>
                            <div class="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                                @template.ashlar.components.stat-card(
                                label = "Revenue",
                                value = "$128.4K",
                                change = @`@template.ashlar.components.badge(text = "+12%", variant = "Success")`
                                )
                                @template.ashlar.components.stat-card(
                                label = "Active accounts",
                                value = "342"
                                )
                                @template.ashlar.components.stat-card(
                                label = "Open support tickets",
                                value = "18",
                                change = @`@template.ashlar.components.badge(text = "+5", variant = "Warning")`
                                )
                                @template.ashlar.components.stat-card(
                                label = "Uptime",
                                value = "99.98%"
                                )
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">The <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> slot exists specifically to hold a composed <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> - reusing its existing color vocabulary (Success/Warning/Danger) rather than this component inventing a second one it has no way to apply correctly, since only the caller knows whether a given change is actually good or bad for that particular metric.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Pitfalls &amp; Troubleshooting</h3>
                    <ul class="mt-2 list-disc space-y-2 pl-5 text-sm text-slate-500 dark:text-slate-400">
                        <li><strong class="font-medium text-slate-700 dark:text-slate-300">This is not built on top of Card</strong>, despite the visually matching bordered surface. It has no <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">title</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">body</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">footer</code> - its own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">description</code>/<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">change</code> contract is unrelated to Card's.</li>
                    </ul>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Native &lt;dl&gt;/&lt;dt&gt;/&lt;dd&gt; semantics for the label/value relationship - no ARIA needed, since the term-and-its-value pairing is already what &lt;dl&gt; means.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>A <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">value</code> already formatted the way it should be read aloud - this component passes it through unchanged.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = statCardApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a>, the usual content of the change slot &middot;
                        <a href="${contextPath}/components/card" class="text-blue-700 hover:underline dark:text-blue-400">Card</a>, a visually similar but independently-built surface
                    </p>
                </div>
            </div>
        </section>
`
)
