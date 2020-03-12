# gdrivedl
Google Drive Downloader

Kotlin script that recursively downloads all non-Google-native files to a local folder. 

## Setup

Requires you to create a Google Cloud project with the Drive API enabled, 
and download the `resources/credentials.json` file.

## Features and TODOs
 
 -[x] Skip already completed files
 -[x] Cache the folder listing API calls
 -[x] Safe characters in local file names
 -[x] Download to a temp file and atomically move
 -[x] Skip 0-quota files
