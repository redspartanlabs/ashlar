// TEST-ONLY (Divider migration parity). Reads the browser's own computed styles and layout boxes for every Divider,
// either in a frame document (controlled oracle) or on a REAL showcase page (injected by RealPageProxy; the page
// templates are not touched).
//
// Output lines, tab separated, written into a result element appended to the body:
//   props<TAB>name,name,...
//   row<TAB>phase<TAB>ordinal<TAB>kind<TAB>orientation/layout<TAB>context<TAB>parentDisplay<TAB>value US value ...<TAB>diagnostics
//   shape<TAB>ordinal<TAB>tag<TAB>attribute names<TAB>role<TAB>aria-orientation<TAB>data-divider-orientation<TAB>children<TAB>class tokens
//   tok<TAB>id<TAB>name<TAB>value
//   meta<TAB>key<TAB>value
// US is the unit separator, \u001f. kind is always "divider". The ordinal is the divider's 1-based position among
// [data-divider] elements in document order. The layout is the closest ancestor's data-layout (a controlled frame
// names its wrappers) or "page". Names that start with "@" are not computed styles but the divider's box in its
// parent's coordinates, so a margin, a stretch or an offset shows up even where the computed value is "auto".
(function () {
  var US = "\u001f";
  var PROPS = ["display", "position", "color", "background-color",
    "border-top-width", "border-right-width", "border-bottom-width", "border-left-width",
    "border-top-style", "border-right-style", "border-bottom-style", "border-left-style",
    "border-top-color", "border-right-color", "border-bottom-color", "border-left-color",
    "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius",
    "box-shadow", "opacity", "outline-style", "padding-top", "padding-right", "padding-bottom", "padding-left",
    "margin-top", "margin-right", "margin-bottom", "margin-left", "width", "height", "min-width", "min-height",
    "max-width", "max-height", "box-sizing", "align-self", "justify-self", "flex-grow", "flex-shrink", "flex-basis",
    "overflow-x", "overflow-y", "vertical-align", "float", "clear", "unicode-bidi", "translate",
    "@x", "@y", "@w", "@h"];

  function attributeNames(el) {
    return [].map.call(el.attributes, function (a) { return a.name; }).sort().join(",");
  }

  function context(el) {
    var chain = [], p = el.parentElement;
    while (p && chain.length < 3) {
      var hook = "";
      for (var i = 0; i < p.attributes.length; i++) {
        var n = p.attributes[i].name;
        if (n.indexOf("data-") === 0 && n !== "data-theme" && n !== "data-skin") { hook = n.substring(5); break; }
      }
      chain.unshift(p.tagName.toLowerCase() + (hook ? "[" + hook + "]" : ""));
      p = p.parentElement;
    }
    return chain.join(" > ");
  }

  function layout(el) {
    for (var p = el.parentElement; p; p = p.parentElement) {
      if (p.hasAttribute("data-layout")) { return p.getAttribute("data-layout"); }
    }
    return "page";
  }

  function round(n) { return String(Math.round(n * 100) / 100); }

  function value(win, el, name) {
    if (name.charAt(0) !== "@") { return win.getComputedStyle(el).getPropertyValue(name); }
    var r = el.getBoundingClientRect();
    var parent = el.parentElement.getBoundingClientRect();
    return { "@x": round(r.left - parent.left), "@y": round(r.top - parent.top), "@w": round(r.width), "@h": round(r.height) }[name];
  }

  function row(win, el, ordinal, phase) {
    var parent = el.parentElement ? win.getComputedStyle(el.parentElement).display : "";
    var values = PROPS.map(function (p) { return value(win, el, p); });
    // Diagnostic only, never compared: the layout above this element (display and size of its ancestors).
    var diag = "boxes=" + el.getClientRects().length + " above=";
    for (var up = el.parentElement, n = 0; up && n < 3; up = up.parentElement, n++) {
      var ucs = win.getComputedStyle(up);
      diag += up.tagName.toLowerCase() + "[" + (up.getAttribute("class") || "").substring(0, 40) + "]:" + ucs.display + ":" + ucs.width + "x" + ucs.height + ">";
    }
    return ["row", phase, ordinal, "divider", (el.getAttribute("data-divider-orientation") || "") + "/" + layout(el), context(el), parent,
      values.join(US), diag].join("\t");
  }

  function measure(win, doc, phase) {
    return [].map.call(doc.querySelectorAll("[data-divider]"), function (el, index) { return row(win, el, index + 1, phase); });
  }

  // Every class token on the dividers themselves, sorted and unique.
  function shapes(doc) {
    return [].map.call(doc.querySelectorAll("[data-divider]"), function (el, index) {
      return ["shape", index + 1, el.tagName.toLowerCase(), attributeNames(el), el.getAttribute("role") || "",
        el.getAttribute("aria-orientation") || "", el.getAttribute("data-divider-orientation") || "",
        el.children.length + ":" + el.textContent.trim().length, (el.getAttribute("class") || "").split(/\s+/).filter(Boolean).sort().join(" ")].join("\t");
    });
  }

  // Puts the legacy Tailwind classes back on the SAME elements.
  function flipToLegacy(doc, roles) {
    [].forEach.call(doc.querySelectorAll("[data-divider]"), function (el) {
      el.setAttribute("class", roles[el.getAttribute("data-divider-orientation")]);
    });
  }

  // What the page's own slate palette resolves to, read through a throwaway element, so a test can compare a divider's
  // computed colour with what the active theme and skin actually define.
  function resolve(win, doc, declaration, property) {
    var el = doc.createElement("div");
    el.style.cssText = declaration;
    doc.body.appendChild(el);
    var resolved = win.getComputedStyle(el).getPropertyValue(property);
    doc.body.removeChild(el);
    return resolved;
  }

  function tokens(win, doc, id) {
    return ["200", "700"].map(function (shade) {
      var name = "--color-slate-" + shade;
      return ["tok", id, name, resolve(win, doc, "color:var(" + name + ")", "color")].join("\t");
    });
  }

  // The legacy utilities are compiled as plain class names (w-full, border-t, inline-block ...), and the page around the
  // Divider uses many of the same names. With Tailwind removed those elements are unstyled, so while the scoped legacy
  // stylesheet is linked they must not pick its rules up: the legacy names are taken off every element that is not a
  // divider, and put back afterwards. Both renderings are then measured in the same unstyled page.
  function legacyTokenSet(roles) {
    var set = {};
    Object.keys(roles).forEach(function (role) { roles[role].split(/\s+/).forEach(function (t) { set[t] = 1; }); });
    return set;
  }

  function detach(doc, set) {
    var saved = [];
    [].forEach.call(doc.querySelectorAll("[class]"), function (el) {
      if (el.hasAttribute("data-divider")) { return; }
      var original = el.getAttribute("class");
      var kept = original.split(/\s+/).filter(function (t) { return t && !set[t]; }).join(" ");
      if (kept !== original.trim()) {
        saved.push({ el: el, original: original });
        el.setAttribute("class", kept);
      }
    });
    return saved;
  }

  function reattach(saved) {
    var mismatch = 0;
    saved.forEach(function (s) {
      s.el.setAttribute("class", s.original);
      if (s.el.getAttribute("class") !== s.original) { mismatch++; }
    });
    return mismatch;
  }

  window.__ashDivider = { PROPS: PROPS, measure: measure, shapes: shapes, tokens: tokens };

  var probe = window.__ASH_PROBE;
  if (probe && probe.config) {
    window.addEventListener("load", function () {
      setTimeout(function () {
        var cfg = probe.config;
        var dividers = document.querySelectorAll("[data-divider]");
        var out = ["props\t" + PROPS.join(",")];
        out.push("meta\tdivider-count\t" + dividers.length);
        out.push("meta\tdividers-without-a-box\t" + [].filter.call(dividers, function (d) { return d.getClientRects().length === 0; }).length);
        out.push("meta\tdata-theme\t" + document.documentElement.getAttribute("data-theme"));
        out.push("meta\tdata-skin\t" + (document.documentElement.getAttribute("data-skin") || ""));
        out = out.concat(shapes(document));
        out = out.concat(tokens(window, document, "page"));
        out = out.concat(measure(window, document, cfg.phase || "ash"));
        function finish() {
          var pre = document.createElement("pre");
          pre.id = "ash-divider-results";
          pre.textContent = out.join("\n");
          document.body.appendChild(pre);
        }
        if (cfg.legacy) {
          // The legacy stylesheet is linked only AFTER the migrated rendering was measured, so that measurement
          // is exactly what production serves. Then the same elements get their legacy classes back.
          var saved = cfg.legacy.detach ? detach(document, legacyTokenSet(cfg.legacy.roles)) : [];
          out.push("meta\tdetached-elements\t" + saved.length);
          var link = document.createElement("link");
          link.rel = "stylesheet";
          link.href = cfg.legacy.stylesheet;
          link.onload = function () {
            flipToLegacy(document, cfg.legacy.roles);
            out = out.concat(measure(window, document, "legacy"));
            out.push("meta\tdetach-restore-mismatch\t" + reattach(saved));
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
