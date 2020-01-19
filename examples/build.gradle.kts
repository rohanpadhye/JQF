dependencies {
    api(project(":jqf-fuzz"))
    api("com.pholser:junit-quickcheck-core")
    api("com.pholser:junit-quickcheck-generators")
    api("org.jgrapht:jgrapht-core")
    api("com.google.guava:guava")
    api("org.lichess:scalachess_2.12")
    api("org.apache.bcel:bcel")

    testImplementation(project(":jqf-instrument"))

    testImplementation("org.jgrapht:jgrapht-ext")
    testImplementation("org.apache.commons:commons-compress")
    testImplementation("org.apache.commons:commons-collections4")
    testImplementation("org.apache.commons:commons-lang3")
    testImplementation("org.apache.commons:commons-math3")
    testImplementation("org.apache.struts:struts2-core")
    testImplementation("org.apache.tomcat:tomcat-coyote")
    testImplementation("org.apache.tomcat:tomcat-catalina")
    testImplementation("org.apache.maven:maven-model-builder")
    testImplementation("com.google.javascript:closure-compiler")
    testImplementation("org.mozilla:rhino")
    testImplementation("org.apache.ant:ant")
    testImplementation("com.google.errorprone:error_prone_check_api")
    testImplementation("com.google.errorprone:error_prone_test_helpers") {
        // It uses 4.13-SNAPSHOT
        exclude("junit", "junit")
    }
    testImplementation("com.google.errorprone:error_prone_core")
    testImplementation("com.google.guava:guava-testlib")
    testImplementation("org.apache.tika:tika-parsers")
}

repositories {
    maven {
        url = uri("https://raw.githubusercontent.com/ornicar/lila-maven/master")
    }
}

tasks {
    test {
        enabled = false
    }
}
