package com.rmdev.kulkasku.ui.profile

data class Profile(
    val username: String = "Unknown",
    val email: String = "Unknown",
    val phoneNumber: String = "",
    val profilePictureUrl: String? = null
)