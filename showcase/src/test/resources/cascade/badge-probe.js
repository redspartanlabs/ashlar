// TEST-ONLY. Measures every Badge on a page: its computed style, the style of any icon inside it, and its
// DOM shape. Used two ways:
//   1. as a library: the controlled frame test calls window.__ashBadge.measure(win, doc, phase);
//   2. injected into REAL showcase pages by RealPageProxy, where it runs on load, measures the badges as
//      rendered ("ash"), and - when the config asks - puts each badge's legacy Tailwind classes back on the
//      very same elements and measures again ("legacy"), all in one browser run.
//
// A Badge that has no layout box where it is found (inside a [hidden] table row, a closed popover) has no
// geometry to compare. For those the ancestors that remove it from rendering are revealed for the length of
// the measurement and put back exactly as they were; every such measurement is marked REVEALED, and the
// restoration is verified (meta reveal-restore-mismatch must be 0).
//
// Output lines, tab separated, written into a result element appended to the body:
//   props<TAB>name,name,...
//   row<TAB>phase<TAB>ordinal<TAB>kind<TAB>variant<TAB>context<TAB>parentDisplay<TAB>value US value ...<TAB>diagnostics
//   shape<TAB>ordinal<TAB>tag<TAB>attributes<TAB>child tags<TAB>svg attributes<TAB>svg aria-hidden<TAB>text
//   meta<TAB>key<TAB>value
// US is the unit separator, \u001f. kind is "badge" or "svg". The ordinal is the badge's 1-based position
// among [data-badge] elements in document order, so an icon row shares its badge's ordinal.
(function () {
  var US = "\u001f";
  var PROPS = ["display", "position", "color", "background-color",
    "border-top-width", "border-right-width", "border-bottom-width", "border-left-width",
    "border-top-style", "border-right-style", "border-bottom-style", "border-left-style", "border-top-color",
    "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius",
    "box-shadow", "font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform",
    "text-decoration-line", "font-variant-numeric", "padding-top", "padding-right", "padding-bottom", "padding-left",
    "margin-top", "margin-right", "margin-bottom", "margin-left", "width", "height", "opacity", "outline-style",
    "box-sizing", "translate", "flex-shrink", "align-items", "align-self", "gap", "white-space", "vertical-align"];

  // phase -> {normal, revealed}; restoreMismatch counts elements that were not put back exactly.
  var stats = { restoreMismatch: 0, phases: {} };

  function context(el) {
    var chain = [], p = el.parentElement;
    while (p && chain.length < 3) {
      var hook = "";
      for (var i = 0; i < p.attributes.length; i++) {
        var n = p.attributes[i].name;
        if (n.indexOf("data-") === 0 && n.indexOf("data-badge") !== 0 && n !== "data-theme" && n !== "data-skin") { hook = n.substring(5); break; }
      }
      chain.unshift(p.tagName.toLowerCase() + (hook ? "[" + hook + "]" : ""));
      p = p.parentElement;
    }
    return chain.join(" > ");
  }

  function describe(el) {
    return el.tagName.toLowerCase() + (el.hasAttribute("hidden") ? "[hidden]" : "") + (el.id ? "#" + el.id : "");
  }

  // The author rules (any stylesheet, any @layer/@media) that set display:none on el. UA rules are not listed.
  function hidingRules(el, limit) {
    var out = [];
    function walk(rules, ctx) {
      for (var i = 0; i < rules.length && out.length < limit; i++) {
        var r = rules[i];
        if (r.selectorText !== undefined && r.style && r.style.getPropertyValue("display") === "none") {
          try {
            if (el.matches(r.selectorText)) {
              out.push((ctx ? ctx + " " : "") + r.selectorText.substring(0, 90) + (r.style.getPropertyPriority("display") ? " !important" : ""));
            }
          } catch (e) { /* a selector that cannot be matched (nesting) is skipped */ }
        }
        if (r.cssRules && r.cssRules.length) {
          var label = r.name ? "@layer(" + r.name + ")" : (r.conditionText ? "@(" + r.conditionText.substring(0, 30) + ")" : "");
          walk(r.cssRules, ctx ? ctx + " " + label : label);
        }
      }
    }
    for (var s = 0; s < document.styleSheets.length && out.length < limit; s++) {
      try {
        var sheet = document.styleSheets[s];
        var before = out.length;
        walk(sheet.cssRules, "");
        for (var k = before; k < out.length; k++) { out[k] += " <" + ((sheet.href || "inline").split("/").pop()) + ">"; }
      } catch (e) { /* unreadable sheet */ }
    }
    return out;
  }

  // Diagnostic only, never compared: the badge's rendering state as FOUND, and the layout around it.
  function found(win, el) {
    var cs = win.getComputedStyle(el);
    var parent = el.parentElement;
    var pcs = parent ? win.getComputedStyle(parent) : null;
    var grand = parent && parent.parentElement ? win.getComputedStyle(parent.parentElement).display : "";
    var s = "boxes=" + el.getClientRects().length + " vis=" + cs.visibility
      + " parent=" + (pcs ? pcs.display + "/" + pcs.position + "/" + pcs.flexDirection : "") + " grand=" + grand;
    for (var p = el; p; p = p.parentElement) {
      if (win.getComputedStyle(p).display === "none") {
        s += " display-none-at=" + describe(p);
        var rules = hidingRules(p, 3);
        s += " rules=[" + (rules.length ? rules.join(" ; ") : "none:ua-or-attribute") + "]";
        if (p.getAttribute("style")) { s += " inline-style=" + p.getAttribute("style").replace(/[\t\n]/g, " ").substring(0, 40); }
        break;
      }
    }
    return s;
  }

  var FALLBACK_DISPLAY = { tr: "table-row", td: "table-cell", th: "table-cell", tbody: "table-row-group", thead: "table-header-group",
    tfoot: "table-footer-group", li: "list-item", table: "table", span: "inline", a: "inline" };

  // Un-hides every ancestor (and the element) whose display is none. Prefers removing the hidden attribute;
  // only if that does not reveal it, forces an inline display appropriate to its element type.
  function reveal(win, el) {
    var touched = [];
    for (var p = el; p; p = p.parentElement) {
      if (win.getComputedStyle(p).display !== "none") { continue; }
      var t = { el: p, hidden: p.getAttribute("hidden"), style: p.getAttribute("style"), how: "" };
      touched.push(t);
      if (p.hasAttribute("hidden")) { p.removeAttribute("hidden"); t.how = "attr"; }
      if (win.getComputedStyle(p).display === "none") {
        p.style.setProperty("display", FALLBACK_DISPLAY[p.tagName.toLowerCase()] || "block", "important");
        t.how += (t.how ? "+" : "") + "style";
      }
    }
    return touched;
  }

  function restore(touched) {
    touched.forEach(function (t) {
      if (t.hidden !== null) { t.el.setAttribute("hidden", t.hidden); } else { t.el.removeAttribute("hidden"); }
      if (t.style !== null) { t.el.setAttribute("style", t.style); } else { t.el.removeAttribute("style"); }
      if (t.el.getAttribute("hidden") !== t.hidden || t.el.getAttribute("style") !== t.style) { stats.restoreMismatch++; }
    });
  }

  function row(win, el, ordinal, kind, phase, diag) {
    var cs = win.getComputedStyle(el);
    var badge = el.closest("[data-badge]");
    var parent = el.parentElement ? win.getComputedStyle(el.parentElement).display : "";
    var values = PROPS.map(function (p) { return cs.getPropertyValue(p); });
    return ["row", phase, ordinal, kind, badge.getAttribute("data-badge-variant") || "", context(badge), parent, values.join(US), diag].join("\t");
  }

  function measure(win, doc, phase) {
    var rows = [];
    var st = stats.phases[phase] || (stats.phases[phase] = { normal: 0, revealed: 0 });
    [].forEach.call(doc.querySelectorAll("[data-badge]"), function (badge, index) {
      var diag = found(win, badge);
      var touched = [];
      if (badge.getClientRects().length === 0) {
        touched = reveal(win, badge);
        diag += " REVEALED(" + touched.map(function (t) { return describe(t.el) + ":" + t.how; }).join(",") + ") now-boxes=" + badge.getClientRects().length;
        st.revealed++;
      } else {
        st.normal++;
      }
      rows.push(row(win, badge, index + 1, "badge", phase, diag));
      [].forEach.call(badge.querySelectorAll("svg"), function (svg) { rows.push(row(win, svg, index + 1, "svg", phase, diag)); });
      restore(touched);
    });
    return rows;
  }

  function attributeNames(el) {
    return [].map.call(el.attributes, function (a) { return a.name; }).sort().join(",");
  }

  function shapes(doc) {
    var out = [];
    [].forEach.call(doc.querySelectorAll("[data-badge]"), function (badge, index) {
      var svg = badge.querySelector("svg");
      out.push(["shape", index + 1, badge.tagName.toLowerCase(), attributeNames(badge),
        [].map.call(badge.children, function (c) { return c.tagName.toLowerCase(); }).join(","),
        svg ? attributeNames(svg) : "", svg ? (svg.getAttribute("aria-hidden") || "") : "",
        badge.textContent.replace(/\s+/g, " ").trim()].join("\t"));
    });
    return out;
  }

  function flipToLegacy(doc, legacy) {
    [].forEach.call(doc.querySelectorAll("[data-badge]"), function (badge) {
      badge.setAttribute("class", legacy.variants[badge.getAttribute("data-badge-variant")]);
      [].forEach.call(badge.querySelectorAll("svg"), function (svg) {
        svg.setAttribute("class", svg.getAttribute("class").replace(/\bash-badge__icon\b/g, legacy.icon));
      });
    });
  }

  function statMeta() {
    var lines = ["meta\treveal-restore-mismatch\t" + stats.restoreMismatch];
    Object.keys(stats.phases).forEach(function (phase) {
      lines.push("meta\tnormal-" + phase + "\t" + stats.phases[phase].normal);
      lines.push("meta\trevealed-" + phase + "\t" + stats.phases[phase].revealed);
    });
    return lines;
  }

  window.__ashBadge = { PROPS: PROPS, measure: measure, stats: stats };

  var probe = window.__ASH_PROBE;
  if (probe && probe.config) {
    window.addEventListener("load", function () {
      setTimeout(function () {
        var cfg = probe.config;
        var out = ["props\t" + PROPS.join(",")];
        out.push("meta\tbadge-count\t" + document.querySelectorAll("[data-badge]").length);
        out.push("meta\tdata-theme\t" + document.documentElement.getAttribute("data-theme"));
        out.push("meta\tdata-skin\t" + (document.documentElement.getAttribute("data-skin") || ""));
        out = out.concat(shapes(document));
        out = out.concat(measure(window, document, cfg.phase || "ash"));
        function finish() {
          out = out.concat(statMeta());
          var pre = document.createElement("pre");
          pre.id = "ash-badge-results";
          pre.textContent = out.join("\n");
          document.body.appendChild(pre);
        }
        if (cfg.legacy) {
          // The legacy stylesheet is linked only AFTER the migrated rendering was measured, so that measurement
          // is exactly what production serves. Then the same elements get their legacy classes back.
          var link = document.createElement("link");
          link.rel = "stylesheet";
          link.href = cfg.legacy.stylesheet;
          link.onload = function () {
            flipToLegacy(document, cfg.legacy);
            out = out.concat(measure(window, document, "legacy"));
            finish();
          };
          link.onerror = function () { out.push("meta\terror\tlegacy stylesheet failed to load"); finish(); };
          document.head.appendChild(link);
        } else {
          finish();
        }
      }, 600);
    });
  }
})();
