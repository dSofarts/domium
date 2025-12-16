abstract class Project {
    abstract val modules: List<Module>

    fun moduleByName(name: String) = modules.first { it.name == name }

    data class Module(
        val name: String,
        val group: String,
        val artifact: String = name,
        val version: String,
        val published: Boolean = true,
    )
}
