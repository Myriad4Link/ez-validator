plugins {
    id("buildsrc.convention.kotlin-jvm")
}

group = "xyz.uthofficial"
version = "unspecified"

dependencies {
    implementation(libs.bundles.kotlinPoetWithKsp)
    implementation(libs.arrow)

    implementation(project(":annotations"))
    implementation(project(":errors"))
}