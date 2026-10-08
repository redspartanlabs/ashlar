@import dev.redspartanlabs.ashlar.select.SelectOption
@import java.util.List

@param String title
@param String contextPath
@param gg.jte.Content content
@param gg.jte.Content afterMain = null
@param String description = "Ashlar is a server-rendered UI component system for Java and Kotlin, built with JTE."
@param boolean componentsActive = true

<%-- The one HTML shell every showcase page renders through.

     SITE CHROME. The header and footer below are real Ashlar components
     (Navbar, Theme Toggle, Footer), not hand-rolled markup - this shell
     dogfoods the library it showcases rather than reimplementing a masthead
     next to it. Composed exactly per each component's own documented
     catalog usage (see pages/components/navbar.jte and footer.jte):
     Navbar's `items` carries plain `nav-link` entries, its `actions` slot
     carries the Theme Toggle (the one place its own doc comment says such a
     control belongs), and Footer wraps a caller-composed copyright/links row
     with ordinary flex utilities.

     "Components" is the only nav-link today because it is the only
     showcase section that exists - it points at `/components`, the
     catalog index, not `/` (the homepage is its own section now). A
     "Themes" destination stays deferred: no `/themes` page exists yet.
     `componentsActive` defaults `true` so all 50 existing component pages
     keep marking it `current` with no edits of their own; the homepage is
     the one caller that passes `false`.

     GitHub links to the project's own declared origin (see ashlar's
     build.gradle.kts publishing block, `scm.url`), not a guessed address.
     `nav-link.jte`'s API has no `target`/`rel` param, so - like every other
     entry - it opens in the same tab; that is a minor documented limitation
     of reusing the existing component as-is, not a new one introduced here.

     `contextPath` is threaded through because this application runs under a
     servlet context path. Note the split: the showcase resolves its OWN
     stylesheet this way, while Ashlar's component assets resolve themselves
     through AshlarAssets inside the component templates. Neither knows about
     the other.

     `afterMain` exists only for content that must sit outside <main> in the
     DOM (toast-region's own fixed-position stack, on the Toast showcase
     page) - everything else belongs in `content`.

     `description` feeds the page's one meta description. Every component
     page is the same generic library blurb by default; the homepage is the
     one caller that passes its own, more specific sentence.

     THEME PRE-PAINT BOOTSTRAP. Sets `data-theme` on <html> before the
     stylesheet can paint anything, so the page never flashes the wrong
     theme - copied faithfully from the playground's own layout/page.jte.
     Must be inline (a linked `<script src>` would arrive after network
     latency, too late) and must run before `<link rel="stylesheet">`, so it
     comes first here. theme-toggle.jte's own `theme.js` does not repeat this
     logic - by design, per the migration contract, Theme Toggle only ever
     reflects and changes state this script already established; it is not
     responsible for initializing it. This is showcase/application-layout
     responsibility, not a component concern.

     SKIN BOOTSTRAP. The same pre-paint treatment for the ten ported skins
     (theme-axiom.css and friends below) - adapted, not copied, from the
     playground's version. Two deliberate differences from the playground:

     1. No default skin. The playground defaults an unset/invalid choice to
        "veridian" because that experiment IS the playground's demo
        appearance. Ashlar's skins are opt-in library assets, so an unset or
        invalid choice here leaves `data-skin` absent entirely, which is what
        keeps every skin's CSS inert and the default Ashlar styling exactly
        as it is without one.
     2. No switcher UI wiring. The playground's script also seeds a
        `?skin=` link-interception menu (skin.js, built on its own Dropdown
        Menu instance in the navbar). That menu is Showcase homepage/chrome
        work, deferred along with the rest of the homepage redesign - this
        bootstrap only establishes the attribute a later menu would drive.
        Until then, a skin is reachable by visiting `?skin=<name>` once
        (which also persists it) or by setting `localStorage.skin` directly.

     DOCUMENTATION-SITE POLISH PASS: the switcher UI deferred above now
     exists, as a real Ashlar Select rather than a new control - composed
     into Navbar's own `actions` slot beside Theme Toggle, the exact slot
     that stays visible in the top bar at every width (Navbar never
     duplicates `actions` into its mobile drawer), so no separate
     mobile-drawer wiring was needed. skin-select.js (a new showcase-only
     asset, not an Ashlar component file) reads this same rendered
     <select>'s own option values as its list of valid skins, rather than
     hardcoding a second list next to this one. Select still owns none of
     the actual skin state: it only ever reflects `data-skin` (corrected
     into the UI once the page has loaded, the same category of problem
     Theme Toggle already solves for light/dark) and, on a real selection,
     writes through to the exact same `localStorage.skin`/`data-skin` pair
     this bootstrap script establishes - one source of truth, not two.

     `clearable = true` represents the no-skin/default-Ashlar state as a
     real clear affordance (empty value, same as Select's own internal
     placeholder option) rather than an extra selectable row invented for
     this - `placeholder = "Default"` is what the trigger shows whenever
     nothing is selected, including on first load before skin-select.js
     has corrected it.

     That clear button and the chevron used to collide: audited and fixed
     directly in ashlar/select.jte (see that file's own doc comment) once
     the overlap turned out to be a real, reusable defect in the
     component's default markup - affecting every `clearable = true`
     caller, not only this narrowed navbar one - rather than something to
     work around here with a showcase-only replacement.

     Select's own stacked-form box model (a block label, `mb-6`, a
     `max-w-sm` trigger) does not fit a navbar action row, so the wrapper
     below applies the exact technique filter-bar.jte already uses for the
     same mismatch - `[&>div]:mb-0` strips the trailing margin, and the
     real label is visually hidden (`sr-only`, not removed - the accessible
     name still comes from it) in favor of one small static "Theme" caption
     so sighted and assistive-tech users each hear/see the word exactly
     once. The trigger itself is further narrowed via its own stable
     `data-select-trigger` hook - `w-auto` lets it shrink to its real
     content (the selected skin's label plus the chevron and, now, the
     clear button) instead of filling whatever width an ancestor offers,
     and `max-w-36` stays only as a ceiling for a hypothetically long
     label. No override of the trigger's own `pr-8` padding is needed or
     applied here - that reserved band is exactly what the select.jte fix
     above now uses correctly. --%>
!{List<SelectOption> skinOptions = List.of(
    SelectOption.of("axiom", "Axiom"),
    SelectOption.of("civic", "Civic"),
    SelectOption.of("fathom", "Fathom"),
    SelectOption.of("foundry", "Foundry"),
    SelectOption.of("harbor", "Harbor"),
    SelectOption.of("keystone", "Keystone"),
    SelectOption.of("ledger", "Ledger"),
    SelectOption.of("sterling", "Sterling"),
    SelectOption.of("summit", "Summit"),
    SelectOption.of("veridian", "Veridian")
);}
<!DOCTYPE html>
<html lang="en">
<head>
    <script>
        (function () {
            var VALID_THEMES = ["light", "dark"];
            var theme = "light";
            try {
                var stored = window.localStorage.getItem("theme");
                if (VALID_THEMES.indexOf(stored) !== -1) {
                    theme = stored;
                } else if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
                    theme = "dark";
                }
            } catch (error) {
                theme = "light";
            }
            document.documentElement.setAttribute("data-theme", theme);

            var VALID_SKINS = ["axiom", "civic", "fathom", "foundry", "harbor", "keystone", "ledger", "sterling", "summit", "veridian"];
            try {
                var requestedSkin = new URLSearchParams(window.location.search).get("skin");
                var storedSkin = window.localStorage.getItem("skin");
                var skin = null;
                if (VALID_SKINS.indexOf(requestedSkin) !== -1) {
                    skin = requestedSkin;
                    window.localStorage.setItem("skin", skin);
                } else if (VALID_SKINS.indexOf(storedSkin) !== -1) {
                    skin = storedSkin;
                }
                if (skin !== null) {
                    document.documentElement.setAttribute("data-skin", skin);
                }
            } catch (error) {
                // Default Ashlar styling applies whenever skin selection fails.
            }
        })();
    </script>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="${description}">
    <title>${title} - Ashlar</title>
    <link rel="stylesheet" href="${contextPath}/css/app.css">
    <%-- Ported skins. Each is inert unless <html> carries its own data-skin
         value (set above), so every link can stay in place regardless of
         which skin, if any, is active - removing one link removes that skin
         entirely without affecting the others or the default library. --%>
    <link rel="stylesheet" href="${contextPath}/css/theme-axiom.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-civic.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-fathom.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-foundry.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-harbor.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-keystone.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-ledger.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-sterling.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-summit.css">
    <link rel="stylesheet" href="${contextPath}/css/theme-veridian.css">
</head>
<body class="min-h-screen bg-slate-50 dark:bg-slate-950">

<%-- The navbar is lifted into its own stacking context above the sticky
     "On this page" outline bar (sm:sticky, z-10) that follows it on component
     pages. The skin Select's open listbox is a descendant of the navbar and
     is z-10 itself; without this, the outline bar - later in the document,
     same z-index - painted over the lower options. The navbar is not
     sticky, so nothing else changes: it still scrolls away normally. --%>
<div class="relative z-20">
@template.ashlar.components.navbar(
brand = @`<a href="${contextPath}/" class="text-lg font-semibold text-slate-900 dark:text-slate-100">Ashlar</a>`,
items = @`
    @template.ashlar.components.nav-link(label = "Components", href = contextPath + "/components", current = componentsActive)
    @template.ashlar.components.nav-link(label = "GitHub", href = "https://github.com/redspartanlabs/ashlar")
`,
actions = @`
    @template.ashlar.components.theme-toggle()
    <div class="flex items-center gap-2">
        <span class="text-sm font-medium text-slate-700 dark:text-slate-300" aria-hidden="true">Theme</span>
        <div class="[&>div]:mb-0 [&_label]:sr-only [&_[data-select-trigger]]:w-auto [&_[data-select-trigger]]:max-w-36" data-skin-select>
            @template.ashlar.components.select(
            name = "skin",
            label = "Theme",
            options = skinOptions,
            placeholder = "Default"
            )
        </div>
    </div>
`
)
</div>

<script src="${contextPath}/js/skin-select.js"></script>

<main class="mx-auto flex max-w-6xl flex-col gap-10 px-4 py-10">
    ${content}
</main>

@if (afterMain != null)
    ${afterMain}
@endif

@template.ashlar.utilities.footer(
content = @`
    <div class="flex flex-col items-start gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p>&copy; 2026 RedSpartan Labs.</p>
        <div class="flex flex-wrap items-center gap-4">
            <a href="${contextPath}/components" class="hover:text-slate-700 hover:underline underline-offset-2 dark:hover:text-slate-200">Components</a>
            <a href="https://github.com/redspartanlabs/ashlar" class="hover:text-slate-700 hover:underline underline-offset-2 dark:hover:text-slate-200">GitHub</a>
        </div>
    </div>
`
)
</body>
</html>
