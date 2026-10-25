// TEST-ONLY (shared parity harness: ParitySession, ParityFrames, ParityProbe). Reads the browser's own computed styles
// and layout boxes for every element a fixture marks with data-parity, in the frame documents of a controlled oracle.
// It knows nothing about any component: the component's identity is only the value of data-parity (the row's kind) and
// data-parity-case (the row's variant). The markers exist in test fixtures only, never in product markup.
//
// Output lines, tab separated, in the same shape the per-component probes use (so ParityNormalizer and the row parser
// are shared):
//   props<TAB>name,name,...
//   row<TAB>phase<TAB>ordinal<TAB>kind<TAB>case/layout<TAB>context<TAB>parentDisplay<TAB>value US value ...<TAB>diagnostics
// US is the unit separator, \u001f. The ordinal is the element's 1-based position among [data-parity] elements in
// document order. The layout is the closest ancestor's data-layout (a fixture names its wrappers) or "page". Names that
// start with "@" are not computed styles but the element's box in its parent's coordinates, so a margin, a stretch or an
// offset shows up even where a computed value is "auto".
(function () {
  var US = "\u001f";
  var BASE_PROPS = ["display", "position", "color", "background-color",
    "border-top-width", "border-right-width", "border-bottom-width", "border-left-width",
    "border-top-style", "border-right-style", "border-bottom-style", "border-left-style",
    "border-top-color", "border-right-color", "border-bottom-color", "border-left-color",
    "border-top-left-radius", "border-top-right-radius", "border-bottom-right-radius", "border-bottom-left-radius",
    "box-shadow", "opacity", "outline-style", "padding-top", "padding-right", "padding-bottom", "padding-left",
    "margin-top", "margin-right", "margin-bottom", "margin-left", "width", "height", "min-width", "min-height",
    "max-width", "max-height", "box-sizing", "align-self", "justify-self", "flex-grow", "flex-shrink", "flex-basis",
    "overflow-x", "overflow-y", "vertical-align", "float", "clear", "unicode-bidi", "translate",
    "@x", "@y", "@w", "@h"];

  function propsWith(extra) {
    // Extra properties go before the box measurements so "@w" and "@h" stay last in every list.
    var named = BASE_PROPS.slice(0, BASE_PROPS.length - 4);
    return named.concat(extra || [], BASE_PROPS.slice(BASE_PROPS.length - 4));
  }

  function context(el) {
    var chain = [], p = el.parentElement;
    while (p && chain.length < 3) {
      var hook = "";
      for (var i = 0; i < p.attributes.length; i++) {
        var n = p.attributes[i].name;
        if (n.indexOf("data-") === 0 && n !== "data-theme" && n !== "data-skin" && n !== "data-layout") { hook = n.substring(5); break; }
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

  function row(win, el, ordinal, phase, props) {
    var parent = el.parentElement ? win.getComputedStyle(el.parentElement).display : "";
    var values = props.map(function (p) { return value(win, el, p); });
    // Diagnostic only, never compared: the layout above this element (display and size of its ancestors).
    var diag = "boxes=" + el.getClientRects().length + " above=";
    for (var up = el.parentElement, n = 0; up && n < 3; up = up.parentElement, n++) {
      var ucs = win.getComputedStyle(up);
      diag += up.tagName.toLowerCase() + "[" + (up.getAttribute("class") || "").substring(0, 40) + "]:" + ucs.display + ":" + ucs.width + "x" + ucs.height + ">";
    }
    return ["row", phase, ordinal, el.getAttribute("data-parity"), (el.getAttribute("data-parity-case") || "") + "/" + layout(el), context(el), parent,
      values.join(US), diag].join("\t");
  }

  function measure(win, doc, phase, extra) {
    var props = propsWith(extra);
    return [].map.call(doc.querySelectorAll("[data-parity]"), function (el, index) { return row(win, el, index + 1, phase, props); });
  }

  window.__ashParity = { propsWith: propsWith, measure: measure };
})();
