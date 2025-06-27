plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.fantamomo"
version = "1.0-SNAPSHOT"

kotlin {

    jvm() // dummy
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":k-event-api"))
                implementation("org.jetbrains.kotlin:kotlin-reflect")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}