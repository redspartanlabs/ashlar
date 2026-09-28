@param String title
@param String contextPath
@param gg.jte.Content content

<%-- The one HTML shell every showcase page renders through.

     Deliberately minimal compared to the laboratory's own layout: that one
     composes a Navbar, a skin-selector Dropdown Menu and a Theme Toggle, none
     of which have migrated yet. Rather than stub those out or pull unrelated
     components into this batch, this shell carries only what the migrated
     components actually need, and grows as they arrive.

     `contextPath` is threaded through because this application runs under a
     servlet context path. Note the split: the showcase resolves its OWN
     stylesheet this way, while Ashlar's component assets resolve themselves
     through AshlarAssets inside the component templates. Neither knows about
     the other. --%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} - Ashlar</title>
    <link rel="stylesheet" href="${contextPath}/css/app.css">
</head>
<body class="min-h-screen bg-slate-50">

<main class="mx-auto flex max-w-6xl flex-col gap-10 px-4 py-10">
    ${content}
</main>

</body>
</html>
