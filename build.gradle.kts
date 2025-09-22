plugins {
    kotlin("multiplatform") version "2.2.10" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}
