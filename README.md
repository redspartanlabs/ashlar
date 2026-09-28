# Ashlar

Server-side UI components for [JTE](https://jte.gg), built for Java applications
that render HTML on the server.

A RedSpartan Labs project. Licensed under the [Apache License 2.0](LICENSE).

> **Status: early.** This repository currently contains a validated vertical
> slice — Button and Icon — proving that Ashlar can be consumed as a real
> dependency. The remaining components are migrating from the laboratory
> project they were developed in.

## Modules

| Module | Published | Purpose |
| --- | --- | --- |
| `ashlar` | `com.redspartan:ashlar` | The library: Java support classes, JTE templates, CSS and JS assets. |
| `showcase` | no | A Spring Boot application that consumes Ashlar exactly as an external consumer would. |

## Consuming Ashlar

JTE's build plugins generate code from `.jte` files **on the filesystem** — they
cannot read templates from a jar on the compile classpath. Ashlar therefore
ships its template source inside the artifact, and a consumer's build unpacks
it into the JTE source root.

Three additions to a consumer's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.redspartan:ashlar:<version>")
}

// 1. A resolvable view of the artifact, to read templates out of the jar.
val ashlarArtifact = configurations.create("ashlarArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
dependencies { ashlarArtifact("com.redspartan:ashlar:<version>") }

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
@import com.redspartan.ashlar.icon.Icon

@template.ashlar.components.button(text = "Save", type = "Primary", icon = Icon.CHECK)
```

### Assets

Ashlar's JavaScript ships at `META-INF/resources/ashlar/js/`, which servlet
containers and Spring Boot serve from dependency jars with no configuration —
they appear at `/ashlar/js/…`.

Applications mounted under a context path tell Ashlar once, at startup:

```java
AshlarAssets.basePath(contextPath + "/ashlar");
```

### Styling

Ashlar's components use Tailwind utility classes. Point your Tailwind build at
the staged template root so those classes survive content scanning:

```css
@source "../../../build/jte-sources";
@custom-variant dark (&:where([data-theme="dark"], [data-theme="dark"] *));
```

## Building

```
./gradlew build          # library + showcase
./gradlew :showcase:bootRun
```

The showcase runs under a context path (`/myapp`) on purpose, so that asset-URL
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
