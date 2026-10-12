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
    return el.tagName.toLowerCase() + (el.id ? "#" + el.id : "") + (cls ? "." + cls : "");
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

  function snapshot() {
    var rows = [];
    var all = document.body.querySelectorAll("*");
    for (var i = 0; i < all.length; i++) {
      if (SKIP[all[i].tagName] || all[i].id === "ash-real-results") { continue; }
      var cs = getComputedStyle(all[i]);
      var vals = [];
      for (var p = 0; p < PROPS.length; p++) { vals.push(cs.getPropertyValue(PROPS[p])); }
      rows.push({ d: descr(all[i]), v: vals.join("\u001f") });
    }
    return rows;
  }

  function token(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }

  function run() {
    var html = document.documentElement;
    meta("props", PROPS.join(","));
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
    var pre = document.createElement("pre");
    pre.id = "ash-real-results";
    pre.textContent = out.join("\n");
    document.body.appendChild(pre);
  }

  window.addEventListener("load", function () { setTimeout(run, 600); });
})();
