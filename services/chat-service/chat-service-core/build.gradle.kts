dependencies {
    api(project(":${RootProject.API.name}"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    api("com.fasterxml.jackson.core:jackson-databind")
}
