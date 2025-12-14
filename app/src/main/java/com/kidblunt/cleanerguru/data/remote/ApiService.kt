package com.kidblunt.cleanerguru.data.remote

import com.kidblunt.cleanerguru.data.model.CreateRecordRequest
import com.kidblunt.cleanerguru.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("data")
    suspend fun createUser(
        @Body request: CreateRecordRequest
    ): Response<UserResponse>
}