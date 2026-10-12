plugins {
    java
    id("org.springframework.boot") version "4.0.8"
    id("io.spring.dependency-management") version "1.1.7"
    id("gg.jte.gradle") version "3.2.4"
}

description = "Ashlar showcase - a demo application that consumes Ashlar as a dependency"

// NOTE for anyone editing the comments in this file: Kotlin block comments
// NEST. Writing a glob such as ashlar-slash-star-star inside a block comment
// opens a nested comment, and the closing marker then only closes that inner
// one - silently commenting out the remainder of the build script with no
// error reported. Prefer line comments when prose needs to contain globs.

// Resolvable view of the Ashlar artifact, used to read templates OUT of the
// jar rather than off the sibling project's source tree. That distinction is
// the whole point of the exercise: reaching into ../ashlar/src/main/jte would
// "work" here and prove nothing about an external consumer.
val ashlarArtifact = configurations.create("ashlarArtifact") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("gg.jte:jte-spring-boot-starter-4:3.2.4")

    // Ashlar as an ordinary dependency. Swapping this line for
    // implementation("dev.redspartan:ashlar:0.1.0") is the only change an
    // external consumer would make - everything below is what that consumer's
    // build has to do as well, and is the thing being validated.
    implementation(project(":ashlar"))
    ashlarArtifact(project(":ashlar"))

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// JTE's Gradle plugin takes exactly one sourceDirectory, and it must be a real
// directory on disk. A consumer therefore cannot simply point it at
// src/main/jte and expect Ashlar's templates - shipped inside a jar - to be
// visible alongside their own.
//
// So the build stages one merged JTE source root: this application's own
// templates, plus Ashlar's templates unpacked from the artifact. Filtering on
// the "ashlar" prefix preserves the namespace - Ashlar's files are already
// laid out as ashlar/components/button.jte inside the jar, so unpacking them
// here yields build/jte-sources/ashlar/components/button.jte and makes
// @template.ashlar.components.button(...) resolve.
//
// Staging into build/ rather than extracting into src/main/jte is deliberate:
// a library has no business writing into a consumer's source tree, where the
// files would be easy to edit by mistake and easy to commit by accident.
val jteSourceRoot = layout.buildDirectory.dir("jte-sources")

val assembleJteSources = tasks.register<Sync>("assembleJteSources") {
    description = "Merges this application's templates with Ashlar's, extracted from the artifact."
    group = "build"

    into(jteSourceRoot)

    // `elements` (rather than `files`) carries the producing task dependency,
    // so Gradle knows this task must run after :ashlar:jar on a clean build.
    from(ashlarArtifact.elements.map { locations -> locations.map { zipTree(it.asFile) } }) {
        include("ashlar/**")
    }

    from("src/main/jte")
}

jte {
    generate()
    sourceDirectory.set(jteSourceRoot.get().asFile.toPath())
}

tasks.named("generateJte") {
    dependsOn(assembleJteSources)
}

// Ashlar's stylesheet is a jar RESOURCE (META-INF/resources/ashlar/css), not part
// of the ashlar/** template namespace staged above, so it gets its own staging
// step: extracted from the artifact, flattened to a single file the Tailwind
// entry can @import with a plain relative path.
val ashlarCssStage = layout.buildDirectory.dir("ashlar-css")

val stageAshlarCss = tasks.register<Sync>("stageAshlarCss") {
    description = "Extracts ashlar.css from the Ashlar artifact for the Tailwind build to import."
    group = "build"

    into(ashlarCssStage)
    from(ashlarArtifact.elements.map { locations -> locations.map { zipTree(it.asFile) } }) {
        include("META-INF/resources/ashlar/css/ashlar.css")
        eachFile { relativePath = RelativePath(true, name) }
        includeEmptyDirs = false
    }
}

// Tailwind scans the same staged directory, which is how Ashlar's utility
// classes survive content scanning: the templates Tailwind needs to see are
// the extracted ones, not anything in this application's own source tree.
val buildCss = tasks.register<Exec>("buildCss") {
    description = "Compiles Tailwind CSS, scanning both this app's and Ashlar's staged templates."
    group = "build"

    dependsOn(assembleJteSources, stageAshlarCss)

    workingDir = rootDir
    val npm = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
    commandLine(npm, "run", "build:css")

    inputs.dir(jteSourceRoot)
    inputs.dir(ashlarCssStage)
    inputs.dir("src/main/tailwind")
    outputs.file("src/main/resources/static/css/app.css")
}

tasks.named("processResources") {
    dependsOn(buildCss)
}

// V1 cascade verification (test-only). Compiles the Path A wrapper entry
// points under src/test/tailwind with the real Tailwind CLI, against the same
// staged templates the production stylesheet is built from. The outputs live
// under build/ and are served to a real browser by the cascade tests; nothing
// here feeds the production app.css.
val cascadeCssDir = layout.buildDirectory.dir("generated/cascade-css")

fun registerCascadeCss(taskName: String, entry: String, outputName: String) = tasks.register<Exec>(taskName) {
    description = "Compiles $entry with the real Tailwind CLI for the cascade tests."
    group = "verification"

    dependsOn(assembleJteSources)

    workingDir = rootDir
    val npmCommand = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
    val output = cascadeCssDir.map { it.file(outputName) }

    inputs.file(entry)
    inputs.dir("src/test/tailwind")
    inputs.dir("src/test/resources/cascade")
    inputs.dir("src/main/tailwind")
    inputs.dir(jteSourceRoot)
    outputs.file(output)

    doFirst {
        output.get().asFile.parentFile.mkdirs()
    }
    commandLine(
        npmCommand, "exec", "--no", "--", "tailwindcss",
        "-i", file(entry).absolutePath,
        "-o", output.get().asFile.absolutePath,
        "--minify"
    )
}

val buildCascadePathA = registerCascadeCss("buildCascadePathA", "src/test/tailwind/path-a-entry.css", "path-a.css")
val buildCascadePathAWrongOrder = registerCascadeCss("buildCascadePathAWrongOrder", "src/test/tailwind/path-a-wrong-order-entry.css", "path-a-wrong-order.css")

// Phase A entries: (task name, entry file, output name).
val phaseACascadeEntries = listOf(
    Triple("buildCascadeA2Import", "src/test/tailwind/a2-import-entry.css", "a2-import.css"),
    Triple("buildCascadeA2ImportNoStatement", "src/test/tailwind/a2-import-no-statement-entry.css", "a2-import-no-statement.css"),
    Triple("buildCascadeA2ImportAshFirst", "src/test/tailwind/a2-import-ash-first-entry.css", "a2-import-ash-first.css"),
    Triple("buildCascadeA3PropertiesFirst", "src/test/tailwind/a3-properties-first-entry.css", "a3-properties-first.css"),
    Triple("buildCascadeA4AppLayerLast", "src/test/tailwind/a4-app-layer-last-entry.css", "a4-app-layer-last.css"),
    Triple("buildCascadePhaseCBaseline", "src/test/tailwind/phase-c-baseline-entry.css", "phase-c-baseline.css")
).map { (taskName, entry, outputName) -> registerCascadeCss(taskName, entry, outputName) }

val buildCascadeCss = tasks.register("buildCascadeCss") {
    description = "Builds every Path A stylesheet used by the cascade tests."
    group = "verification"
    dependsOn(buildCascadePathA, buildCascadePathAWrongOrder)
    dependsOn(phaseACascadeEntries)
}

tasks.named<Test>("test") {
    dependsOn(buildCascadeCss)
    systemProperty("ashlar.cascade.dir", cascadeCssDir.get().asFile.absolutePath)

    // Phase C runs the REAL packaged application and compares it with the generated
    // stylesheet: it needs the boot jar and the library's generated output.
    dependsOn(tasks.named("bootJar"))
    systemProperty("ashlar.showcase.jar", tasks.named<Jar>("bootJar").get().archiveFile.get().asFile.absolutePath)
    systemProperty("ashlar.css.generated", rootProject.layout.projectDirectory
        .file("ashlar/build/generated/css/META-INF/resources/ashlar/css/ashlar.css").asFile.absolutePath)
    dependsOn(":ashlar:generateAshlarCss")
}
