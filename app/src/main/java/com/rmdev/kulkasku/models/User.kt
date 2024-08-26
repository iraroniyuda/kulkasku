package com.rmdev.kulkasku.models

data class User(
    var username: String = "Unknown",
    var email: String = "Unknown",
    var bio: String = "",
    var isAdmin: Boolean = false,
    var phoneNumber: String = "",
    var profilePictureUrl: String? = null,
    var roles: Roles = Roles()
)

data class Roles(
    var creator: Boolean = false,
    var seller: Boolean = false,
    var distributor: Boolean = false,
    var courier: Boolean = false
)