package info.benjaminhill.gdrivedl

import java.nio.file.Path
import java.nio.file.Paths


fun main() {
    processDir("1NOrBLUi-qyUjM1C4wangO69O-K9OyFoS", Paths.get("./downloads"))
}

fun processDir(remoteDirId: String, localDir: Path) {
    println(localDir)
    var pageToken: String? = null
    do {
        val result = service.files().list()
            .setQ("'$remoteDirId' in parents and trashed=false")
            .setPageSize(1000)
            .setFields("nextPageToken, files(id, name, mimeType)")
            .setPageToken(pageToken)
            .execute()

        result.files?.filterNotNull()?.forEach { file ->
            when {
                // recurse (don't create dirs yet)
                file.mimeType == "application/vnd.google-apps.folder" -> {
                    processDir(file.id, localDir.resolve(file.name.replace("\\s+", "_")))
                }
                file.mimeType.startsWith("application/vnd.google-apps") -> {
                    println("Skipping native file '${file.name}'")
                }
                else -> {
                    println("Downloading ${file.mimeType} '${file.name}' (${file.quotaBytesUsed})")
                    localDir.toFile().mkdirs()
                    localDir.resolve(file.name.replace("\\s+", "_")).toFile().outputStream().use { outputStream ->
                        service.files().get(file.id)
                            .executeMediaAndDownloadTo(outputStream)
                    }
                }
            }
        }

        pageToken = result.nextPageToken
    } while (pageToken != null)
}


/*
val outputStream: OutputStream = ByteArrayOutputStream()

*/
