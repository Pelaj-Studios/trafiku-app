package com.pelajstudios.trafiku.data.advertisement

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MediaStoreAdvertisementDataSource(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun getAdvertisements(): List<Advertisement> = withContext(ioDispatcher) {
        queryImagesSafely()
            .plus(queryVideosSafely())
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Advertisement::displayName))
    }

    private fun queryImagesSafely(): List<Advertisement> = try {
        queryImages()
    } catch (exception: SecurityException) {
        emptyList()
    }

    private fun queryVideosSafely(): List<Advertisement> = try {
        queryVideos()
    } catch (exception: SecurityException) {
        emptyList()
    }

    private fun queryImages(): List<Advertisement> = queryMedia(
        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        supportedMimeTypes = IMAGE_MIME_TYPES,
    ) { id, name, uri ->
        Advertisement.Image(
            id = "image:$id",
            displayName = name,
            contentUri = uri.toString(),
        )
    }

    private fun queryVideos(): List<Advertisement> = queryMedia(
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        supportedMimeTypes = VIDEO_MIME_TYPES,
    ) { id, name, uri ->
        Advertisement.Video(
            id = "video:$id",
            displayName = name,
            contentUri = uri.toString(),
        )
    }

    private fun queryMedia(
        collection: Uri,
        supportedMimeTypes: Set<String>,
        toAdvertisement: (Long, String, Uri) -> Advertisement,
    ): List<Advertisement> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val mimePlaceholders = supportedMimeTypes.joinToString(separator = ",") { "?" }
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.MediaColumns.MIME_TYPE} IN ($mimePlaceholders)"
        val selectionArguments = arrayOf(ADVERTISEMENT_RELATIVE_PATH) + supportedMimeTypes

        return buildList {
            contentResolver.query(
                collection,
                projection,
                selection,
                selectionArguments,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: continue
                    add(toAdvertisement(id, name, ContentUris.withAppendedId(collection, id)))
                }
            }
        }
    }

    private companion object {
        const val ADVERTISEMENT_RELATIVE_PATH = "DCIM/TrafikuAds/"

        val IMAGE_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
        )
        val VIDEO_MIME_TYPES = setOf(
            "video/mp4",
            "video/webm",
        )
    }
}
