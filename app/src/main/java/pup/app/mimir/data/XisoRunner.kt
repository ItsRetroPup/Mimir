package pup.app.mimir.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import pup.app.mimir.domain.FileOperation
import pup.app.mimir.domain.ImageReader
import pup.app.mimir.domain.Xiso
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Rewrites Xbox disc images as trimmed XISOs directly through the Storage Access Framework, without
 * staging the multi-gigabyte source in the app cache. The XISO keeps the source image's name.
 */
class XisoRunner(private val context: Context) {
    /** Returns the XDVDFS partition offset, 0 for an existing XISO, or null for a non-Xbox image. */
    fun partitionOffset(file: DocumentFile): Long? = runCatching {
        withReader(file.uri) { reader, size -> Xiso.findPartitionOffset(reader, size) }
    }.getOrNull()

    /** Replaces the source image with its XISO and returns the new image size in bytes. */
    fun convertInPlace(
        root: DocumentFile,
        operation: FileOperation.ConvertWithTool,
        cancellation: OperationCancellation? = null,
        onProgress: ((Float) -> Unit)? = null,
    ): Long {
        val source = findFile(root, operation.sourcePath)
            ?: error("Missing source image: ${operation.sourcePath}")
        val sourceName = source.name ?: error("Source image has no name.")
        val parent = source.parentFile ?: error("Unable to access the folder containing ${operation.sourcePath}")
        val tempName = "$sourceName$TEMP_SUFFIX"
        parent.findFile(tempName)?.delete()
        val temp = parent.createFile("application/octet-stream", tempName)
            ?: error("Unable to create a temporary XISO next to ${operation.sourcePath}")
        val outputSize = try {
            context.contentResolver.openOutputStream(temp.uri, "w").use { output ->
                requireNotNull(output) { "Unable to write ${temp.name}" }
                withReader(source.uri) { reader, size ->
                    Xiso.rewrite(
                        reader = reader,
                        imageSize = size,
                        output = output.buffered(OUTPUT_BUFFER_BYTES),
                        onProgress = { onProgress?.invoke(it) },
                        ensureActive = {
                            if (cancellation?.shouldInterruptCurrentOperation() == true) {
                                throw OperationStoppedException("Conversion stopped by user.")
                            }
                        },
                    )
                }
            }
        } catch (error: Throwable) {
            temp.delete()
            throw error
        }
        require(source.delete()) {
            temp.delete()
            "Failed to replace ${operation.sourcePath} with its XISO."
        }
        require(temp.renameTo(sourceName)) {
            "The XISO for ${operation.sourcePath} was saved as ${temp.name} but could not be renamed."
        }
        return outputSize
    }

    private fun <T> withReader(uri: Uri, block: (ImageReader, Long) -> T): T {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IOException("Unable to read $uri")
        return descriptor.use {
            FileInputStream(it.fileDescriptor).channel.use { channel ->
                block(ChannelImageReader(channel), channel.size())
            }
        }
    }

    private class ChannelImageReader(private val channel: FileChannel) : ImageReader {
        override fun readFully(position: Long, buffer: ByteArray, offset: Int, length: Int) {
            val target = ByteBuffer.wrap(buffer, offset, length)
            while (target.hasRemaining()) {
                val read = channel.read(target, position + (target.position() - offset))
                if (read < 0) throw IOException("Unexpected end of disc image.")
            }
        }
    }

    private fun findFile(root: DocumentFile, relativePath: String): DocumentFile? {
        var current = root
        val parts = relativePath.split('/').filter(String::isNotBlank)
        parts.forEachIndexed { index, part ->
            val child = current.findFile(part) ?: return null
            if (index == parts.lastIndex) return child
            current = child
        }
        return null
    }

    private companion object {
        const val TEMP_SUFFIX = ".mimir-xiso"
        const val OUTPUT_BUFFER_BYTES = 1024 * 1024
    }
}
