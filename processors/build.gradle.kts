plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

group = "xyz.uthofficial"
version = "unspecified"

dependencies {
    implementation(libs.bundles.kotlinPoetWithKsp)
    implementation(libs.arrow)

    compileOnly(libs.autoServiceAnnotations)
    ksp(libs.autoServiceKsp)

    implementation(project(":annotations"))
    implementation(project(":errors"))

    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
    testImplementation(libs.kotlinCompileTestingKsp)
    testImplementation(libs.kotlinCompileTestingCore)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    jvmArgs("-Djdk.instrument.traceUsage=true")

    val tmpDir = layout.buildDirectory.dir("tmp")
    systemProperty("java.io.tmpdir", tmpDir.map { it.asFile.absolutePath }.get())
    doFirst { tmpDir.get().asFile.mkdirs() }
}