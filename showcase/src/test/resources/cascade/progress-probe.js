// TEST-ONLY (Progress migration parity). Reads the browser's own computed styles and layout boxes for every Progress
// and the elements inside it, either in a frame document (controlled oracle) or on a REAL showcase page (injected
// by RealPageProxy; the page templates are not touched).
//
// Output lines, tab separated, written into a result element appended to the body:
//   props<TAB>name,name,...
//   row<TAB>phase<TAB>ordinal<TAB>kind<TAB>shape/layout<TAB>context<TAB>parentDisplay<TAB>value US value ...<TAB>diagnostics
//   shape<TAB>ordinal<TAB>wrapper attrs<TAB>child tags<TAB>header tags<TAB>track attrs<TAB>track child tags<TAB>fill attrs<TAB>aria-valuenow<TAB>fill width style<TAB>percent text<TAB>aria-label<TAB>class tokens
//   tok<TAB>id<TAB>name<TAB>value
//   meta<TAB>key<TAB>value
// US is the unit separator, \u001f. kind is progress, header, label, value, track or fill. The ordinal is the
// Progress's 1-based position among [data-progress] elements in document order, so every row of one Progress shares
// it. The shape is size/header ("medium/lv" = a label and a value, "-v" a value only, "l-" a label only, "--" no
// header) and the layout is the closest ancestor's data-layout (a controlled frame names its wrappers) or "page".
// Names that start with "@" are not computed styles but the element's box in its parent's coordinates.
(function () {
  var US = "\u001f";
  var PROPS = ["display", "position", "color", "background-color", "opacity",
    "border-top-width", "border-right-width", "border-bottom-width", "border-left-width",
    "border-top-style", "border-right-style", "border-bottom-style", "border-left-style",
    "border-top-color", "border-right-color", "border-bottom-color", "border-left-color",
    "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius",
    "box-shadow", "font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform",
    "padding-top", "padding-right", "padding-bottom", "padding-left",
    "margin-top", "margin-right", "margin-bottom", "margin-left", "width", "height", "min-width", "min-height",
    "max-width", "max-height", "box-sizing", "flex-direction", "flex-wrap", "flex-grow", "flex-shrink", "flex-basis",
    "align-items", "align-self", "justify-content", "row-gap", "column-gap", "white-space", "overflow-x", "overflow-y",
    "overflow-wrap", "vertical-align", "text-align", "transition-property", "animation-name",
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

  // The parts of one Progress, found structurally (the classes differ between the migrated and the legacy rendering).
  function parts(progress) {
    var header = [].filter.call(progress.children, function (c) { return c.tagName === "DIV" && !c.hasAttribute("role"); })[0] || null;
    var track = progress.querySelector(":scope > [role=progressbar]");
    var fill = track ? track.firstElementChild : null;
    var spans = header ? [].slice.call(header.children).filter(function (c) { return c.tagName === "SPAN"; }) : [];
    var last = spans.length ? spans[spans.length - 1] : null;
    var valueEl = last && /^-?\d+%$/.test(last.textContent.trim()) ? last : null;
    var labelEl = spans.length && spans[0] !== valueEl ? spans[0] : null;
    return { header: header, track: track, fill: fill, label: labelEl, value: valueEl };
  }

  function shapeOf(progress) {
    var p = parts(progress);
    return (progress.getAttribute("data-progress-size") || "") + "/" + (p.label ? "l" : "-") + (p.value ? "v" : "-");
  }

  function row(win, el, ordinal, kind, phase, progress) {
    var parent = el.parentElement ? win.getComputedStyle(el.parentElement).display : "";
    var values = PROPS.map(function (p) { return value(win, el, p); });
    var pw = el.parentElement ? win.getComputedStyle(el.parentElement).width : "";
    return ["row", phase, ordinal, kind, shapeOf(progress) + "/" + layout(progress), context(progress), parent, values.join(US),
      "boxes=" + el.getClientRects().length + " parentWidth=" + pw].join("\t");
  }

  function measure(win, doc, phase) {
    var rows = [];
    [].forEach.call(doc.querySelectorAll("[data-progress]"), function (progress, index) {
      var n = index + 1, p = parts(progress);
      rows.push(row(win, progress, n, "progress", phase, progress));
      if (p.header) { rows.push(row(win, p.header, n, "header", phase, progress)); }
      if (p.label) { rows.push(row(win, p.label, n, "label", phase, progress)); }
      if (p.value) { rows.push(row(win, p.value, n, "value", phase, progress)); }
      if (p.track) { rows.push(row(win, p.track, n, "track", phase, progress)); }
      if (p.fill) { rows.push(row(win, p.fill, n, "fill", phase, progress)); }
    });
    return rows;
  }

  // Every class token on the Progress's own elements, sorted and unique.
  function classTokens(els) {
    var seen = {};
    els.forEach(function (el) {
      if (el) { (el.getAttribute("class") || "").split(/\s+/).forEach(function (t) { if (t) { seen[t] = 1; } }); }
    });
    return Object.keys(seen).sort().join(" ");
  }

  function shapes(doc) {
    return [].map.call(doc.querySelectorAll("[data-progress]"), function (progress, index) {
      var p = parts(progress);
      return ["shape", index + 1, attributeNames(progress),
        [].map.call(progress.children, function (c) { return c.tagName.toLowerCase(); }).join(","),
        p.header ? [].map.call(p.header.children, function (c) { return c.tagName.toLowerCase(); }).join(",") : "",
        p.track ? attributeNames(p.track) : "", p.track ? [].map.call(p.track.children, function (c) { return c.tagName.toLowerCase(); }).join(",") : "",
        p.fill ? attributeNames(p.fill) : "", p.track ? (p.track.getAttribute("aria-valuenow") || "") : "",
        p.fill ? (p.fill.getAttribute("style") || "") : "", p.value ? p.value.textContent.trim() : "",
        p.track ? (p.track.getAttribute("aria-label") || "") : "",
        classTokens([progress, p.header, p.label, p.value, p.track, p.fill])].join("\t");
    });
  }

  // Puts the legacy Tailwind classes back on the SAME elements: every element inside a Progress (and the Progress itself)
  // whose class attribute is exactly a migrated class value gets the legacy value of the same role.
  function flipToLegacy(doc, map) {
    [].forEach.call(doc.querySelectorAll("[data-progress], [data-progress] *"), function (el) {
      var current = el.getAttribute("class");
      if (current !== null && Object.prototype.hasOwnProperty.call(map, current)) { el.setAttribute("class", map[current]); }
    });
  }

  // What the page's own palette resolves to, read through a throwaway element, so a test can compare a Progress's
  // computed colours with what the active theme and skin actually define.
  function resolve(win, doc, declaration, property) {
    var el = doc.createElement("div");
    el.style.cssText = declaration;
    doc.body.appendChild(el);
    var resolved = win.getComputedStyle(el).getPropertyValue(property);
    doc.body.removeChild(el);
    return resolved;
  }

  function tokens(win, doc, id) {
    var out = [];
    [["slate", 200], ["slate", 700], ["slate", 300], ["slate", 400], ["slate", 500], ["blue", 600], ["blue", 500]].forEach(function (c) {
      var name = "--color-" + c[0] + "-" + c[1];
      out.push(["tok", id, name, resolve(win, doc, "color:var(" + name + ")", "color")].join("\t"));
    });
    return out;
  }

  // The legacy utilities are compiled as plain class names (flex, w-full, text-sm ...), and the page around the Progress
  // uses many of the same names. With Tailwind removed those elements are unstyled, so while the scoped legacy
  // stylesheet is linked they must not pick its rules up: the legacy names are taken off every element that is not
  // inside a Progress, and put back afterwards. Both renderings are then measured in the same unstyled page.
  function legacyTokenSet(map) {
    var set = {};
    Object.keys(map).forEach(function (k) { map[k].split(/\s+/).forEach(function (t) { set[t] = 1; }); });
    return set;
  }

  function detach(doc, set) {
    var saved = [];
    [].forEach.call(doc.querySelectorAll("[class]"), function (el) {
      if (el.closest("[data-progress]")) { return; }
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

  window.__ashProgress = { PROPS: PROPS, measure: measure, shapes: shapes, tokens: tokens };

  var probe = window.__ASH_PROBE;
  if (probe && probe.config) {
    window.addEventListener("load", function () {
      setTimeout(function () {
        var cfg = probe.config;
        var progresses = document.querySelectorAll("[data-progress]");
        var out = ["props\t" + PROPS.join(",")];
        out.push("meta\tprogress-count\t" + progresses.length);
        out.push("meta\tprogress-without-a-box\t" + [].filter.call(progresses, function (d) { return d.getClientRects().length === 0; }).length);
        out.push("meta\tdata-theme\t" + document.documentElement.getAttribute("data-theme"));
        out.push("meta\tdata-skin\t" + (document.documentElement.getAttribute("data-skin") || ""));
        out = out.concat(shapes(document));
        out = out.concat(tokens(window, document, "page"));
        out = out.concat(measure(window, document, cfg.phase || "ash"));
        function finish() {
          var pre = document.createElement("pre");
          pre.id = "ash-progress-results";
          pre.textContent = out.join("\n");
          document.body.appendChild(pre);
        }
        if (cfg.legacy) {
          // The legacy stylesheet is linked only AFTER the migrated rendering was measured, so that measurement
          // is exactly what production serves. Then the same elements get their legacy classes back.
          var saved = cfg.legacy.detach ? detach(document, legacyTokenSet(cfg.legacy.map)) : [];
          out.push("meta\tdetached-elements\t" + saved.length);
          var link = document.createElement("link");
          link.rel = "stylesheet";
          link.href = cfg.legacy.stylesheet;
          link.onload = function () {
            flipToLegacy(document, cfg.legacy.map);
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
