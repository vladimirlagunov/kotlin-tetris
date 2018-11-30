plugins {
    `build-scan`
    kotlin("jvm") version "1.3.0"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testCompile("org.junit.jupiter:junit-jupiter-api:5.+")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.+")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
