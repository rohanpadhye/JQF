plugins {
    id("com.github.johnrengelman.shadow")
}

val zestCli by configurations.creating {
    extendsFrom(configurations.runtimeClasspath.get())
}

dependencies {
    api(project(":jqf-instrument"))
    api("com.pholser:junit-quickcheck-core")
    api("com.pholser:junit-quickcheck-generators")
    api("info.picocli:picocli")

    implementation("org.jacoco:org.jacoco.report")

    testImplementation("org.mockito:mockito-core")

    // Add logging to zest-cli.jar otherwise slf4j won't print the logging
    zestCli("org.slf4j:slf4j-log4j12")
}

tasks {
    jar {
        dependsOn(shadowJar)
    }

    test {
        // Inner classes in GuidanceTest are not true tests, so Gradle should not try executing them
        exclude("**/GuidanceTest$*")
    }

    shadowJar {
        manifest {
            attributes["Main-Class"] = "edu.berkeley.cs.jqf.fuzz.ei.ZestCLI"
        }
        archiveClassifier.set("zest-cli")
        configurations = listOf(zestCli)
        exclude("META-INF/maven/**")
        exclude("META-INF/LICENSE*")
        exclude("META-INF/NOTICE*")
    }
}
