package com.rmdev.kulkasku.ui.profile

data class Comment(
    val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    var username: String = "",
    var profileImageUrl: String? = null
)