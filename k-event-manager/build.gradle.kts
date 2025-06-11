plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {

    jvm() // dummy

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