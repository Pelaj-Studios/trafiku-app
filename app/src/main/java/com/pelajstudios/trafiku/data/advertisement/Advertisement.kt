package com.pelajstudios.trafiku.data.advertisement

sealed interface Advertisement {
    val id: String
    val displayName: String
    val contentUri: String

    data class Image(
        override val id: String,
        override val displayName: String,
        override val contentUri: String,
    ) : Advertisement

    data class Video(
        override val id: String,
        override val displayName: String,
        override val contentUri: String,
    ) : Advertisement
}
