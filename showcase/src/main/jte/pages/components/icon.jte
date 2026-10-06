@import com.redspartanlabs.ashlar.showcase.catalog.ApiParam
@import com.redspartanlabs.ashlar.icon.Icon
@import java.util.List

@param String contextPath

<%-- Batch 2B documentation page (catalog-completion orphan). Icon had zero
     catalog presence despite being composed by 23 other library components
     (accordion-item, alert, badge, button, combobox, date-picker,
     date-range-picker, drawer, dropdown-menu, empty-state, file-upload,
     list-item, modal, search-input, select, sidebar-item, stepper-item,
     table, tabs, text-input, theme-toggle, timeline-item, toast,
     tree-item) - the single most-composed primitive in the library.

     Verified against source: icon.jte's own 3 params, Icon.java's full
     enum (confirmed count below is Icon.values().length, not a hand
     count that can drift), and IconSvg.java's doc comment (exhaustive
     switch - a missing case is a compile error, so "is this icon
     supported" is never a runtime question).

     The gallery below renders every Icon.values() entry directly rather
     than a hand-copied list, so it can never go stale relative to
     Icon.java - the one deliberate departure from this batch's "verify,
     don't infer" rule, because here the source enum IS the inventory. --%>
!{String quickStartCode = """
    @template.ashlar.components.icon(icon = Icon.SETTINGS)""";}
!{List<ApiParam> iconApi = List.of(
    ApiParam.required("icon", "Icon", "Which icon to render - see the full list below."),
    ApiParam.optional("classes", "String", "\"h-5 w-5\"", "Tailwind size classes. Button and other composing components override this to match their own size."),
    ApiParam.optional("label", "String", "\"\"", "Only pass this when the icon conveys meaning on its own, with no adjacent visible text or aria-label. See Accessibility.")
);}

@template.layout.page(
contextPath = contextPath,
title = "Icon",
content = @`
    @template.layout.component-nav(currentSlug = "icon", contextPath = contextPath)

        <section class="rounded-xl border border-slate-200 bg-white p-8 shadow-lg dark:border-slate-700 dark:bg-slate-900">
            <h2 class="text-lg font-semibold text-slate-900 dark:text-slate-100">Icon</h2>
            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">An inline &lt;svg&gt;, stroke-only and colored entirely via <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">currentColor</code> so it always matches the surrounding text. Every icon is bundled artwork, not a font or an external sprite request - there is nothing to load.</p>

            <div class="mt-6 flex flex-col gap-10">
                <div>
                    @template.layout.usage-snippet(label = "Quick Start", code = quickStartCode)
                    <div class="mt-3">
                        @template.ashlar.components.icon(icon = Icon.SETTINGS)
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">How to Use</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Most of the time you won't call Icon directly - you'll pass an <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Icon</code> value into another component's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">icon</code> param (Button, Alert, Badge, Text Input, and 19 others all have one). Call this component directly only for a bare icon in your own prose or layout, outside any of those built-in slots - exactly what this page's own gallery below does.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Variants &amp; States</h3>
                    <div class="mt-4 flex flex-col gap-8">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Sizing via classes</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">There is no size param - size is just the Tailwind height/width classes you pass.</p>
                            <div class="mt-3 flex flex-wrap items-center gap-4">
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-3 w-3")
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-5 w-5")
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-8 w-8")
                                @template.ashlar.components.icon(icon = Icon.STAR, classes = "h-12 w-12")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Color via currentColor</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">An icon has no color of its own - it always paints with the text color of whatever wraps it.</p>
                            <div class="mt-3 flex flex-wrap items-center gap-4">
                                <span class="text-slate-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                                <span class="text-blue-600 dark:text-blue-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                                <span class="text-green-600 dark:text-green-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                                <span class="text-red-600 dark:text-red-400">@template.ashlar.components.icon(icon = Icon.BELL, classes = "h-6 w-6")</span>
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Standalone, meaningful icon: label</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Passing <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> switches it from decorative (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-hidden</code>) to a named image (<code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">role="img"</code> + <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-label</code>) - only do this when no visible text or surrounding aria-label already says the same thing.</p>
                            <div class="mt-3">
                                @template.ashlar.components.icon(icon = Icon.CHECK_CIRCLE, classes = "h-6 w-6 text-green-600 dark:text-green-400", label = "Verified")
                            </div>
                        </div>

                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Full set (${Icon.values().length} icons)</p>
                            <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Rendered directly from the <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Icon</code> enum itself, in declaration order, so this list can't drift from what the library actually ships.</p>
                            <div class="mt-3 grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
                                @for (Icon value : Icon.values())
                                    <div class="flex flex-col items-center gap-1 rounded-md border border-slate-200 p-3 text-center dark:border-slate-700">
                                        @template.ashlar.components.icon(icon = value, classes = "h-5 w-5 text-slate-600 dark:text-slate-300")
                                        <span class="font-mono text-[10px] leading-tight text-slate-500 dark:text-slate-400">${value.name()}</span>
                                    </div>
                                @endfor
                            </div>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Composition</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">Icon is composed internally by 23 other library components - most visibly <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a>, where the icon's size is already paired to the button's own <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">size</code> so you never set <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">classes</code> yourself in that context.</p>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Best Practices</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Do</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Pair an icon with visible text wherever possible - an icon alone rarely identifies an action as reliably as a word does.</li>
                                <li>Use a component's own built-in <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">icon</code> param when one exists, rather than placing a bare Icon call next to it by hand.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Avoid</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li>Passing <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> on an icon that already sits next to visible text saying the same thing - that announces the meaning twice.</li>
                                <li>Picking an icon for decoration alone on a control a user must operate - pair it with text or an explicit <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">ariaLabel</code> on the control itself (see Button's Pitfalls).</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Accessibility</h3>
                    <div class="mt-2 grid gap-6 sm:grid-cols-2">
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">Ashlar provides</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">aria-hidden="true"</code> by default - an icon says nothing to assistive tech unless you opt it in.</li>
                                <li>An exhaustive artwork switch - every <code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">Icon</code> value is guaranteed to render something; there's no "missing icon" failure at runtime.</li>
                            </ul>
                        </div>
                        <div>
                            <p class="text-sm font-medium text-slate-700 dark:text-slate-300">You provide</p>
                            <ul class="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-500 dark:text-slate-400">
                                <li><code class="rounded bg-slate-100 px-1 py-0.5 text-xs dark:bg-slate-800">label</code> only on the rare icon that must convey meaning entirely on its own.</li>
                            </ul>
                        </div>
                    </div>
                </div>

                <div>
                    @template.layout.api-reference(apiParams = iconApi)
                </div>

                <div>
                    <h3 class="text-xs font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">Related Components</h3>
                    <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
                        <a href="${contextPath}/components/button" class="text-blue-700 hover:underline dark:text-blue-400">Button</a> for the most common icon host &middot;
                        <a href="${contextPath}/components/badge" class="text-blue-700 hover:underline dark:text-blue-400">Badge</a> and
                        <a href="${contextPath}/components/alert" class="text-blue-700 hover:underline dark:text-blue-400">Alert</a> for icon-paired status indicators
                    </p>
                </div>
            </div>
        </section>
`
)
