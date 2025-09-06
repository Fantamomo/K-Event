plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.fantamomo"
version = "1.5-SNAPSHOT"

kotlin {

    jvm() // dummy
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}