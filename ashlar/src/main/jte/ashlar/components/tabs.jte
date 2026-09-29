@import java.util.List
@import com.redspartanlabs.ashlar.tabs.Tab
@import com.redspartanlabs.ashlar.tabs.TabsSupport
@import com.redspartanlabs.ashlar.icon.Icon
@import com.redspartanlabs.ashlar.AshlarAssets

@param String id
@param List<Tab> tabs
@param String activeTab = ""
@param String ariaLabel = ""

!{String resolvedActiveTabId = TabsSupport.resolveActiveTabId(tabs, activeTab);}
!{String ariaLabelAttr = ariaLabel.isEmpty() ? null : ariaLabel;}
!{String tabBaseClasses = "inline-flex items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-medium transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:text-slate-300 disabled:hover:text-slate-300 disabled:hover:border-transparent dark:disabled:text-slate-600 dark:disabled:hover:text-slate-600";}
!{String activeTabClasses = tabBaseClasses + " border-blue-600 text-blue-600 hover:text-blue-700 dark:border-blue-500 dark:text-blue-400 dark:hover:text-blue-300";}
!{String inactiveTabClasses = tabBaseClasses + " border-transparent text-slate-500 hover:border-slate-300 hover:text-slate-700 dark:text-slate-400 dark:hover:border-slate-600 dark:hover:text-slate-200";}

<%-- Renders only the tablist. Each tab's panel is a separate
     @template.ashlar.components.tab-panel(...) call the caller places nearby - JTE
     content blocks can't be nested inside a Java builder chain (Tab.java),
     so panel markup can't live on the Tab objects themselves. Both this
     template and tab-panel.jte derive the same ids from `${id}` + the tab's
     own id, which is what keeps a tab and its panel correctly paired
     without sharing any object between the two calls. --%>
<div id="${id}" data-tabs>
    <div
            data-tabs-list
            role="tablist"
            aria-label="${ariaLabelAttr}"
            class="flex flex-wrap gap-1 border-b border-slate-200 dark:border-slate-700"
    >
        @for (Tab tab : tabs)
            !{boolean isActive = tab.id().equals(resolvedActiveTabId);}
            !{String tabButtonId = id + "-tab-" + tab.id();}
            !{String panelId = id + "-panel-" + tab.id();}
            !{String badgeText = tab.badge();}
            !{String tabClasses = isActive ? activeTabClasses : inactiveTabClasses;}

            <button
                    type="button"
                    id="${tabButtonId}"
                    data-tab
                    data-tab-id="${tab.id()}"
                    role="tab"
                    aria-selected="${String.valueOf(isActive)}"
                    aria-controls="${panelId}"
                    tabindex="${isActive ? "0" : "-1"}"
                    disabled="${tab.disabled()}"
                    data-tab-active-classes="${activeTabClasses}"
                    data-tab-inactive-classes="${inactiveTabClasses}"
                    class="${tabClasses}"
            >
                @if (tab.icon() != null)
                    @template.ashlar.components.icon(icon = tab.icon(), classes = "h-4 w-4")
                @endif
                <span>${tab.label()}</span>
                @if (badgeText != null && !badgeText.isEmpty())
                    <span class="inline-flex min-w-5 items-center justify-center rounded-full bg-slate-100 px-1.5 py-0.5 text-xs font-medium text-slate-600 dark:bg-slate-800 dark:text-slate-300">${badgeText}</span>
                @endif
            </button>
        @endfor
    </div>
</div>

<script type="module" src="${AshlarAssets.js("tabs.js")}"></script>
