plugins {
    `java-library`
    `maven-publish`
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

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "ashlar"
            from(components["java"])

            pom {
                name = "Ashlar"
                description = "Server-side UI components for JTE"
                url = "https://github.com/redspartanlabs/ashlar"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "redspartanlabs"
                        name = "RedSpartan Labs"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/redspartanlabs/ashlar.git"
                    developerConnection = "scm:git:ssh://git@github.com/redspartanlabs/ashlar.git"
                    url = "https://github.com/redspartanlabs/ashlar"
                }
            }
        }
    }
}
