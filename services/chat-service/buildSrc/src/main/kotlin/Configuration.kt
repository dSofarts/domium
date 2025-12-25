const val PROJECT_NAME = "chat-service"
const val GROUP_NAME = "ru.domium"
const val VERSION = "0.0.1"

@Suppress("MemberVisibilityCanBePrivate")
object RootProject : Project() {
    val API =
        Module(
            name = "$PROJECT_NAME-api",
            group = "$GROUP_NAME.$PROJECT_NAME",
            version = VERSION,
        )
    val CORE =
        Module(
            name = "$PROJECT_NAME-core",
            group = "$GROUP_NAME.$PROJECT_NAME",
            version = VERSION,
        )
    val MAIN =
        Module(
            name = "$PROJECT_NAME-main",
            group = "$GROUP_NAME.$PROJECT_NAME",
            version = VERSION,
        )

    override val modules = listOf(API, CORE, MAIN)
}

object Version {
    const val KOTLIN = "2.2.21"
    const val KTLINT = "13.1.0"

    const val SPRING_BOOT = "3.5.8"
    const val SPRING_DEPENDENCY = "1.1.7"

    const val LOGGING = "7.0.13"
}
