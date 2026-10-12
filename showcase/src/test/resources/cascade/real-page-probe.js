// TEST-ONLY. Injected into REAL showcase pages by RealPageProxy (the page
// templates are not touched). Reads the browser's own computed styles and
// CSSOM and writes tab-separated lines into a result element appended to the body.
//
//   meta<TAB>key<TAB>value
//   el<TAB>index<TAB>descriptor<TAB>value1 US value2 US ...   (US = \u001f)
//
// Mode "snapshot": per-element computed-style snapshot plus layer/sheet facts and
// a SIMULATED `properties` fallback check. Mode "interactive": real theme toggle
// and real skin selection.
(function () {
  var MODE = (window.__ASH_PROBE || {}).mode || "snapshot";
  var PROPS = ["display", "position", "color", "background-color", "border-top-color",
    "border-top-width", "border-top-style", "border-top-left-radius", "box-shadow",
    "font-family", "font-size", "font-weight", "line-height", "letter-spacing",
    "text-transform", "text-decoration-line", "padding-top", "padding-left", "margin-top",
    "width", "height", "opacity", "outline-style", "box-sizing", "translate"];
  var SKIP = { SCRIPT: 1, STYLE: 1, LINK: 1, NOSCRIPT: 1, TEMPLATE: 1, META: 1, TITLE: 1, HEAD: 1 };
  var out = [];

  function emit() { out.push(Array.prototype.slice.call(arguments).join("\t")); }
  function meta(key, value) { emit("meta", key, String(value).replace(/\s+/g, " ")); }
  function descr(el) {
    var cls = (el.getAttribute("class") || "").trim().split(/\s+/).slice(0, 3).join(".");
    // "~badge" marks a Badge and anything inside one, so a comparison can apply the parity normalizations
    // to exactly the migrated component's elements and to nothing else.
    return el.tagName.toLowerCase() + (el.id ? "#" + el.id : "") + (cls ? "." + cls : "") + (el.closest("[data-badge]") ? "~badge" : "");
  }

  function layerOrder() {
    var seen = [];
    function reg(name) {
      var parts = name.split("."), p = "";
      for (var a = 0; a < parts.length; a++) {
        p = p ? p + "." + parts[a] : parts[a];
        if (seen.indexOf(p) < 0) { seen.push(p); }
      }
    }
    for (var s = 0; s < document.styleSheets.length; s++) {
      var rs;
      try { rs = document.styleSheets[s].cssRules; } catch (e) { continue; }
      for (var r = 0; r < rs.length; r++) {
        if (rs[r] instanceof CSSLayerStatementRule) {
          for (var n = 0; n < rs[r].nameList.length; n++) { reg(rs[r].nameList[n]); }
        } else if (rs[r] instanceof CSSLayerBlockRule && rs[r].name) {
          reg(rs[r].name);
        }
      }
    }
    return seen.join(";");
  }

  function hasRule(selector) {
    var found = false;
    function walk(rules) {
      for (var i = 0; i < rules.length && !found; i++) {
        var rule = rules[i];
        if (rule instanceof CSSStyleRule && rule.selectorText === selector) { found = true; }
        else if (rule.cssRules) { walk(rule.cssRules); }
      }
    }
    for (var s = 0; s < document.styleSheets.length && !found; s++) {
      try { walk(document.styleSheets[s].cssRules); } catch (e) { /* cross-origin */ }
    }
    return found;
  }

  // A Badge (or anything inside one) that has no layout box where it is found - inside a [hidden] table row on a
  // paged table, a closed popover - has no geometry, so its radius cannot be compared. For exactly those elements
  // the ancestors that remove them from rendering are revealed for the length of ONE measurement and put back
  // before the next element is measured, so no other element is ever measured in a modified state. Both the
  // baseline and the integrated load run this same procedure on the same page state. Restoration is verified.
  var restoreMismatch = 0;
  var FALLBACK_DISPLAY = { tr: "table-row", td: "table-cell", th: "table-cell", tbody: "table-row-group", thead: "table-header-group",
    tfoot: "table-footer-group", li: "list-item", table: "table", span: "inline", a: "inline" };

  function describeHidden(el) {
    return el.tagName.toLowerCase() + (el.hasAttribute("hidden") ? "[hidden]" : "");
  }

  function reveal(el, how) {
    var touched = [];
    for (var p = el; p; p = p.parentElement) {
      if (getComputedStyle(p).display !== "none") { continue; }
      var t = { el: p, hidden: p.getAttribute("hidden"), style: p.getAttribute("style") };
      touched.push(t);
      var by = "";
      if (p.hasAttribute("hidden")) { p.removeAttribute("hidden"); by = "attr"; }
      if (getComputedStyle(p).display === "none") {
        p.style.setProperty("display", FALLBACK_DISPLAY[p.tagName.toLowerCase()] || "block", "important");
        by += (by ? "+" : "") + "style";
      }
      how.push(describeHidden(t.el) + ":" + by);
    }
    return touched;
  }

  function restore(touched) {
    touched.forEach(function (t) {
      if (t.hidden !== null) { t.el.setAttribute("hidden", t.hidden); } else { t.el.removeAttribute("hidden"); }
      if (t.style !== null) { t.el.setAttribute("style", t.style); } else { t.el.removeAttribute("style"); }
      if (t.el.getAttribute("hidden") !== t.hidden || t.el.getAttribute("style") !== t.style) { restoreMismatch++; }
    });
  }

  function snapshot() {
    var rows = [];
    var all = document.body.querySelectorAll("*");
    for (var i = 0; i < all.length; i++) {
      if (SKIP[all[i].tagName] || all[i].id === "ash-real-results") { continue; }
      var touched = [];
      if (all[i].closest("[data-badge]") && all[i].getClientRects().length === 0) { touched = reveal(all[i], []); }
      var cs = getComputedStyle(all[i]);
      var vals = [];
      for (var p = 0; p < PROPS.length; p++) { vals.push(cs.getPropertyValue(PROPS[p])); }
      rows.push({ d: descr(all[i]), v: vals.join("\u001f") });
      restore(touched);
    }
    return rows;
  }

  // Per Badge, as found: rendered normally, or needing a reveal (and by which ancestors). Counted once per load.
  function badgeRendering() {
    var normal = 0, revealed = 0, by = {};
    [].forEach.call(document.querySelectorAll("[data-badge]"), function (badge) {
      if (badge.getClientRects().length > 0) { normal++; return; }
      revealed++;
      var touched = reveal(badge, []);
      touched.forEach(function (t) { var k = describeHidden(t.el); by[k] = (by[k] || 0) + 1; });
      var ok = badge.getClientRects().length > 0;
      restore(touched);
      if (!ok) { by["STILL-NOT-RENDERED"] = (by["STILL-NOT-RENDERED"] || 0) + 1; }
    });
    return { normal: normal, revealed: revealed, by: by };
  }

  function token(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }

  function run() {
    var html = document.documentElement;
    meta("props", PROPS.join(","));
    // How many Badges the page has, and how many still carry the pre-migration Tailwind ring classes.
    var badges = document.querySelectorAll("[data-badge]");
    meta("badge-count", badges.length);
    var rendering = badgeRendering();
    meta("badge-normal", rendering.normal);
    meta("badge-revealed", rendering.revealed);
    meta("badge-revealed-by", Object.keys(rendering.by).sort().map(function (k) { return k + "=" + rendering.by[k]; }).join(","));
    meta("badge-legacy-count", [].filter.call(badges, function (b) { return (b.getAttribute("class") || "").indexOf("ring-inset") >= 0; }).length);
    meta("data-theme", html.getAttribute("data-theme"));
    meta("data-skin", html.getAttribute("data-skin") || "");
    meta("layer-order", layerOrder());
    var links = document.querySelectorAll('link[rel="stylesheet"]');
    var loaded = [], missing = [];
    for (var i = 0; i < links.length; i++) {
      var name = links[i].getAttribute("href").split("/").pop();
      (links[i].sheet ? loaded : missing).push(name);
    }
    meta("sheets-loaded", loaded.join(","));
    meta("sheets-missing", missing.join(","));

    if (MODE === "snapshot") {
      meta("ash-sr-only-rule", hasRule(".ash-sr-only"));
      meta("ash-color-bg", token("--ash-color-bg"));
      var rows = snapshot();
      meta("element-count", rows.length);
      rows.forEach(function (row, index) { emit("el", index, row.d, row.v); });

      // SIMULATION (not observed browser behaviour): inject the real, ungated
      // `properties` fallback rule from the compiled sheet into the same layer and
      // count how many elements' computed styles change.
      var supportsRule = null, styleRule = null;
      for (var s = 0; s < document.styleSheets.length && !styleRule; s++) {
        var rules;
        try { rules = document.styleSheets[s].cssRules; } catch (e) { continue; }
        for (var r = 0; r < rules.length && !styleRule; r++) {
          if (rules[r] instanceof CSSLayerBlockRule && rules[r].name === "properties") {
            for (var q = 0; q < rules[r].cssRules.length && !styleRule; q++) {
              if (rules[r].cssRules[q] instanceof CSSSupportsRule) {
                supportsRule = rules[r].cssRules[q];
                styleRule = supportsRule.cssRules[0];
              }
            }
          }
        }
      }
      meta("properties-fallback-found", styleRule !== null);
      if (styleRule) {
        meta("properties-gate-true-in-this-browser", CSS.supports(supportsRule.conditionText));
        var injected = new CSSStyleSheet();
        injected.replaceSync("@layer properties{" + styleRule.cssText + "}");
        document.adoptedStyleSheets = document.adoptedStyleSheets.concat([injected]);
        var after = snapshot();
        var changed = 0;
        for (var k = 0; k < rows.length; k++) { if (!after[k] || after[k].v !== rows[k].v) { changed++; } }
        meta("properties-simulation-changed-elements", changed);
      }
    } else {
      // Real behaviour: the theme toggle and the skin Select, as a visitor uses them.
      meta("theme-before", html.getAttribute("data-theme"));
      meta("ash-bg-before", token("--ash-color-bg"));
      var toggle = document.querySelector("[data-theme-toggle]");
      meta("theme-toggle-present", toggle !== null);
      if (toggle) {
        toggle.click();
        meta("theme-after-click", html.getAttribute("data-theme"));
        meta("ash-bg-after-click", token("--ash-color-bg"));
        meta("color-scheme-after-click", getComputedStyle(html).colorScheme);
        toggle.click();
        meta("theme-after-second-click", html.getAttribute("data-theme"));
        meta("ash-bg-after-second-click", token("--ash-color-bg"));
      }
      var select = document.querySelector("[data-skin-select] [data-select-input]");
      meta("skin-select-present", select !== null);
      if (select) {
        ["axiom", "ledger"].forEach(function (skin) {
          select.value = skin;
          select.dispatchEvent(new Event("change", { bubbles: true }));
          meta("skin-" + skin + "-data-skin", html.getAttribute("data-skin"));
          meta("skin-" + skin + "-blue-600", token("--color-blue-600"));
          meta("skin-" + skin + "-ash-accent", token("--ash-color-accent"));
        });
        select.value = "";
        select.dispatchEvent(new Event("change", { bubbles: true }));
        meta("skin-cleared-data-skin", html.getAttribute("data-skin") || "");
      }
    }
    meta("reveal-restore-mismatch", restoreMismatch);
    var pre = document.createElement("pre");
    pre.id = "ash-real-results";
    pre.textContent = out.join("\n");
    document.body.appendChild(pre);
  }

  window.addEventListener("load", function () { setTimeout(run, 600); });
})();
