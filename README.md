# Ashlar

[![CI](https://github.com/redspartanlabs/ashlar/actions/workflows/ci.yml/badge.svg)](https://github.com/redspartanlabs/ashlar/actions/workflows/ci.yml)

Ashlar is a server-rendered UI component system for Java and Kotlin
applications that use [JTE](https://jte.gg). Components are JTE templates that
render real HTML on the server; components that need behaviour load a small
JavaScript module of their own. Ashlar ships a stylesheet of its own, which
styles its CSS foundation and the components migrated to it so far (see
[Styling](#styling)); the remaining components are still styled with Tailwind
CSS.

It provides 61 components and 9 layout utilities, each documented with live
examples in the showcase application (53 pages; see [Building](#building) to
run it).

A RedSpartan Labs project. Licensed under the [Apache License 2.0](LICENSE).

## Modules

| Module | Published | Purpose |
| --- | --- | --- |
| `ashlar` | `dev.redspartan:ashlar` | The library: Java support classes, JTE templates, JavaScript assets, Ashlar's stylesheet (`ashlar.css`) and a generated Tailwind class list. |
| `showcase` | no | A Spring Boot application that consumes Ashlar exactly as an external consumer would. |

## Consuming Ashlar

JTE's build plugins generate code from `.jte` files **on the filesystem** — they
cannot read templates from a jar on the compile classpath. Ashlar therefore
ships its template source inside the artifact, and a consumer's build unpacks
it into the JTE source root.

Ashlar requires Java 21 or newer and declares JTE 3.2.4 as a dependency; styling
still needs Tailwind CSS v4 for the components that have not yet moved to
Ashlar's own CSS (see [Styling](#styling)). A Kotlin application uses the same artifact and the same
`.jte` templates.

Three additions to a consumer's `build.gradle.kts`, which assumes the
[JTE Gradle plugin](https://jte.gg/gradle-plugin/) (`gg.jte.gradle`) is already
applied:

```kotlin
dependencies {
    implementation("dev.redspartan:ashlar:0.2.0")
}

// 1. A resolvable view of the artifact, to read templates out of the jar.
val ashlarArtifact = configurations.create("ashlarArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies { ashlarArtifact("dev.redspartan:ashlar:0.2.0") }

// 2. Stage one JTE source root: your templates plus Ashlar's.
val jteSourceRoot = layout.buildDirectory.dir("jte-sources")
val assembleJteSources = tasks.register<Sync>("assembleJteSources") {
    into(jteSourceRoot)
    from(ashlarArtifact.elements.map { l -> l.map { zipTree(it.asFile) } }) {
        include("ashlar/**")
    }
    from("src/main/jte")
}

// 3. Point JTE at the staged root.
jte {
    generate()
    sourceDirectory.set(jteSourceRoot.get().asFile.toPath())
}
tasks.named("generateJte") { dependsOn(assembleJteSources) }
```

Then call components under the `ashlar` namespace:

```jte
@import dev.redspartanlabs.ashlar.icon.Icon

@template.ashlar.components.button(text = "Save", type = "Primary", icon = Icon.CHECK)
```

The Maven group is `dev.redspartan`; Ashlar's Java packages live under
`dev.redspartanlabs.ashlar`.

### Assets

Ashlar's JavaScript ships at `META-INF/resources/ashlar/js/`, which servlet
containers and Spring Boot serve from dependency jars with no configuration —
they appear at `/ashlar/js/…`.

Applications mounted under a context path tell Ashlar once, at startup:

```java
import dev.redspartanlabs.ashlar.AshlarAssets;

AshlarAssets.basePath(contextPath + "/ashlar");
```

### Styling

Ashlar has two kinds of styling, because the migration from Tailwind to Ashlar's
own CSS is not finished:

- **Ashlar's own CSS** covers the CSS foundation and five components: Badge,
  Alert, Divider, Progress and Empty State. Their rules do not depend on
  Tailwind utility classes or on Tailwind's CSS being present. They can read
  Tailwind theme variables when available and fall back to fixed values
  otherwise (see [Compatibility](#compatibility-with-earlier-versions) for what
  that does and does not mean).
- **Tailwind CSS** still styles the other 56 components. They use Tailwind
  utility classes, and your own Tailwind build generates the CSS for them.

An application that uses any of the other 56 components therefore still needs
Tailwind CSS v4, and the Tailwind setup below. Ashlar's stylesheet is needed for
the five migrated components and the foundation. On the showcase's pages, in
Chrome, loading it did not change how the Tailwind-styled components render.

#### Ashlar's stylesheet

The assembled stylesheet is in the jar at
`META-INF/resources/ashlar/css/ashlar.css`. There are two ways to get it into a
page.

**Extract it and import it (tested).** The showcase extracts the file from the
artifact and imports it into its own Tailwind build. This is the setup that is
used and tested end to end in this repository:

```kotlin
// build.gradle.kts: extract ashlar.css from the artifact (ashlarArtifact is the
// configuration from the staging step above).
val stageAshlarCss = tasks.register<Sync>("stageAshlarCss") {
    into(layout.buildDirectory.dir("ashlar-css"))
    from(ashlarArtifact.elements.map { l -> l.map { zipTree(it.asFile) } }) {
        include("META-INF/resources/ashlar/css/ashlar.css")
        eachFile { relativePath = RelativePath(true, name) }
        includeEmptyDirs = false
    }
}
// Run it before your Tailwind build.
```

```css
/* src/main/tailwind/app.css. Import Ashlar FIRST, before Tailwind. */
@import "../../../build/ashlar-css/ashlar.css";
@import "tailwindcss";
```

The order matters. Ashlar's file starts with the layer-order statement, and a
layer takes its place in the cascade where it is first mentioned, so the file
must come before anything else that names layers. If it is loaded after
Tailwind, Ashlar's layers rank above Tailwind's `utilities` layer instead of
below it.

**Serve it from the jar (checked with Spring Boot only).** Like the JavaScript,
the file is a resource under `META-INF/resources/`, so a Spring Boot application
serves it from the dependency jar at `<context-path>/ashlar/css/ashlar.css`. For
an application mounted at the root that is `/ashlar/css/ashlar.css`. The showcase
runs under the context path `/ashlar` and calls
`AshlarAssets.basePath(contextPath + "/ashlar")` at startup (see
[Assets](#assets)), so there the file is at `/ashlar/ashlar/css/ashlar.css`, and
`AshlarAssets.css("ashlar.css")` returns the URL for the base path in use.

If you link the file as a separate stylesheet this way, link it before your
Tailwind stylesheet, for the same reason as the import order above.

This was checked once by starting the showcase's boot jar and requesting that
URL: it returned the stylesheet as `text/css`, while `/ashlar/css/ashlar.css`
under that context path returned 404. No committed test requests the URL, and
serving from other servlet containers has not been checked. The cascade tests
that link `ashlar.css` as a separate stylesheet use fixture files, not the file
served from the jar, so they establish the layer-order behaviour described here,
not the served URL.

#### Layer order

`ashlar.css` declares this order, lowest priority first:

```css
@layer properties, theme, base,
       ash.tokens, ash.base, ash.layout, ash.components, ash.skin, ash.utilities,
       components, utilities;
```

`properties`, `theme`, `base`, `components` and `utilities` are Tailwind's layer
names, named here only to fix the order; Ashlar writes no rules in them. Every
Ashlar rule is in an `ash.*` sublayer, between Tailwind's `base` and
`components`. `ash.skin` is reserved in this order and holds no rules today.

Your own styles can override Ashlar without `!important`:

- Rules that are not in any layer win over every layer.
- Rules in Tailwind's `components` and `utilities` layers come after every
  `ash.*` layer, so they win over Ashlar's rules.
- Components expose custom properties for their colours and sizes (for example
  `--ash-progress-fill`); set one on the element with an inline style or an
  unlayered rule.

#### What the CSS foundation contains

- **Tokens** (`ash.tokens`): custom properties on `:root`, such as
  `--ash-color-bg`, `--ash-color-fg`, `--ash-color-accent`, `--ash-radius-md`
  and `--ash-space-4`. Each reads the matching Tailwind theme variable when one
  exists (for example `--color-blue-600`) and has a fixed fallback otherwise, so
  it works with or without Tailwind, and a skin that overrides the Tailwind
  variables still recolours Ashlar.
- **Base** (`ash.base`): `box-sizing: border-box` for elements with an `ash-`
  class and an opt-in `.ash-focus-ring`. There is no global reset.
- **Layout primitives** (`ash.layout`): `.ash-container`, `.ash-stack`,
  `.ash-grid`, `.ash-split` and `.ash-cluster`, with their modifiers.
- **Utilities** (`ash.utilities`): `.ash-sr-only` and `.ash-truncate`.
- **Components** (`ash.components`): `.ash-badge`, `.ash-alert`, `.ash-divider`,
  `.ash-progress` and `.ash-empty-state`, with their element and modifier
  classes.

#### Dark mode

Dark mode follows a `data-theme="dark"` attribute, not the operating system's
`prefers-color-scheme`:

- Put `data-theme="dark"` on the root element (`<html data-theme="dark">`).
  The dark token values apply to `:root[data-theme="dark"]` and do not apply if
  the attribute is on any other element.
- The migrated components apply their dark colours to an element that has
  `data-theme="dark"` itself or inside any ancestor that does.
- Ashlar does not set `color-scheme`. If you want native controls and scrollbars
  to follow the theme, set `color-scheme: dark` yourself for
  `:root[data-theme="dark"]`, as the showcase does.
- Ashlar does not switch the attribute. Your application does that, for example
  from a theme toggle.

#### Compatibility with earlier versions

The five migrated components no longer emit the Tailwind utility classes they
used before (for example `flex`, `text-sm` or `px-6` on an Empty State). Their
markup structure, parameters, accessibility attributes and `data-*` hooks are
unchanged. Anything that depended on the old
utility classes being present on those elements will change:

- CSS or JavaScript selectors that target those utility classes on these
  components stop matching.
- The five components were compared against their previous Tailwind rendering
  both with Tailwind present and with Tailwind removed, in Chrome. The
  comparisons indicate that the migrated components' own rules did not rely on
  Tailwind in those comparisons; they do not make Ashlar independent of
  Tailwind. In particular, the icon inside an Empty State is rendered by the Icon
  component, which has not been migrated and still emits Tailwind classes
  (`inline-block shrink-0` and the size you pass). Empty State states the few
  properties it needs itself, and its rendering matched the Tailwind version in
  the Tailwind-removed comparison.

Select these components by their `ash-*` classes or their `data-*` hooks
instead.

#### Tailwind setup for the other components

Tailwind must scan the directory containing the staged Ashlar templates and the
generated `ashlar/tailwind/classes.txt` - that is the `jte-sources` directory the
staging step above creates (`build/jte-sources`). The `classes.txt` file lists
the classes Ashlar applies from Java and JavaScript, which appear in no template.

```css
/* The path is relative to this stylesheet. For a stylesheet at
   src/main/tailwind/app.css, the staged root is three levels up. */
@source "../../../build/jte-sources";
@custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));
```

The `dark` variant above is what makes the Tailwind-styled components follow
`data-theme="dark"`; the migrated components do not need it.

## Building

```
./gradlew build          # library + showcase
./gradlew :showcase:bootRun
```

The showcase runs under a context path (`/ashlar`) on purpose, so that asset-URL
handling is exercised rather than assumed.

## Developing with live template reload

Because Ashlar's templates are staged into `build/jte-sources`, JTE's
development mode watches that staged root rather than `src/main/jte`. Re-run
the staging task on change and edits appear live, with no restart. Two
terminals:

```
./gradlew :showcase:assembleJteSources -t                        # re-stages on every edit
./gradlew :showcase:bootRun --args=--spring.profiles.active=dev  # JTE dev mode
```

Verified: editing a template in `showcase/src/main/jte` **or** in
`ashlar/src/main/jte` is re-staged automatically and served on the next
request, without restarting the application.

Note that the packaged boot jar cannot run in development mode at all — JTE
requires precompiled templates inside a self-contained jar. Development mode
is a `bootRun` workflow; `application-dev.properties` carries the settings.

## Third-party material

Icon artwork provenance and licensing is recorded in
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
