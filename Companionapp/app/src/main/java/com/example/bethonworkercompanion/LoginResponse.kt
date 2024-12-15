// LoginResponse.kt
package com.example.bethonworkercompanion

data class LoginResponse(
    val success: Boolean,
    val userId: Int,
    val token: String,  // Add this line
    val message: String? = null
)