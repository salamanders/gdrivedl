package info.benjaminhill.gdrivedl

import com.google.api.client.http.HttpResponseException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

internal val nonAsciiRe = Regex("[^a-z0-9._\\-]+", RegexOption.IGNORE_CASE)

data class SFile(val id: String, val mimeType: String, val quotaBytesUsed: Long, val name: String) : Serializable

@ExperimentalTime
internal val cacheFolderToFiles = SimpleCache<String, LinkedList<SFile>>()

@ExperimentalTime
fun main() {
    //dirToFiles("root").forEach { childFile ->
    //    processFile(childFile, Paths.get("./downloads"))
    //}

    dirToFiles("0ByXCDxKWoabVbnprMVlwLXJFaXc").forEach { childFile ->
        processFile(childFile, Paths.get("./downloads/my_computers"))
    }
}

/**
 * Page-aware Drive folder ID to children
 */
@ExperimentalTime
private fun dirToFiles(remoteDirId: String) = cacheFolderToFiles.getOrPut(remoteDirId) {
    val serList = LinkedList<SFile>()
    var pageToken: String? = null
    var pageNum = 0
    do {
        val result = GDrive.service.files().list()
            .setQ("'$remoteDirId' in parents and trashed=false")
            .setPageSize(1000)
            .setFields("nextPageToken, files(id, name, mimeType, quotaBytesUsed)")
            .setPageToken(pageToken)
            .execute()

        result.files?.filterNotNull()?.mapTo(serList) {
            SFile(it.id, it.mimeType, it.quotaBytesUsed, it.name)
        }
        pageToken = result.nextPageToken
        pageNum++
    } while (pageToken != null)
    serList
}

@ExperimentalTime
private fun processFile(file: SFile, parentDir: Path): Unit = when {
    // recurse (don't create local dirs yet)
    file.mimeType == "application/vnd.google-apps.folder" -> {
        try {
            dirToFiles(file.id).forEach { childFile ->
                processFile(childFile, parentDir.resolve(file.name.replace(nonAsciiRe, "_")))
            }
        } catch (e: Exception) {
            when (e) {
                is SocketTimeoutException -> runBlocking {
                    println("SocketTimeoutException whenlisting ${file.id}, skipping this time.")
                    delay(5.seconds.inMilliseconds.toLong())
                }
                is HttpResponseException -> println("HttpResponseException when downloading ${file.id}, skipping this time.")
                else -> throw e
            }
        }
    }
    file.mimeType.startsWith("application/vnd.google-apps") -> {
        println("Skipping native file '${file.name}'")
    }
    file.quotaBytesUsed == 0L -> {
        println("Skipping file that doesn't use quota: ${file.name}")
    }
    else -> {
        downloadFile(file, parentDir)
    }
}

private fun downloadFile(file: SFile, parentDir: Path) {
    val mb = file.quotaBytesUsed / (1024 * 1024)
    val name = file.name.replace(nonAsciiRe, "_")
    val targetFile = parentDir.resolve(name).toFile()
    if (targetFile.exists()) {
        println("File already exists, skipping: $name")
    } else {
        print("Downloading ${file.mimeType} '$name' (~$mb mb)...")
        try {
            val tmp = Paths.get("downloadfile.tmp").toFile()

            tmp.outputStream().use { outputStream ->
                GDrive.service.files().get(file.id)
                    .executeMediaAndDownloadTo(outputStream)
            }

            parentDir.toFile().mkdirs()
            Files.move(tmp.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
            println(" done.")
        } catch (e: SocketTimeoutException) {
            println("SocketTimeoutException when downloading $name, skipping this time.")
        }
    }
}


