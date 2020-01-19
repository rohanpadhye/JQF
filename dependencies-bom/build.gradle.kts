plugins {
    `java-platform`
}

val String.v: String get() = rootProject.extra["$this.version"] as String

// Note: Gradle allows to declare dependency on "bom" as "api",
// and it makes the contraints to be transitively visible
// However Maven can't express that, so the approach is to use Gradle resolution
// and generate pom files with resolved versions
// See https://github.com/gradle/gradle/issues/9866

fun DependencyConstraintHandlerScope.apiv(
    notation: String,
    versionProp: String = notation.substringAfterLast(':')
) =
    "api"(notation + ":" + versionProp.v)

fun DependencyConstraintHandlerScope.runtimev(
    notation: String,
    versionProp: String = notation.substringAfterLast(':')
) =
    "runtime"(notation + ":" + versionProp.v)

dependencies {
    // Parenthesis are needed here: https://github.com/gradle/gradle/issues/9248
    (constraints) {
        // api means "the dependency is for both compilation and runtime"
        // runtime means "the dependency is only for runtime, not for compilation"
        // In other words, marking dependency as "runtime" would avoid accidental
        // dependency on it during compilation
        apiv("com.google.errorprone:error_prone_check_api", "error_prone")
        apiv("com.google.errorprone:error_prone_core", "error_prone")
        apiv("com.google.errorprone:error_prone_test_helpers", "error_prone")
        apiv("com.google.guava:guava")
        apiv("com.google.guava:guava-testlib", "guava")
        apiv("com.google.javascript:closure-compiler")
        apiv("com.pholser:junit-quickcheck-core", "junit-quickcheck")
        apiv("com.pholser:junit-quickcheck-generators", "junit-quickcheck")
        apiv("com.pholser:junit-quickcheck-generators", "junit-quickcheck")
        apiv("info.picocli:picocli")
        apiv("junit:junit", "junit4")
        apiv("org.apache.ant:ant")
        apiv("org.apache.bcel:bcel")
        apiv("org.apache.commons:commons-collections4")
        apiv("org.apache.commons:commons-compress")
        apiv("org.apache.commons:commons-lang3")
        apiv("org.apache.commons:commons-math3")
        apiv("org.apache.maven:maven-model-builder")
        apiv("org.apache.struts:struts2-core")
        apiv("org.apache.tika:tika-parsers")
        apiv("org.apache.tomcat:tomcat-catalina")
        apiv("org.apache.tomcat:tomcat-coyote")
        apiv("org.exparity:hamcrest-date")
        apiv("org.hamcrest:hamcrest")
        apiv("org.hamcrest:hamcrest-core", "hamcrest")
        apiv("org.hamcrest:hamcrest-library", "hamcrest")
        apiv("org.jacoco:org.jacoco.report", "jacoco")
        apiv("org.jgrapht:jgrapht-core", "jgrapht")
        apiv("org.jgrapht:jgrapht-ext", "jgrapht")
        apiv("org.junit.jupiter:junit-jupiter-api", "junit5")
        apiv("org.junit.jupiter:junit-jupiter-params", "junit5")
        apiv("org.lichess:scalachess_2.12")
        apiv("org.mockito:mockito-core")
        apiv("org.mozilla:rhino")
        apiv("org.ow2.asm:asm", "asm")
        apiv("org.slf4j:slf4j-api", "slf4j")
        apiv("org.slf4j:slf4j-log4j12", "slf4j")


        runtimev("org.junit.jupiter:junit-jupiter-engine", "junit5")
        runtimev("org.junit.vintage:junit-vintage-engine", "junit5")
    }
}
