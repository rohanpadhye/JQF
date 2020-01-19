pluginManagement {
    plugins {
        fun String.v() = extra["$this.version"].toString()
        fun PluginDependenciesSpec.idv(id: String, key: String = id) = id(id) version key.v()

        idv("com.github.johnrengelman.shadow")
        idv("com.github.vlsi.crlf", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.gradle-extensions", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.ide", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.license-gather", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.stage-vote-release", "com.github.vlsi.vlsi-release-plugins")
        idv("com.gradle.plugin-publish")
        idv("org.jetbrains.gradle.plugin.idea-ext")
    }
}

rootProject.name = "jqf"

include(
    "afl-proxy"
)

for (p in listOf(
    "dependencies-bom",
    "fuzz",
    "instrument",
    "examples"
    // maven-plugin is not implemented in Gradle syet
    // "maven-plugin"
)) {
    include(p)
    project(":$p").apply {
        name = "jqf-$p"
    }
}
