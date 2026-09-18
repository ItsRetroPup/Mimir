package pup.app.mimir.domain

object RelativePaths {
    fun parentOf(path: String): String =
        path.substringBeforeLast('/', "")

    fun nameOf(path: String): String =
        path.substringAfterLast('/')

    fun join(parent: String, child: String): String =
        if (parent.isBlank()) child else "$parent/$child"

    fun normalize(path: String): String =
        path.split('/').filter { it.isNotBlank() }.joinToString("/")
}
