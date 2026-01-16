import org.springframework.boot.gradle.plugin.SpringBootPlugin

apply<SpringBootPlugin>()

dependencies {
    implementation(project(":${RootProject.CORE.name}"))
    implementation(project(":${RootProject.API.name}"))

    implementation("ru.domium:security:1.0.0")
    implementation("ru.domium:openapi:1.0.0")

    // SPRING COMPONENTS
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-rsocket")
    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery")

    // OTHER
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:r2dbc-postgresql")

    implementation("io.netty:netty-handler")

    runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.109.Final:osx-aarch_64")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("org.postgresql:postgresql")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
    }
}

tasks {
    bootJar {
        archiveFileName.set("chat-service.jar")
    }
}
