// Root build script holds shared configuration only. It deliberately applies
// no plugins of its own: the Spring Boot plugin in particular must not reach
// the `ashlar` module, which is a plain library and must not be packaged as
// an executable application.

plugins {
    java
}

allprojects {
    group = "com.redspartanlabs"
    version = "0.1.0-SNAPSHOT"

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
