// Root build script holds shared configuration only. It deliberately applies
// no plugins of its own: the Spring Boot plugin in particular must not reach
// the `ashlar` module, which is a plain library and must not be packaged as
// an executable application.

plugins {
    java
}

allprojects {
    // The Maven group is the reverse of the broader RedSpartan publishing
    // domain (redspartan.dev). The Java package, dev.redspartanlabs.ashlar, is
    // RedSpartan Labs' own source namespace (redspartanlabs.dev). The two are
    // deliberately different and independent on Maven Central.
    group = "dev.redspartan"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
