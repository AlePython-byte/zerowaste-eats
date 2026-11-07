// Plain Kotlin the app's build script uses, kept here so it can have unit tests
plugins {
    `embedded-kotlin`
}

dependencies {
    testImplementation(libs.junit)
}

// Gradle builds buildSrc before configuring the app, so a failing test stops every build
// (the tests compile against the jar, so they run right after it rather than before)
tasks.jar {
    finalizedBy(tasks.test)
}
