package info.benjaminhill.gdrivedl

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.InputStreamReader

private const val APPLICATION_NAME = "Google Drive Downloader"
private val JSON_FACTORY: JsonFactory = JacksonFactory.getDefaultInstance()
private const val TOKENS_DIRECTORY_PATH = "tokens"

/**
 * Global instance of the scopes required by this app.
 * If modifying these scopes, delete your previously saved tokens/ folder.
 */
private val SCOPES = listOf(DriveScopes.DRIVE_READONLY)
private const val CREDENTIALS_FILE_PATH = "/credentials.json"
private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

private fun getCredentials(): Credential {
    object {}.javaClass.getResourceAsStream(CREDENTIALS_FILE_PATH).use { credFile ->
        require(credFile != null) { "Resource not found: $CREDENTIALS_FILE_PATH" }
        val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(credFile))
        // Build flow and trigger user authorization request.
        val flow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES
            )
            .setDataStoreFactory(FileDataStoreFactory(java.io.File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }
}

internal val service: Drive by lazy {
    Drive
        .Builder(httpTransport, JSON_FACTORY, getCredentials())
        .setApplicationName(APPLICATION_NAME)
        .build()!!
}
