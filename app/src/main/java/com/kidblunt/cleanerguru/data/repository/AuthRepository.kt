package com.kidblunt.cleanerguru.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.JsonSyntaxException
import com.kidblunt.cleanerguru.data.model.ApiResult
import com.kidblunt.cleanerguru.data.model.CreateRecordRequest
import com.kidblunt.cleanerguru.data.model.UserResponse
import com.kidblunt.cleanerguru.data.remote.RetrofitClient
import com.kidblunt.cleanerguru.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun registerAnonymousUser(): ApiResult<UserResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting anonymous user registration")
            
            val request = CreateRecordRequest(
                appId = Constants.APP_ID,
                tableName = "users",
                data = mapOf("provider" to "anonymous")
            )

            val response = RetrofitClient.apiService.createUser(request)
            
            when {
                response.isSuccessful && response.body() != null -> {
                    val userResponse = response.body()!!
                    Log.d(TAG, "User registered successfully: ${userResponse.id}")
                    
                    saveUserCredentials(userResponse.id)
                    
                    ApiResult.Success(userResponse)
                }
                response.code() == 401 -> {
                    Log.e(TAG, "Unauthorized: ${response.message()}")
                    ApiResult.Error("Session expired. Please login again", 401)
                }
                response.code() == 403 -> {
                    Log.e(TAG, "Forbidden: ${response.message()}")
                    ApiResult.Error("Access denied", 403)
                }
                response.code() == 404 -> {
                    Log.e(TAG, "Not found: ${response.message()}")
                    ApiResult.Error("Resource not found", 404)
                }
                response.code() >= 500 -> {
                    Log.e(TAG, "Server error: ${response.code()} - ${response.message()}")
                    ApiResult.Error("Server error. Please try again later", response.code())
                }
                else -> {
                    Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                    ApiResult.Error("Error: ${response.message()}", response.code())
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            ApiResult.Error("No internet connection")
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error: ${e.code()}", e)
            ApiResult.Error("Network error: ${e.message()}", e.code())
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON parsing error", e)
            ApiResult.Error("Invalid response format")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            ApiResult.Error("Unexpected error: ${e.localizedMessage}")
        }
    }

    private fun saveUserCredentials(userId: String) {
        val email = "${System.currentTimeMillis()}@anonymous.com"
        sharedPreferences.edit().apply {
            putString(Constants.KEY_USER_ID, userId)
            putString(Constants.KEY_USER_EMAIL, email)
            putBoolean(Constants.KEY_IS_LOGGED_IN, true)
            apply()
        }
        Log.d(TAG, "User credentials saved: userId=$userId, email=$email")
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(Constants.KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(Constants.KEY_USER_ID, null)
    }
}