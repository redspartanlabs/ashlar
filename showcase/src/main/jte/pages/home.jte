@import com.redspartan.ashlar.icon.Icon
@import com.redspartan.ashlar.AshlarAssets

@param String contextPath

<%-- Every Ashlar call below is namespaced: `@template.ashlar.components.*`.
     Those templates are not in this module's source tree - they were unpacked
     from the Ashlar jar into build/jte-sources by the `assembleJteSources`
     task. If this page renders, the distribution model works. --%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Ashlar Showcase</title>
    <link rel="stylesheet" href="${contextPath}/css/app.css">
</head>
<body class="min-h-screen bg-slate-50 p-8">

<main class="mx-auto flex max-w-3xl flex-col gap-8">

    <div>
        <h1 class="text-xl font-semibold text-slate-900">Ashlar vertical slice</h1>
        <p class="mt-1 text-sm text-slate-500">
            Button and Icon, consumed from the <code class="rounded bg-slate-200 px-1 py-0.5 text-xs">com.redspartan:ashlar</code> artifact.
        </p>
    </div>

    <section class="rounded-xl border border-slate-200 bg-white p-6">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-slate-500">Variants</h2>
        <div class="mt-3 flex flex-wrap items-center gap-3">
            @template.ashlar.components.button(text = "Primary", type = "Primary")
            @template.ashlar.components.button(text = "Secondary", type = "Secondary")
            @template.ashlar.components.button(text = "Ghost", type = "Ghost")
            @template.ashlar.components.button(text = "Danger", type = "Danger")
        </div>
    </section>

    <section class="rounded-xl border border-slate-200 bg-white p-6">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-slate-500">Sizes</h2>
        <div class="mt-3 flex flex-wrap items-center gap-3">
            @template.ashlar.components.button(text = "Small", size = "Small")
            @template.ashlar.components.button(text = "Medium", size = "Medium")
            @template.ashlar.components.button(text = "Large", size = "Large")
        </div>
    </section>

    <section class="rounded-xl border border-slate-200 bg-white p-6">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-slate-500">Template composition</h2>
        <p class="mt-1 text-sm text-slate-500">
            These render an SVG because button.jte calls
            <code class="rounded bg-slate-200 px-1 py-0.5 text-xs">&#64;template.ashlar.components.icon(...)</code>
            - one Ashlar template resolving another, both from the jar.
        </p>
        <div class="mt-3 flex flex-wrap items-center gap-3">
            @template.ashlar.components.button(text = "With icon", icon = Icon.CHECK)
            @template.ashlar.components.button(text = "Trailing", icon = Icon.CHEVRON_RIGHT, iconPosition = "trailing")
            @template.ashlar.components.button(icon = Icon.SETTINGS, ariaLabel = "Settings", type = "Secondary")
        </div>
    </section>

    <section class="rounded-xl border border-slate-200 bg-white p-6">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-slate-500">States</h2>
        <div class="mt-3 flex flex-wrap items-center gap-3">
            @template.ashlar.components.button(text = "Disabled", disabled = true)
            @template.ashlar.components.button(text = "Loading", loading = true)
        </div>
    </section>

    <section class="rounded-xl border border-slate-200 bg-white p-6">
        <h2 class="text-xs font-semibold uppercase tracking-wide text-slate-500">Asset URLs</h2>
        <p class="mt-1 text-sm text-slate-500">
            Resolved by <code class="rounded bg-slate-200 px-1 py-0.5 text-xs">AshlarAssets</code> from this
            application's context path, not hardcoded in the component.
        </p>
        <dl class="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
            <dt class="font-medium text-slate-700">Context path</dt>
            <dd class="font-mono text-slate-600" data-probe-context>${contextPath.isEmpty() ? "(root)" : contextPath}</dd>
            <dt class="font-medium text-slate-700">Ashlar base</dt>
            <dd class="font-mono text-slate-600" data-probe-base>${AshlarAssets.basePath()}</dd>
            <dt class="font-medium text-slate-700">button.js</dt>
            <dd class="font-mono text-slate-600" data-probe-js>${AshlarAssets.js("button.js")}</dd>
        </dl>
    </section>

</main>
</body>
</html>
