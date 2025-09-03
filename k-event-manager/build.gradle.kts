plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.fantamomo"
version = "1.1-SNAPSHOT"

kotlin {

    jvm() // dummy

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation(project(":k-event-api"))
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
        all {
            languageSettings.enableLanguageFeature("BreakContinueInInlineLambdas")
        }
    }
}