import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Version.KOTLIN
    kotlin("kapt") version Version.KOTLIN
    kotlin("plugin.spring") version Version.KOTLIN

    id("org.springframework.boot") version Version.SPRING_BOOT
    id("io.spring.dependency-management") version Version.SPRING_DEPENDENCY

    id("org.jlleitschuh.gradle.ktlint") version Version.KTLINT
}

apply(from = "repositories.gradle.kts")

subprojects {
    val module = RootProject.moduleByName(name)

    group = module.group
    version = module.version

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    apply(from = "$rootDir/repositories.gradle.kts")

    tasks.withType<Jar> {
        manifest { attributes("Implementation-Version" to VERSION) }
    }

    tasks.withType<Test> {
        @Suppress("UnstableApiUsage")
        useJUnitPlatform()
        testLogging.showStandardStreams = true
        jvmArgs =
            mutableListOf(
                "-Djava.net.preferIPv4Stack=true",
                "--add-opens",
                "java.base/java.util=ALL-UNNAMED",
            )
    }

    val javaVersion = JavaVersion.VERSION_21
    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.set(listOf("-Xjsr305=strict"))
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
        }
    }

    dependencies {
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.projectreactor:reactor-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
        testImplementation("io.mockk:mockk:1.14.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

configure(subprojects.filter { it.name != RootProject.API.name }) {
    dependencies {
        kapt("org.springframework.boot:spring-boot-configuration-processor")

        implementation("io.github.oshai:kotlin-logging:${Version.LOGGING}")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("com.google.guava:guava:33.4.8-jre")
    }
}

configure(subprojects) {
    @Suppress("UnstableApiUsage")
    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
}

configure(subprojects.filter { it.name != RootProject.MAIN.name }) {
    tasks.named("bootJar") {
        enabled = false
    }
}

tasks.bootJar {
    mainClass.set("ru.domium.chat.ChatServiceApplicationKt")
}
