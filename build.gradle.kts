plugins {
    `build-scan`
    kotlin("jvm") version "1.3.10"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testCompile("org.junit.jupiter:junit-jupiter-api:5.+")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.+")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
