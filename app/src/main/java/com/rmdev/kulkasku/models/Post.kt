package com.rmdev.kulkasku.models

data class Post(
    var id: String = "",
    var userId: String = "",
    var username: String = "",
    var profilePictureUrl: String? = null,
    var timestamp: Long = 0,
    var content: String = "",
    var mediaUrl: String? = null,
    var mediaType: String? = null,
    var likesCount: Int = 0,
    var commentsCount: Int = 0
)