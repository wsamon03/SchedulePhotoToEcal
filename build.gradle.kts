// Intentionally empty: the Android/Compose plugins are declared directly in
// app/build.gradle.kts instead of here with `apply false`, so that running
// Gradle tasks scoped to the plain-Kotlin :parser module (e.g. `:parser:test`)
// never requires resolving the Android Gradle Plugin.
