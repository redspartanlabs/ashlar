@import java.util.List
@import dev.redspartanlabs.ashlar.tabs.Tab
@import dev.redspartanlabs.ashlar.tabs.TabsSupport
@import dev.redspartanlabs.ashlar.icon.Icon
@import dev.redspartanlabs.ashlar.AshlarAssets

@param String id
@param List<Tab> tabs
@param String activeTab = ""
@param String ariaLabel = ""

!{String resolvedActiveTabId = TabsSupport.resolveActiveTabId(tabs, activeTab);}
!{String ariaLabelAttr = ariaLabel.isEmpty() ? null : ariaLabel;}

<%-- Renders only the tablist. Each tab's panel is a separate
     @template.ashlar.components.tab-panel(...) call the caller places nearby - JTE
     content blocks can't be nested inside a Java builder chain (Tab.java),
     so panel markup can't live on the Tab objects themselves. Both this
     template and tab-panel.jte derive the same ids from `${id}` + the tab's
     own id, which is what keeps a tab and its panel correctly paired
     without sharing any object between the two calls. --%>
<%-- The look of a tab (selected, disabled, hover, focus) is tabs.css's response to the button's own aria-selected and disabled, so the template
     renders no class string for state and tabs.js sets no class: selecting a tab changes attributes only. --%>
<div id="${id}" data-tabs class="ash-tabs">
    <div
            data-tabs-list
            role="tablist"
            aria-label="${ariaLabelAttr}"
            class="ash-tabs__list"
    >
        @for (Tab tab : tabs)
            !{boolean isActive = tab.id().equals(resolvedActiveTabId);}
            !{String tabButtonId = id + "-tab-" + tab.id();}
            !{String panelId = id + "-panel-" + tab.id();}
            !{String badgeText = tab.badge();}

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
                    class="ash-tabs__tab"
            >
                @if (tab.icon() != null)
                    @template.ashlar.components.icon(icon = tab.icon(), classes = "ash-icon--sm")
                @endif
                <span class="ash-tabs__label">${tab.label()}</span>
                @if (badgeText != null && !badgeText.isEmpty())
                    <span class="ash-tabs__badge">${badgeText}</span>
                @endif
            </button>
        @endfor
    </div>
</div>

<script type="module" src="${AshlarAssets.js("tabs.js")}"></script>
