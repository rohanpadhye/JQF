import com.github.vlsi.gradle.properties.dsl.props
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("com.gradle.plugin-publish") apply false
    id("org.jetbrains.gradle.plugin.idea-ext")
    id("com.github.vlsi.gradle-extensions")
    id("com.github.vlsi.ide")
    id("com.github.vlsi.stage-vote-release")
}

val String.v: String get() = rootProject.extra["$this.version"] as String

val buildVersion = "jqf".v + releaseParams.snapshotSuffix

println("Building JQF $buildVersion")

val enableGradleMetadata by props()
val skipJavadoc by props()
val slowSuiteLogThreshold by props(0L)
val slowTestLogThreshold by props(2000L)

releaseParams {
    tlp.set("Jqf")
    organizationName.set("rohanpadhye")
    componentName.set("Jqf")
    prefixForProperties.set("jqf")
    svnDistEnabled.set(false)
    sitePreviewEnabled.set(false)
    nexus {
        mavenCentral()
    }
    voteText.set {
        """
        ${it.componentName} v${it.version}-rc${it.rc} is ready for preview.

        Git SHA: ${it.gitSha}
        Staging repository: ${it.nexusRepositoryUri}
        """.trimIndent()
    }
}

allprojects {
    group = "edu.berkeley.cs.jqf"
    version = buildVersion

    val javaUsed = file("src/main/java").isDirectory || file("src/test/java").isDirectory
    if (javaUsed) {
        apply(plugin = "java-library")
        dependencies {
            val compileOnly by configurations
            compileOnly("net.jcip:jcip-annotations:1.0")
            compileOnly("com.github.spotbugs:spotbugs-annotations:3.1.6")
            compileOnly("com.google.code.findbugs:jsr305:3.0.2")
        }
    }
    if (javaUsed) {
        dependencies {
            val implementation by configurations
            implementation(platform(project(":jqf-dependencies-bom")))
        }
    }

    val hasTests = file("src/test/java").isDirectory
    if (hasTests) {
        // Add default tests dependencies
        dependencies {
            val testImplementation by configurations
            val testRuntimeOnly by configurations
            if (project.props.bool("junit4", default = true)) {
                // By default the projects (e.g. 'examples') use JUnit4 test API
                testImplementation("junit:junit")
                testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
            }
            if (project.props.bool("junit5", default = false)) {
                // If junit5=true property is added to gradle.properties, then junit5 tests can be used
                testImplementation("org.junit.jupiter:junit-jupiter-api")
                testImplementation("org.junit.jupiter:junit-jupiter-params")
            }
            testImplementation("org.hamcrest:hamcrest")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
            // Allow tests to print slf4j logging via log4j
            testRuntimeOnly("org.slf4j:slf4j-log4j12")
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        // Ensure builds are reproducible
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirMode = "775".toInt(8)
        fileMode = "664".toInt(8)
    }

    plugins.withType<JavaPlugin> {
        configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            withSourcesJar()
            if (!skipJavadoc) {
                withJavadocJar()
            }
        }

        repositories {
            mavenCentral()
        }

        apply(plugin = "maven-publish")

        if (!enableGradleMetadata) {
            tasks.withType<GenerateModuleMetadata> {
                enabled = false
            }
        }

        tasks {
            withType<JavaCompile>().configureEach {
                options.encoding = "UTF-8"
            }

            withType<Jar>().configureEach {
                manifest {
                    attributes["Bundle-License"] = "BSD-2-Clause"
                    attributes["Implementation-Title"] = "Jqf"
                    attributes["Implementation-Version"] = project.version
                    attributes["Specification-Vendor"] = "Jqf"
                    attributes["Specification-Version"] = project.version
                    attributes["Specification-Title"] = "Jqf"
                    attributes["Implementation-Vendor"] = "Jqf"
                    attributes["Implementation-Vendor-Id"] = "edu.berkeley.cs.jqf"
                }
            }
            withType<Test>().configureEach {
                useJUnitPlatform()
                testLogging {
                    exceptionFormat = TestExceptionFormat.FULL
                    showStandardStreams = true
                }
                // Pass the property to tests
                fun passProperty(name: String, default: String? = null) {
                    val value = System.getProperty(name) ?: default
                    value?.let { systemProperty(name, it) }
                }
                passProperty("junit.jupiter.execution.parallel.enabled", "true")
                passProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                passProperty("junit.jupiter.execution.timeout.default", "5 m")
                // https://github.com/junit-team/junit5/issues/2041
                // Gradle does not print parameterized test names yet :(
                // Hopefully it will be fixed in Gradle 6.1
                fun String?.withDisplayName(displayName: String?, separator: String = ", "): String? = when {
                    displayName == null -> this
                    this == null -> displayName
                    endsWith(displayName) -> this
                    else -> "$this$separator$displayName"
                }
                fun printResult(descriptor: TestDescriptor, result: TestResult) {
                    val test = descriptor as org.gradle.api.internal.tasks.testing.TestDescriptorInternal
                    val classDisplayName = test.className.withDisplayName(test.classDisplayName)
                    val testDisplayName = test.name.withDisplayName(test.displayName)
                    val duration = "%5.1fsec".format((result.endTime - result.startTime) / 1000f)
                    val displayName = classDisplayName.withDisplayName(testDisplayName, " > ")
                    // Hide SUCCESS from output log, so FAILURE/SKIPPED are easier to spot
                    val resultType = result.resultType
                        .takeUnless { it == TestResult.ResultType.SUCCESS }
                        ?.toString()
                        ?: (if (result.skippedTestCount > 0 || result.testCount == 0L) "WARNING" else "       ")
                    if (!descriptor.isComposite) {
                        println("$resultType $duration, $displayName")
                    } else {
                        val completed = result.testCount.toString().padStart(4)
                        val failed = result.failedTestCount.toString().padStart(3)
                        val skipped = result.skippedTestCount.toString().padStart(3)
                        println("$resultType $duration, $completed completed, $failed failed, $skipped skipped, $displayName")
                    }
                }
                afterTest(KotlinClosure2<TestDescriptor, TestResult, Any>({ descriptor, result ->
                    // There are lots of skipped tests, so it is not clear how to log them
                    // without making build logs too verbose
                    if (result.resultType == TestResult.ResultType.FAILURE ||
                        result.endTime - result.startTime >= slowTestLogThreshold) {
                        printResult(descriptor, result)
                    }
                }))
                afterSuite(KotlinClosure2<TestDescriptor, TestResult, Any>({ descriptor, result ->
                    if (descriptor.name.startsWith("Gradle Test Executor")) {
                        return@KotlinClosure2
                    }
                    if (result.resultType == TestResult.ResultType.FAILURE ||
                        result.endTime - result.startTime >= slowSuiteLogThreshold) {
                        printResult(descriptor, result)
                    }
                }))
            }
            configure<PublishingExtension> {
                if (project.path == ":") {
                    // Do not publish "root" project. Java plugin is applied here for DSL purposes only
                    return@configure
                }
                publications {
                    if (project.path != ":jqf-plugin-gradle") {
                        create<MavenPublication>(project.name) {
                            artifactId = project.name
                            version = rootProject.version.toString()
                            description = project.description
                            from(project.components.get("java"))
                        }
                    }
                    withType<MavenPublication> {
                        // if (!skipJavadoc) {
                        // Eager task creation is required due to
                        // https://github.com/gradle/gradle/issues/6246
                        //  artifact(sourcesJar.get())
                        //  artifact(javadocJar.get())
                        // }

                        // Use the resolved versions in pom.xml
                        // Gradle might have different resolution rules, so we set the versions
                        // that were used in Gradle build/test.
                        versionMapping {
                            usage(Usage.JAVA_RUNTIME) {
                                fromResolutionResult()
                            }
                            usage(Usage.JAVA_API) {
                                fromResolutionOf("runtimeClasspath")
                            }
                        }
                        pom {
                            withXml {
                                val sb = asString()
                                var s = sb.toString()
                                // <scope>compile</scope> is Maven default, so delete it
                                s = s.replace("<scope>compile</scope>", "")
                                // Cut <dependencyManagement> because all dependencies have the resolved versions
                                s = s.replace(
                                    Regex(
                                        "<dependencyManagement>.*?</dependencyManagement>",
                                        RegexOption.DOT_MATCHES_ALL
                                    ),
                                    ""
                                )
                                sb.setLength(0)
                                sb.append(s)
                                // Re-format the XML
                                asNode()
                            }
                            name.set(
                                (project.findProperty("artifact.name") as? String)
                                    ?: "Jqf ${project.name.capitalize()}"
                            )
                            description.set(
                                project.description
                                    ?: "Jqf ${project.name.capitalize()}"
                            )
                            developers {
                                developer {
                                    name.set("Rohan Padhye")
                                    email.set("rohanpadhye@cs.berkeley.edu")
                                    organization.set("University of California, Berkeley")
                                    url.set("https://people.eecs.berkeley.edu/~rohanpadhye")
                                }
                                developer {
                                    name.set("Caroline Lemieux")
                                    email.set("clemieux@cs.berkeley.edu")
                                    organization.set("University of California, Berkeley")
                                    url.set("http://www.carolemieux.com")
                                }
                                developer {
                                    name.set("Yevgeny Pats")
                                    email.set("yp@fuzzit.dev")
                                    url.set("https://fuzzit.dev")
                                }
                            }
                            inceptionYear.set("2018")
                            url.set("https://github.com/rohanpadhye/jqf")
                            licenses {
                                license {
                                    name.set("BSD-2-Clause")
                                    url.set("https://raw.githubusercontent.com/rohanpadhye/jqf/master/LICENSE")
                                    comments.set("BSD-2-Clause, Copyright (c) 2017-2018 The Regents of the University of California")
                                    distribution.set("repo")
                                }
                            }
                            issueManagement {
                                system.set("GitHub")
                                url.set("https://github.com/rohanpadhye/jqf/issues")
                            }
                            scm {
                                connection.set("scm:git:https://github.com/rohanpadhye/jqf.git")
                                developerConnection.set("scm:git:https://github.com/rohanpadhye/jqf.git")
                                url.set("https://github.com/rohanpadhye/jqf")
                                tag.set("HEAD")
                            }
                        }
                    }
                }
            }
        }
    }
}
