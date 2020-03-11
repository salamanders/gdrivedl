package info.benjaminhill.gdrivedl

import com.google.api.services.drive.model.File
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

internal val nonAsciiRe = Regex("[^a-z0-9._\\-]+", RegexOption.IGNORE_CASE)
fun main() {
    processDir("root", Paths.get("./downloads"))
}

private fun processDir(remoteDirId: String, localDir: Path) {
    println(localDir)
    var pageToken: String? = null
    do {
        val result = service.files().list()
            .setQ("'$remoteDirId' in parents and trashed=false")
            .setPageSize(1000)
            .setFields("nextPageToken, files(id, name, mimeType, quotaBytesUsed)")
            .setPageToken(pageToken)
            .execute()

        result.files?.filterNotNull()?.forEach { file ->
            processFile(file, localDir)
        }

        pageToken = result.nextPageToken
    } while (pageToken != null)
}

private fun processFile(file: File, localDir: Path) = when {
    // recurse (don't create dirs yet)
    file.mimeType == "application/vnd.google-apps.folder" -> {
        processDir(file.id, localDir.resolve(file.name.replace(nonAsciiRe, "_")))
    }
    file.mimeType.startsWith("application/vnd.google-apps") -> {
        println("Skipping native file '${file.name}'")
    }
    file.quotaBytesUsed == 0L -> {
        println("Skipping file that doesn't use quota: ${file.name}")
    }
    else -> {
        val mb = file.quotaBytesUsed / (1024 * 1024)
        val name = file.name.replace(nonAsciiRe, "_")
        val targetFile = localDir.resolve(name).toFile()
        if (targetFile.exists()) {
            println("File already exists, skipping: $name")
        } else {
            print("Downloading ${file.mimeType} '$name' (~$mb mb)...")
            try {
                val tmp = Paths.get("downloadfile.tmp").toFile()

                tmp.outputStream().use { outputStream ->
                    service.files().get(file.id)
                        .executeMediaAndDownloadTo(outputStream)
                }

                localDir.toFile().mkdirs()
                Files.move(tmp.toPath(), targetFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                println(" done.")
            } catch (e: SocketTimeoutException) {
                println("SocketTimeoutException when downloading $name, skipping this time.")
            }
        }
    }
}


