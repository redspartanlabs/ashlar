plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
}

description = "Ashlar - server-side UI components for JTE"

dependencies {
    // Ashlar's own Java classes are plain Java and use none of JTE's API -
    // but its templates do, and a consumer's JTE build must have it. Declared
    // `api` so the requirement travels with the dependency and so Gradle
    // resolves it against whatever newer version the consumer already has.
    api("gg.jte:jte:3.2.4")

    // Unit tests for the module's pure-Java support classes only - no JTE
    // template rendering, no Spring context. The root build already turns on
    // useJUnitPlatform() for every subproject; this is the one dependency
    // that was missing to actually run a JUnit 5 test here.
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/**
 * The single most important line in this build.
 *
 * JTE's build plugins generate code from `.jte` files on the filesystem; they
 * cannot read templates out of a jar on the compile classpath. So a component
 * library written in JTE cannot ship only compiled classes - it has to ship
 * its template SOURCE and let the consumer's own JTE build read it.
 *
 * Adding the template root as a resource directory does exactly that, with no
 * copy task: templates authored at `src/main/jte/ashlar/components/button.jte`
 * land in the jar at `ashlar/components/button.jte`. That path shape is not
 * incidental - a consumer extracts it into their JTE source root and the
 * leading `ashlar/` segment becomes the template namespace, which is what
 * makes `@template.ashlar.components.button(...)` resolve and what keeps
 * Ashlar's templates from colliding with the consumer's own `components/`.
 */
sourceSets {
    main {
        resources {
            srcDir("src/main/jte")
        }
    }
}

/**
 * Ships Ashlar's complete Tailwind class vocabulary inside the jar.
 *
 * Ashlar's utility classes do not all live in templates. Variant maps sit in
 * Java (`ButtonStyles`, `ModalSize`, ...) and runtime state classes sit in the
 * JavaScript modules (`drawer.js` toggles `opacity-100`, `table.js` toggles
 * `bg-blue-50/60`, ...). A consumer's Tailwind build scans the staged
 * `ashlar/` template root - and nothing else - so classes held anywhere other
 * than a template never reach its stylesheet. Compiled classes and jars are
 * invisible to Tailwind, and the consumer must not need Ashlar's source layout.
 *
 * So the build writes every string literal found in the library's Java and
 * JavaScript sources into `ashlar/tailwind/classes.txt`. It lands under the
 * `ashlar/` prefix on purpose: the consumer's existing staging step already
 * unpacks everything under `ashlar/` into the JTE source root, JTE ignores files that are not
 * templates, and Tailwind scans text files in whatever root it is pointed at.
 *
 * The extraction is deliberately dumb. It does not know what a Tailwind class
 * is, so it cannot silently drop one: negative utilities (`-translate-x-full`)
 * and arbitrary or data variants (`data-[toast-state=open]:opacity-100`) are
 * exactly what a "looks like a class" filter lost in an earlier attempt. Extra
 * tokens (words from messages, SVG path data) are harmless; Tailwind ignores
 * what is not a utility. Output is sorted and de-duplicated so it is
 * byte-for-byte deterministic.
 *
 * Convention this depends on: a Tailwind class used by Ashlar must appear as a
 * complete string literal in Java or JavaScript. Building one from fragments
 * ("bg-" + color) would be invisible here, exactly as it is to Tailwind itself.
 */
val tailwindVocabularyDir = layout.buildDirectory.dir("generated/tailwind-vocabulary")

val generateTailwindVocabulary = tasks.register("generateTailwindVocabulary") {
    description = "Writes every string literal from Ashlar's Java and JavaScript sources to ashlar/tailwind/classes.txt."
    group = "build"

    val scanned = fileTree("src/main") { include("**/*.java", "**/*.js") }
    inputs.files(scanned).withPropertyName("scannedSources").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(tailwindVocabularyDir)

    doLast {
        // \x22 is ", \x27 is ' and \x60 is `. Java: text blocks first, then ordinary
        // strings. JavaScript: all three string forms; backtick strings may span lines.
        val javaStrings = Regex("""\x22{3}[\s\S]*?\x22{3}|\x22(?:[^\x22\\\n]|\\.)*\x22""")
        val jsStrings = Regex("""\x22(?:[^\x22\\\n]|\\.)*\x22|\x27(?:[^\x27\\\n]|\\.)*\x27|\x60(?:[^\x60\\]|\\.)*\x60""")
        val tokens = sortedSetOf<String>()
        scanned.forEach { file ->
            val isJava = file.name.endsWith(".java")
            val pattern = if (isJava) javaStrings else jsStrings
            pattern.findAll(file.readText(Charsets.UTF_8)).forEach { match ->
                val literal = match.value
                val edge = if (literal.startsWith("\"\"\"")) 3 else 1
                literal.substring(edge, literal.length - edge)
                    .split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .forEach { tokens.add(it) }
            }
        }
        val output = tailwindVocabularyDir.get().file("ashlar/tailwind/classes.txt").asFile
        output.parentFile.mkdirs()
        output.writeText(tokens.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
    }
}

tasks.processResources {
    from(generateTailwindVocabulary)
}

/**
 * Publishing to Maven Central through the Central Portal.
 *
 * One plugin owns the whole path: it generates the sources and Javadoc jars
 * Central requires, builds the POM, signs, and uploads the bundle. Nothing here
 * is a credential - those arrive as Gradle properties, which CI supplies from
 * encrypted secrets as ORG_GRADLE_PROJECT_* environment variables:
 *
 *   mavenCentralUsername / mavenCentralPassword   Central Portal user token
 *   signingInMemoryKey / signingInMemoryKeyPassword   ASCII-armored private key
 *   ashlarDeveloperEmail                           published in the POM
 *
 * Signing is switched on only when a key is present, so an ordinary local
 * build, test run or publishToMavenLocal needs no credentials at all.
 */
mavenPublishing {
    publishToMavenCentral()

    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates("dev.redspartan", "ashlar", project.version.toString())

    pom {
        name = "Ashlar"
        description = "Server-side UI components for JTE"
        url = "https://github.com/redspartanlabs/ashlar"
        inceptionYear = "2026"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "redspartanlabs"
                name = "RedSpartan Labs"
                url = "https://github.com/redspartanlabs"
                organization = "RedSpartan Labs"
                organizationUrl = "https://redspartan.dev"
                // Central asks for a contact address. It is personal data, so it is
                // supplied at release time rather than invented or committed here.
                email = providers.gradleProperty("ashlarDeveloperEmail")
            }
        }

        scm {
            connection = "scm:git:https://github.com/redspartanlabs/ashlar.git"
            developerConnection = "scm:git:ssh://git@github.com/redspartanlabs/ashlar.git"
            url = "https://github.com/redspartanlabs/ashlar"
        }
    }
}
