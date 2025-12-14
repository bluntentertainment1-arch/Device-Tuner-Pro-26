package com.kidblunt.cleanerguru.data.model

import com.google.gson.annotations.SerializedName

data class CreateRecordRequest(
    @SerializedName("app_id")
    val appId: String,
    @SerializedName("table_name")
    val tableName: String,
    @SerializedName("data")
    val data: Map<String, Any>
)