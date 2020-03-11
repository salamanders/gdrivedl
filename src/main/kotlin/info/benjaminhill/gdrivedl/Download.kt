package info.benjaminhill.gdrivedl


fun main() {
    val service = getDriveService()

    var pageToken: String? = null
    do {
        service.files().list()
            .setQ("'1NOrBLUi-qyUjM1C4wangO69O-K9OyFoS' in parents and trashed=false")
            .setPageSize(1000)
            .setFields("nextPageToken, files(id, name, mimeType)")
            .setPageToken(pageToken)
            .execute().apply {
                files?.forEach { file ->
                    println(file)
                }
                pageToken = nextPageToken
            }
    } while (pageToken != null)
}


/*
val outputStream: OutputStream = ByteArrayOutputStream()
driveService.files().get(fileId)
    .executeMediaAndDownloadTo(outputStream)
*/
