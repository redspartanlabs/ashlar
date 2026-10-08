# Ashlar

[![CI](https://github.com/redspartanlabs/ashlar/actions/workflows/ci.yml/badge.svg)](https://github.com/redspartanlabs/ashlar/actions/workflows/ci.yml)

Ashlar is a server-rendered UI component system for Java and Kotlin
applications that use [JTE](https://jte.gg). Components are JTE templates that
render real HTML on the server; components that need behaviour load a small
JavaScript module of their own. Styling is Tailwind CSS - Ashlar ships no
stylesheet.

It provides 61 components and 9 layout utilities, each documented with live
examples in the showcase application (53 pages; see [Building](#building) to
run it).

A RedSpartan Labs project. Licensed under the [Apache License 2.0](LICENSE).

## Modules

| Module | Published | Purpose |
| --- | --- | --- |
| `ashlar` | `dev.redspartan:ashlar` | The library: Java support classes, JTE templates, JavaScript assets and a generated Tailwind class list. It ships no prebuilt CSS. |
| `showcase` | no | A Spring Boot application that consumes Ashlar exactly as an external consumer would. |

## Consuming Ashlar

JTE's build plugins generate code from `.jte` files **on the filesystem** — they
cannot read templates from a jar on the compile classpath. Ashlar therefore
ships its template source inside the artifact, and a consumer's build unpacks
it into the JTE source root.

Ashlar requires Java 21 or newer and declares JTE 3.2.4 as a dependency; styling
needs Tailwind CSS v4. A Kotlin application uses the same artifact and the same
`.jte` templates.

Three additions to a consumer's `build.gradle.kts`, which assumes the
[JTE Gradle plugin](https://jte.gg/gradle-plugin/) (`gg.jte.gradle`) is already
applied:

```kotlin
dependencies {
    implementation("dev.redspartan:ashlar:0.1.0")
}

// 1. A resolvable view of the artifact, to read templates out of the jar.
val ashlarArtifact = configurations.create("ashlarArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies { ashlarArtifact("dev.redspartan:ashlar:0.1.0") }

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

Ashlar ships no CSS: its components use Tailwind utility classes, and your own
Tailwind build generates the stylesheet. Tailwind must scan the directory
containing the staged Ashlar templates and the generated
`ashlar/tailwind/classes.txt` - that is the `jte-sources` directory the staging
step above creates (`build/jte-sources`). The `classes.txt` file lists the
classes Ashlar applies from Java and JavaScript, which appear in no template.

```css
/* The path is relative to this stylesheet. For a stylesheet at
   src/main/tailwind/app.css, the staged root is three levels up. */
@source "../../../build/jte-sources";
@custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));
```

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
