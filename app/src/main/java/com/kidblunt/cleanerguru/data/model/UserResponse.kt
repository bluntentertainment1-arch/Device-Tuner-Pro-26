package com.kidblunt.cleanerguru.data.model

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("provider")
    val provider: String = "",
    @SerializedName("created_at")
    val createdAt: String? = null
)