dependencies {
    implementation("org.ow2.asm:asm")
}

tasks.jar {
    manifest {
        attributes["Premain-Class"] = "janala.instrument.SnoopInstructionTransformer"
        attributes["Can-Redefine-Classes"] = true
        attributes["Can-Retransform-Classes"] = true
    }
}
