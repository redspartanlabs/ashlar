// TEST-ONLY (Empty State migration parity). Reads the browser's own computed styles and layout boxes for every Empty State
// and the elements inside it, either in a frame document (controlled oracle) or on a REAL showcase page (injected by
// RealPageProxy; the page templates are not touched).
//
// Output lines, tab separated, written into a result element appended to the body:
//   props<TAB>name,name,...
//   row<TAB>phase<TAB>ordinal<TAB>kind<TAB>shape/layout<TAB>context<TAB>parentDisplay<TAB>value US value ...<TAB>diagnostics
//   shape<TAB>ordinal<TAB>wrapper attrs<TAB>child tags<TAB>icon attrs<TAB>svg attrs<TAB>title attrs<TAB>body attrs<TAB>action attrs<TAB>title text<TAB>svg aria-hidden<TAB>body children<TAB>body paragraphs<TAB>action children<TAB>class tokens
//   tok<TAB>id<TAB>name<TAB>value
//   meta<TAB>key<TAB>value
// US is the unit separator, \u001f. kind is wrapper, icon, glyph, title, body, body-p (a paragraph that is a direct child
// of the body), body-x (any other direct child of the body), action or action-x (a direct child of the action). The
// ordinal is the Empty State's 1-based position among [data-empty-state] elements in document order, so every row of one
// Empty State shares it. The shape is three letters, "i" for an icon, "b" for a body and "a" for an action, each "-" when
// absent ("iba" is all three), and the layout is the closest ancestor's data-layout (a controlled frame names its
// wrappers) or "page". Names that start with "@" are not computed styles but the element's box in its parent's
// coordinates.
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
    "fill", "stroke", "stroke-width",
    "@x", "@y", "@w", "@h"];

  function attributeNames(el) {
    return el ? [].map.call(el.attributes, function (a) { return a.name; }).sort().join(",") : "";
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

  function has(el, token) {
    return (" " + (el.getAttribute("class") || "") + " ").indexOf(" " + token + " ") >= 0;
  }

  // The parts of one Empty State, found structurally (the classes differ between the migrated and the legacy rendering;
  // the body and the action are told apart by the one class token each of them carries in either rendering).
  function parts(es) {
    var kids = [].slice.call(es.children);
    var first = kids[0];
    var icon = first && first.tagName === "DIV" && first.firstElementChild && first.firstElementChild.tagName.toLowerCase() === "svg" ? first : null;
    var title = kids.filter(function (c) { return c.tagName === "H3"; })[0] || null;
    var body = kids.filter(function (c) { return c.tagName === "DIV" && c !== icon && (has(c, "ash-empty-state__body") || has(c, "max-w-sm")); })[0] || null;
    var action = kids.filter(function (c) { return c.tagName === "DIV" && c !== icon && (has(c, "ash-empty-state__action") || has(c, "justify-center")); })[0] || null;
    return { icon: icon, glyph: icon ? icon.firstElementChild : null, title: title, body: body, action: action };
  }

  function shapeOf(es) {
    var p = parts(es);
    return (p.icon ? "i" : "-") + (p.body ? "b" : "-") + (p.action ? "a" : "-");
  }

  function row(win, el, ordinal, kind, phase, es) {
    var parent = el.parentElement ? win.getComputedStyle(el.parentElement).display : "";
    var values = PROPS.map(function (p) { return value(win, el, p); });
    var pw = el.parentElement ? win.getComputedStyle(el.parentElement).width : "";
    return ["row", phase, ordinal, kind, shapeOf(es) + "/" + layout(es), context(es), parent, values.join(US),
      "boxes=" + el.getClientRects().length + " parentWidth=" + pw].join("\t");
  }

  function measure(win, doc, phase) {
    var rows = [];
    [].forEach.call(doc.querySelectorAll("[data-empty-state]"), function (es, index) {
      var n = index + 1, p = parts(es);
      rows.push(row(win, es, n, "wrapper", phase, es));
      if (p.icon) { rows.push(row(win, p.icon, n, "icon", phase, es)); }
      if (p.glyph) { rows.push(row(win, p.glyph, n, "glyph", phase, es)); }
      if (p.title) { rows.push(row(win, p.title, n, "title", phase, es)); }
      if (p.body) {
        rows.push(row(win, p.body, n, "body", phase, es));
        [].forEach.call(p.body.children, function (c) { rows.push(row(win, c, n, c.tagName === "P" ? "body-p" : "body-x", phase, es)); });
      }
      if (p.action) {
        rows.push(row(win, p.action, n, "action", phase, es));
        [].forEach.call(p.action.children, function (c) { rows.push(row(win, c, n, "action-x", phase, es)); });
      }
    });
    return rows;
  }

  // Every class token on the Empty State's own elements (not the caller's content), sorted and unique.
  function classTokens(els) {
    var seen = {};
    els.forEach(function (el) {
      if (el) { (el.getAttribute("class") || "").split(/\s+/).forEach(function (t) { if (t) { seen[t] = 1; } }); }
    });
    return Object.keys(seen).sort().join(" ");
  }

  function shapes(doc) {
    return [].map.call(doc.querySelectorAll("[data-empty-state]"), function (es, index) {
      var p = parts(es);
      return ["shape", index + 1, attributeNames(es),
        [].map.call(es.children, function (c) { return c.tagName.toLowerCase(); }).join(","),
        attributeNames(p.icon), attributeNames(p.glyph), attributeNames(p.title), attributeNames(p.body), attributeNames(p.action),
        p.title ? p.title.textContent.trim() : "",
        p.glyph ? (p.glyph.getAttribute("aria-hidden") || "") : "",
        p.body ? p.body.children.length : 0,
        p.body ? [].filter.call(p.body.children, function (c) { return c.tagName === "P"; }).length : 0,
        p.action ? p.action.children.length : 0,
        classTokens([es, p.icon, p.glyph, p.title, p.body, p.action])].join("\t");
    });
  }

  // Puts the legacy Tailwind classes back on the SAME elements: every element inside an Empty State (and the Empty State
  // itself) whose class attribute is exactly a migrated class value gets the legacy value of the same role.
  function flipToLegacy(doc, map) {
    [].forEach.call(doc.querySelectorAll("[data-empty-state], [data-empty-state] *"), function (el) {
      var current = el.getAttribute("class");
      if (current !== null && Object.prototype.hasOwnProperty.call(map, current)) { el.setAttribute("class", map[current]); }
    });
  }

  // A Tailwind-removed page cannot style the caller's own content (a Button, say, is still Tailwind's), and the legacy
  // stylesheet's scoped preflight would reach it in only one of the two renderings. So when asked, the content of every
  // action is replaced, BEFORE anything is measured, by an inert box that both renderings share.
  function neutralize(doc) {
    var n = 0;
    [].forEach.call(doc.querySelectorAll("[data-empty-state]"), function (es) {
      var action = parts(es).action;
      if (action) {
        action.innerHTML = "<span style=\"display:inline-block;box-sizing:border-box;width:100px;height:36px\"></span>";
        n++;
      }
    });
    return n;
  }

  // What the page's own palette and skin resolve to, read through throwaway elements, so a test can compare an Empty
  // State's computed colours with what the active theme and skin actually define.
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
    [["slate", 100], ["slate", 400], ["slate", 500], ["slate", 900]].forEach(function (c) {
      var name = "--color-" + c[0] + "-" + c[1];
      out.push(["tok", id, name, resolve(win, doc, "color:var(" + name + ")", "color")].join("\t"));
    });
    // What a skin does to ANY [data-empty-state] (Foundry's shadow) and to a paragraph inside one (Harbor's line height),
    // read from a throwaway Empty State of the skin's own selectors' shape, with no Ashlar class on it.
    var probe = doc.createElement("div");
    probe.setAttribute("data-empty-state", "");
    probe.style.cssText = "position:absolute;left:-9999px;font-size:14px";
    probe.innerHTML = "<div><p>x</p></div>";
    doc.body.appendChild(probe);
    out.push(["tok", id, "skin-shadow", win.getComputedStyle(probe).getPropertyValue("box-shadow")].join("\t"));
    out.push(["tok", id, "skin-p-line-height", win.getComputedStyle(probe.querySelector("p")).getPropertyValue("line-height")].join("\t"));
    out.push(["tok", id, "skin-p-font-size", win.getComputedStyle(probe.querySelector("p")).getPropertyValue("font-size")].join("\t"));
    doc.body.removeChild(probe);
    return out;
  }

  // The legacy utilities are compiled as plain class names (flex, text-sm, px-6 ...), and the page around the Empty State
  // uses many of the same names. With Tailwind removed those elements are unstyled, so while the scoped legacy stylesheet
  // is linked they must not pick its rules up: the legacy names are taken off every element that is not inside an Empty
  // State, and put back afterwards. Both renderings are then measured in the same unstyled page.
  function legacyTokenSet(map) {
    var set = {};
    Object.keys(map).forEach(function (k) { map[k].split(/\s+/).forEach(function (t) { set[t] = 1; }); });
    return set;
  }

  function detach(doc, set) {
    var saved = [];
    [].forEach.call(doc.querySelectorAll("[class]"), function (el) {
      if (el.closest("[data-empty-state]")) { return; }
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

  window.__ashEmptyState = { PROPS: PROPS, measure: measure, shapes: shapes, tokens: tokens };

  var probe = window.__ASH_PROBE;
  if (probe && probe.config) {
    window.addEventListener("load", function () {
      setTimeout(function () {
        var cfg = probe.config;
        var states = document.querySelectorAll("[data-empty-state]");
        var out = ["props\t" + PROPS.join(",")];
        if (cfg.neutralize) { out.push("meta\tneutralized-actions\t" + neutralize(document)); }
        out.push("meta\tempty-state-count\t" + states.length);
        out.push("meta\tempty-state-without-a-box\t" + [].filter.call(states, function (d) { return d.getClientRects().length === 0; }).length);
        out.push("meta\tdata-theme\t" + document.documentElement.getAttribute("data-theme"));
        out.push("meta\tdata-skin\t" + (document.documentElement.getAttribute("data-skin") || ""));
        out = out.concat(shapes(document));
        out = out.concat(tokens(window, document, "page"));
        out = out.concat(measure(window, document, cfg.phase || "ash"));
        function finish() {
          var pre = document.createElement("pre");
          pre.id = "ash-empty-state-results";
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
