// LoginResponse.kt
package com.example.bethonworkercompanion

data class LoginResponse(
    val success: Boolean,
    val userId: Int,
    val message: String? = null
)