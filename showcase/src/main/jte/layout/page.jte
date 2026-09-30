@param String title
@param String contextPath
@param gg.jte.Content content
@param gg.jte.Content afterMain = null

<%-- The one HTML shell every showcase page renders through.

     Deliberately minimal compared to the laboratory's own layout: that one
     also composes a Navbar and a skin-selector Dropdown Menu, neither of
     which is this showcase's concern (Navbar is now migrated and demonstrated
     on its own page; the skin system is a playground-only experiment that
     was never part of the migration contract). This shell carries only what
     the migrated components actually need, and grows as they arrive.

     `contextPath` is threaded through because this application runs under a
     servlet context path. Note the split: the showcase resolves its OWN
     stylesheet this way, while Ashlar's component assets resolve themselves
     through AshlarAssets inside the component templates. Neither knows about
     the other.

     `afterMain` exists only for content that must sit outside <main> in the
     DOM (toast-region's own fixed-position stack, on the Toast showcase
     page) - everything else belongs in `content`.

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
        (which also persists it) or by setting `localStorage.skin` directly. --%>
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

<main class="mx-auto flex max-w-6xl flex-col gap-10 px-4 py-10">
    ${content}
</main>

@if (afterMain != null)
    ${afterMain}
@endif
</body>
</html>
