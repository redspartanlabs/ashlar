// TEST-ONLY (Alert migration parity). Reads the browser's own computed styles for every Alert and the elements
// inside it, either in a frame document (controlled oracle) or on a REAL showcase page (injected by RealPageProxy;
// the page templates are not touched).
//
// Output lines, tab separated, written into a result element appended to the body:
//   props<TAB>name,name,...
//   row<TAB>phase<TAB>ordinal<TAB>kind<TAB>variant/shape<TAB>context<TAB>parentDisplay<TAB>value US value ...<TAB>diagnostics
//   shape<TAB>ordinal<TAB>tag<TAB>attributes<TAB>child tags<TAB>svg attributes<TAB>svg aria-hidden<TAB>text<TAB>content child tags<TAB>label text<TAB>title tag<TAB>class tokens
//   meta<TAB>key<TAB>value
// US is the unit separator, \u001f. kind is alert, icon, content, label, title, body or p. The ordinal is the
// Alert's 1-based position among [data-alert] elements in document order, so every row of one Alert shares it.
// An Alert is never hidden on any showcase page; an Alert without a layout box is reported, never revealed.
(function () {
  var US = "\u001f";
  var PROPS = ["display", "position", "color", "background-color",
    "border-top-width", "border-right-width", "border-bottom-width", "border-left-width",
    "border-top-style", "border-right-style", "border-bottom-style", "border-left-style",
    "border-top-color", "border-right-color", "border-bottom-color", "border-left-color",
    "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius",
    "box-shadow", "font-family", "font-size", "font-weight", "line-height", "letter-spacing", "text-transform",
    "text-decoration-line", "font-variant-numeric", "padding-top", "padding-right", "padding-bottom", "padding-left",
    "margin-top", "margin-right", "margin-bottom", "margin-left", "width", "height", "min-width", "opacity",
    "outline-style", "box-sizing", "translate", "flex-direction", "flex-grow", "flex-shrink", "flex-basis",
    "align-items", "align-self", "justify-content", "gap", "white-space", "overflow-x", "overflow-y", "overflow-wrap",
    "vertical-align", "clip-path", "stroke", "fill", "float", "flex-wrap", "max-width"];

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

  // The parts of one Alert, found structurally (the classes differ between the migrated and the legacy rendering).
  function parts(alert) {
    var svg = alert.querySelector(":scope > svg");
    var content = alert.querySelector(":scope > div");
    var label = content ? content.querySelector(":scope > span") : null;
    var title = content ? content.querySelector(":scope > h3") : null;
    var body = content ? [].filter.call(content.children, function (c) { return c.tagName === "DIV"; })[0] || null : null;
    return { svg: svg, content: content, label: label, title: title, body: body };
  }

  function shapeOf(alert) {
    var p = parts(alert);
    return (p.title ? "t" : "-") + (p.body ? "b" : "-");
  }

  function row(win, el, ordinal, kind, phase, alert) {
    var cs = win.getComputedStyle(el);
    var parent = el.parentElement ? win.getComputedStyle(el.parentElement).display : "";
    var values = PROPS.map(function (p) { return cs.getPropertyValue(p); });
    // Diagnostic only, never compared: the layout above this element (display and width of its ancestors).
    var diag = "boxes=" + el.getClientRects().length + " above=";
    for (var up = el.parentElement, n = 0; up && n < 4; up = up.parentElement, n++) {
      var ucs = win.getComputedStyle(up);
      diag += up.tagName.toLowerCase() + "[" + (up.getAttribute("class") || "").substring(0, 40) + "]:" + ucs.display + ":" + ucs.width + ">";
    }
    return ["row", phase, ordinal, kind, (alert.getAttribute("data-alert-variant") || "") + "/" + shapeOf(alert), context(alert), parent,
      values.join(US), diag].join("\t");
  }

  function measure(win, doc, phase) {
    var rows = [];
    [].forEach.call(doc.querySelectorAll("[data-alert]"), function (alert, index) {
      var n = index + 1, p = parts(alert);
      rows.push(row(win, alert, n, "alert", phase, alert));
      if (p.svg) { rows.push(row(win, p.svg, n, "icon", phase, alert)); }
      if (p.content) { rows.push(row(win, p.content, n, "content", phase, alert)); }
      if (p.label) { rows.push(row(win, p.label, n, "label", phase, alert)); }
      if (p.title) { rows.push(row(win, p.title, n, "title", phase, alert)); }
      if (p.body) {
        rows.push(row(win, p.body, n, "body", phase, alert));
        [].forEach.call(p.body.querySelectorAll(":scope > p"), function (para) { rows.push(row(win, para, n, "p", phase, alert)); });
      }
    });
    return rows;
  }

  // Every class token on the Alert's own elements (not on whatever a caller put inside the body), sorted and unique.
  function classTokens(els) {
    var seen = {};
    els.forEach(function (el) {
      if (el) { (el.getAttribute("class") || "").split(/\s+/).forEach(function (t) { if (t) { seen[t] = 1; } }); }
    });
    return Object.keys(seen).sort().join(" ");
  }

  function shapes(doc) {
    var out = [];
    [].forEach.call(doc.querySelectorAll("[data-alert]"), function (alert, index) {
      var p = parts(alert);
      out.push(["shape", index + 1, alert.tagName.toLowerCase(), attributeNames(alert),
        [].map.call(alert.children, function (c) { return c.tagName.toLowerCase(); }).join(","),
        p.svg ? attributeNames(p.svg) : "", p.svg ? (p.svg.getAttribute("aria-hidden") || "") : "",
        alert.textContent.replace(/\s+/g, " ").trim(),
        p.content ? [].map.call(p.content.children, function (c) { return c.tagName.toLowerCase(); }).join(",") : "",
        p.label ? p.label.textContent.trim() : "",
        p.title ? p.title.tagName.toLowerCase() : "",
        classTokens([alert, p.svg, p.content, p.label, p.title, p.body])].join("\t"));
    });
    return out;
  }

  // Puts the legacy Tailwind classes back on the SAME elements (the icon keeps the classes icon.jte gives it).
  function flipToLegacy(doc, roles) {
    [].forEach.call(doc.querySelectorAll("[data-alert]"), function (alert) {
      var v = alert.getAttribute("data-alert-variant");
      var p = parts(alert);
      alert.setAttribute("class", roles.container[v]);
      p.svg.setAttribute("class", p.svg.getAttribute("class").replace(/\bash-alert__icon\b/g, roles.icon[v]));
      p.content.setAttribute("class", roles.content[""]);
      p.label.setAttribute("class", roles.label[""]);
      if (p.title) { p.title.setAttribute("class", roles.title[v]); }
      if (p.body) { p.body.setAttribute("class", (p.title ? roles["body-titled"] : roles["body-plain"])[v]); }
    });
  }

  // What the page's own palette and radius variables resolve to, read through a throwaway element, so a test can
  // compare an Alert's computed colours and radius with what the active skin actually defines.
  function resolve(win, doc, declaration, property) {
    var el = doc.createElement("div");
    el.style.cssText = declaration;
    doc.body.appendChild(el);
    var value = win.getComputedStyle(el).getPropertyValue(property);
    doc.body.removeChild(el);
    return value;
  }

  function tokens(win, doc, id) {
    var out = [];
    ["sky", "green", "amber", "red"].forEach(function (hue) {
      [50, 200, 600, 900].forEach(function (shade) {
        var name = "--color-" + hue + "-" + shade;
        out.push(["tok", id, name, resolve(win, doc, "color:var(" + name + ")", "color")].join("\t"));
      });
    });
    out.push(["tok", id, "--radius-lg", resolve(win, doc, "border-top-left-radius:var(--radius-lg)", "border-top-left-radius")].join("\t"));
    out.push(["tok", id, "--radius-lg-raw", win.getComputedStyle(doc.documentElement).getPropertyValue("--radius-lg").trim()].join("\t"));
    return out;
  }

  // The legacy utilities are compiled as plain class names (flex, p-4, text-sm ...), and the page around the Alert uses
  // many of the same names. With Tailwind removed those elements are unstyled, so while the scoped legacy stylesheet
  // is linked they must not pick its rules up: the legacy names are taken off every element that is not one of an
  // Alert's own six, and put back afterwards. Both renderings are then measured in the same unstyled page.
  function legacyTokenSet(roles) {
    var set = {};
    Object.keys(roles).forEach(function (role) {
      Object.keys(roles[role]).forEach(function (v) { roles[role][v].split(/\s+/).forEach(function (t) { set[t] = 1; }); });
    });
    return set;
  }

  function detach(doc, set) {
    var own = [];
    [].forEach.call(doc.querySelectorAll("[data-alert]"), function (alert) {
      var p = parts(alert);
      own.push(alert, p.svg, p.content, p.label, p.title, p.body);
    });
    var saved = [];
    [].forEach.call(doc.querySelectorAll("[class]"), function (el) {
      if (own.indexOf(el) >= 0) { return; }
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

  window.__ashAlert = { PROPS: PROPS, measure: measure, shapes: shapes, tokens: tokens };

  var probe = window.__ASH_PROBE;
  if (probe && probe.config) {
    window.addEventListener("load", function () {
      setTimeout(function () {
        var cfg = probe.config;
        var alerts = document.querySelectorAll("[data-alert]");
        var out = ["props\t" + PROPS.join(",")];
        out.push("meta\talert-count\t" + alerts.length);
        out.push("meta\talerts-without-a-box\t" + [].filter.call(alerts, function (a) { return a.getClientRects().length === 0; }).length);
        out.push("meta\ticon-is-first-svg\t" + [].filter.call(alerts, function (a) {
          return a.querySelector(":scope > svg:first-of-type") === parts(a).svg;
        }).length);
        out.push("meta\tdata-theme\t" + document.documentElement.getAttribute("data-theme"));
        out.push("meta\tdata-skin\t" + (document.documentElement.getAttribute("data-skin") || ""));
        out = out.concat(shapes(document));
        out = out.concat(tokens(window, document, "page"));
        out = out.concat(measure(window, document, cfg.phase || "ash"));
        function finish() {
          var pre = document.createElement("pre");
          pre.id = "ash-alert-results";
          pre.textContent = out.join("\n");
          document.body.appendChild(pre);
        }
        if (cfg.legacy) {
          // The legacy stylesheet is linked only AFTER the migrated rendering was measured, so that measurement
          // is exactly what production serves. Then the same elements get their legacy classes back.
          var saved = cfg.legacy.detach ? detach(document, legacyTokenSet(cfg.legacy.roles)) : [];
          out.push("meta	detached-elements	" + saved.length);
          var link = document.createElement("link");
          link.rel = "stylesheet";
          link.href = cfg.legacy.stylesheet;
          link.onload = function () {
            flipToLegacy(document, cfg.legacy.roles);
            out = out.concat(measure(window, document, "legacy"));
            out.push("meta	detach-restore-mismatch	" + reattach(saved));
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
