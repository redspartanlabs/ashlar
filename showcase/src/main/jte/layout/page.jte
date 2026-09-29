@param String title
@param String contextPath
@param gg.jte.Content content

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

     THEME PRE-PAINT BOOTSTRAP. Sets `data-theme` on <html> before the
     stylesheet can paint anything, so the page never flashes the wrong
     theme - copied faithfully from the playground's own layout/page.jte
     (skin-switching logic omitted: Ashlar has no theme/skin system to carry
     over, only light/dark). Must be inline (a linked `<script src>` would
     arrive after network latency, too late) and must run before
     `<link rel="stylesheet">`, so it comes first here. theme-toggle.jte's own
     `theme.js` does not repeat this logic - by design, per the migration
     contract, Theme Toggle only ever reflects and changes state this script
     already established; it is not responsible for initializing it. This is
     showcase/application-layout responsibility, not a component concern. --%>
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
        })();
    </script>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} - Ashlar</title>
    <link rel="stylesheet" href="${contextPath}/css/app.css">
</head>
<body class="min-h-screen bg-slate-50 dark:bg-slate-950">

<main class="mx-auto flex max-w-6xl flex-col gap-10 px-4 py-10">
    ${content}
</main>

</body>
</html>
