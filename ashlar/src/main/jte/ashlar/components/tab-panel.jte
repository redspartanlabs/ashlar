@param String tabsId
@param String tabId
@param boolean active = false
@param gg.jte.Content content = null

!{String panelId = tabsId + "-panel-" + tabId;}
!{String tabButtonId = tabsId + "-tab-" + tabId;}
!{String visibilityClass = active ? "" : "hidden";}

<%-- `active` only decides what's visible before JS runs on first paint;
     tabs.js re-derives the true active tab from the tablist itself
     (aria-selected="true") and reconciles every panel's visibility to match
     at init, so a caller that forgets to keep this in sync with the paired
     tabs() call's activeTab self-heals rather than producing a stuck or
     mismatched panel. --%>
<div
        id="${panelId}"
        data-tab-panel
        data-tabs-id="${tabsId}"
        data-tab-id="${tabId}"
        role="tabpanel"
        aria-labelledby="${tabButtonId}"
        tabindex="0"
        class="${visibilityClass} py-4 text-sm text-slate-700 dark:text-slate-300"
>
    @if (content != null)
        ${content}
    @endif
</div>
