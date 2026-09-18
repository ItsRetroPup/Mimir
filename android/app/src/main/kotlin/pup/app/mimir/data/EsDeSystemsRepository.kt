package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class EsDeSystem(
    val name: String,
    val fullName: String,
    val defaultPath: String,
    val romFolder: String,
) {
    val isDefault: Boolean
        get() = defaultPath.startsWith("%ROMPATH%/") && romFolder == defaultPath.removePrefix("%ROMPATH%/")
}

class EsDeSystemsRepository(private val context: Context) {
    suspend fun loadInstalled(rootUri: Uri): List<EsDeSystem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        val file = findFile(root, listOf(CUSTOM_SYSTEMS_DIR, SYSTEMS_FILE))
            ?: return@withContext emptyList()
        parseSystems(context.contentResolver.openInputStream(file.uri)?.use { it.readBytes() } ?: return@withContext emptyList())
    }

    suspend fun fetchLatest(): List<EsDeSystem> = withContext(Dispatchers.IO) {
        parseSystems(mergeCatalogXml(download(OFFICIAL_SYSTEMS_URL), download(LATEST_SYSTEMS_URL)))
    }

    suspend fun scanRomFolders(rootUri: Uri): List<String> = withContext(Dispatchers.IO) {
        DocumentFile.fromTreeUri(context, rootUri)
            ?.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) }
            ?.sortedBy(String::lowercase)
            .orEmpty()
    }

    fun systemsForRomFolders(catalog: List<EsDeSystem>, folders: List<String>): List<EsDeSystem> {
        val byName = catalog.associateBy { it.name.lowercase() }
        return folders.mapNotNull { folder ->
            val system = byName[folder.lowercase()] ?: return@mapNotNull null
            system.copy(
                defaultPath = "%ROMPATH%/$folder",
                romFolder = folder,
            )
        }
    }

    suspend fun installLatest(
        rootUri: Uri,
        systems: List<EsDeSystem>,
    ): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to open the selected ES-DE folder.")
        val customSystems = root.findFile(CUSTOM_SYSTEMS_DIR)?.takeIf { it.isDirectory }
            ?: root.createDirectory(CUSTOM_SYSTEMS_DIR)
            ?: error("Unable to create $CUSTOM_SYSTEMS_DIR in the selected ES-DE folder.")

        val systemsXml = buildInstalledXml(
            mergeCatalogXml(download(OFFICIAL_SYSTEMS_URL), download(LATEST_SYSTEMS_URL)),
            systems,
        )
        writeFile(customSystems, SYSTEMS_FILE, systemsXml)
        writeFile(customSystems, FIND_RULES_FILE, download(LATEST_FIND_RULES_URL))
        parseSystems(systemsXml).size
    }

    suspend fun installCurrent(
        rootUri: Uri,
        systems: List<EsDeSystem>,
    ): Int = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: error("Unable to open the selected ES-DE folder.")
        val customSystems = root.findFile(CUSTOM_SYSTEMS_DIR)?.takeIf { it.isDirectory }
            ?: root.createDirectory(CUSTOM_SYSTEMS_DIR)
            ?: error("Unable to create $CUSTOM_SYSTEMS_DIR in the selected ES-DE folder.")
        val currentFile = customSystems.findFile(SYSTEMS_FILE)
        val currentXml = currentFile?.let {
            context.contentResolver.openInputStream(it.uri)?.use { input -> input.readBytes() }
        }
        val systemsXml = if (currentXml != null) {
            buildInstalledXml(currentXml, systems)
        } else {
            buildInstalledXml(
                mergeCatalogXml(download(OFFICIAL_SYSTEMS_URL), download(LATEST_SYSTEMS_URL)),
                systems,
            )
        }
        writeFile(customSystems, SYSTEMS_FILE, systemsXml)
        if (customSystems.findFile(FIND_RULES_FILE) == null) {
            writeFile(customSystems, FIND_RULES_FILE, download(LATEST_FIND_RULES_URL))
        }
        parseSystems(systemsXml).size
    }

    private fun buildInstalledXml(xml: ByteArray, systems: List<EsDeSystem>): ByteArray {
        val document = parseDocument(xml)
        val selected = systems.associateBy { it.name }
        val systemNodes = document.getElementsByTagName("system")
        for (index in systemNodes.length - 1 downTo 0) {
            val system = systemNodes.item(index) as? Element ?: continue
            val name = system.childText("name") ?: continue
            val selectedSystem = selected[name]
            if (selectedSystem == null) {
                system.parentNode.removeChild(system)
                continue
            }
            val path = system.childElements("path").firstOrNull() ?: continue
            path.textContent = if (selectedSystem.isDefault) {
                "%ROMPATH%/${selectedSystem.romFolder}"
            } else {
                selectedSystem.romFolder
            }
        }
        return serialize(document)
    }

    private fun mergeCatalogXml(baseXml: ByteArray, customXml: ByteArray): ByteArray {
        val base = parseDocument(baseXml)
        val custom = parseDocument(customXml)
        val baseList = base.documentElement
        val baseByName = base.getElementsByTagName("system").let { nodes ->
            buildMap {
                for (index in 0 until nodes.length) {
                    val element = nodes.item(index) as? Element ?: continue
                    element.childText("name")?.let { put(it, element) }
                }
            }
        }
        val customSystems = custom.getElementsByTagName("system")
        for (index in 0 until customSystems.length) {
            val customSystem = customSystems.item(index) as? Element ?: continue
            val name = customSystem.childText("name") ?: continue
            val imported = base.importNode(customSystem, true)
            val existing = baseByName[name]
            if (existing != null) {
                baseList.replaceChild(imported, existing)
            } else {
                baseList.appendChild(imported)
            }
        }
        return serialize(base)
    }

    private fun parseSystems(xml: ByteArray): List<EsDeSystem> {
        val document = parseDocument(xml)
        val systems = document.getElementsByTagName("system")
        return buildList {
            for (index in 0 until systems.length) {
                val system = systems.item(index) as? Element ?: continue
                val name = system.childText("name")?.trim().orEmpty()
                val fullName = system.childText("fullname")?.trim().orEmpty().ifBlank { name }
                val path = system.childText("path")?.trim().orEmpty()
                if (name.isBlank() || path.isBlank()) continue
                add(
                    EsDeSystem(
                        name = name,
                        fullName = fullName,
                        defaultPath = path,
                        romFolder = path.removePrefix("%ROMPATH%/").removePrefix("%ROMPATH%"),
                    )
                )
            }
        }.distinctBy { it.name }
    }

    private fun writeFile(directory: DocumentFile, name: String, contents: ByteArray) {
        val file = directory.findFile(name)
            ?: directory.createFile("application/xml", name)
            ?: error("Unable to create $name in $CUSTOM_SYSTEMS_DIR.")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
            output.write(contents)
        } ?: error("Unable to write $name to the selected ES-DE folder.")
    }

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "Mimir/ES-DE-custom-systems")
        return try {
            Log.d(TAG, "Downloading $url")
            require(connection.responseCode in 200..299) {
                "GitHub returned HTTP ${connection.responseCode} while downloading ES-DE definitions."
            }
            connection.inputStream.use { it.readBytes() }.also {
                Log.d(TAG, "Downloaded ${it.size} bytes from $url")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseDocument(xml: ByteArray): Document =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }.newDocumentBuilder().parse(ByteArrayInputStream(xml))

    private fun serialize(document: Document): ByteArray = ByteArrayOutputStream().also { output ->
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.INDENT, "yes")
        }.transform(DOMSource(document), StreamResult(output))
    }.toByteArray()

    private fun findFile(root: DocumentFile, segments: List<String>): DocumentFile? {
        var current: DocumentFile? = root
        for (segment in segments) current = current?.findFile(segment) ?: return null
        return current
    }

    private fun Element.childText(name: String): String? =
        childElements(name).firstOrNull()?.textContent

    private fun Element.childElements(name: String): List<Element> =
        childNodes.let { nodes ->
            buildList {
                for (index in 0 until nodes.length) {
                    val child = nodes.item(index) as? Element ?: continue
                    if (child.tagName == name) add(child)
                }
            }
        }

    companion object {
        private const val TAG = "Mimir.ESDE"
        const val CUSTOM_SYSTEMS_DIR = "custom_systems"
        const val SYSTEMS_FILE = "es_systems.xml"
        const val FIND_RULES_FILE = "es_find_rules.xml"
        const val LATEST_SYSTEMS_URL =
            "https://raw.githubusercontent.com/GlazedBelmont/es-de-android-custom-systems/main/es_systems.xml"
        const val LATEST_FIND_RULES_URL =
            "https://raw.githubusercontent.com/GlazedBelmont/es-de-android-custom-systems/main/es_find_rules.xml"

        fun absolutePathForTreeUri(uri: Uri): String? {
            val documentId = uri.path
                ?.substringAfterLast("/tree/", "")
                ?.let(Uri::decode)
                ?.takeIf { it.isNotBlank() }
                ?: return null
            return absolutePathForDocumentId(documentId)
        }

        fun childTreeUri(rootUri: Uri, childName: String): Uri? {
            val documentId = rootUri.path
                ?.substringAfterLast("/tree/", "")
                ?.let(Uri::decode)
                ?.takeIf { it.isNotBlank() }
                ?: return null
            val childId = "$documentId/${childName.trim('/')}"
            return DocumentsContract.buildTreeDocumentUri(rootUri.authority, childId)
        }

        private const val OFFICIAL_SYSTEMS_URL =
            "https://gitlab.com/es-de/emulationstation-de/-/raw/stable-3.0/resources/systems/android/es_systems.xml"

        internal fun absolutePathForDocumentId(documentId: String): String? {
            val separator = documentId.indexOf(':')
            if (separator <= 0) return null
            val volume = documentId.substring(0, separator)
            val relativePath = documentId.substring(separator + 1).trim('/')
            val base = if (volume.equals("primary", ignoreCase = true)) {
                "/storage/emulated/0"
            } else {
                "/storage/$volume"
            }
            return if (relativePath.isBlank()) base else "$base/$relativePath"
        }
    }
}
