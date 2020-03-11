package info.benjaminhill.gdrivedl

import com.google.api.services.drive.model.File
import java.nio.file.Path
import java.nio.file.Paths


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

private fun processFile(file: File, localDir: Path) {
    when {
        // recurse (don't create dirs yet)
        file.mimeType == "application/vnd.google-apps.folder" -> {
            processDir(file.id, localDir.resolve(file.name.replace("\\s+", "_")))
        }
        file.mimeType.startsWith("application/vnd.google-apps") -> {
            println("Skipping native file '${file.name}'")
        }
        file.quotaBytesUsed?.let { it > 0 } ?: false -> {
            println("Downloading ${file.mimeType} '${file.name}' (${file.quotaBytesUsed})")
            localDir.toFile().mkdirs()
            localDir.resolve(file.name.replace("\\s+", "_")).toFile().outputStream().use { outputStream ->
                service.files().get(file.id)
                    .executeMediaAndDownloadTo(outputStream)
            }
        }
        else -> println("Skipping file that doesn't use quota: ${file.name}")
    }
}

