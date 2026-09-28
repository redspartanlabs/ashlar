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
    // implementation("com.redspartan:ashlar:<version>") is the only change an
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

// Tailwind scans the same staged directory, which is how Ashlar's utility
// classes survive content scanning: the templates Tailwind needs to see are
// the extracted ones, not anything in this application's own source tree.
val buildCss = tasks.register<Exec>("buildCss") {
    description = "Compiles Tailwind CSS, scanning both this app's and Ashlar's staged templates."
    group = "build"

    dependsOn(assembleJteSources)

    workingDir = rootDir
    val npm = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
    commandLine(npm, "run", "build:css")

    inputs.dir(jteSourceRoot)
    inputs.dir("src/main/tailwind")
    outputs.file("src/main/resources/static/css/app.css")
}

tasks.named("processResources") {
    dependsOn(buildCss)
}
