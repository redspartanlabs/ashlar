rootProject.name = "ashlar"

// Two modules, deliberately: `ashlar` is the published library, `showcase`
// is an ordinary Spring Boot application that consumes it the same way any
// external consumer would - through the built artifact, never by reaching
// into the library's source tree. If the showcase builds, consumption works.
include("ashlar")
include("showcase")
