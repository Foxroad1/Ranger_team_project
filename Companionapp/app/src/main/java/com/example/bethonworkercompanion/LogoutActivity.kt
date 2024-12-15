package com.example.bethonworkercompanion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LogoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        // Log the logout time
        logLogoutTime()
    }

    private fun logLogoutTime() {
        val apiService = ApiClient.createService(ApiService::class.java)

        logWithTime("Sending logout request to server")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.logEndTime()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        logWithTime("Logout time logged successfully")
                    } else {
                        logWithTime("Failed to log logout time: ${response.errorBody()?.string()}")
                    }
                    navigateToLogin()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logWithTime("Error logging logout time: ${e.message}")
                    navigateToLogin()
                }
            }
        }
    }

    private fun navigateToLogin() {
        logWithTime("Navigating to LoginActivity")
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun logWithTime(message: String) {
        val utcTime = Instant.now()
        val helsinkiTime = utcTime.atZone(ZoneId.of("Europe/Helsinki"))
        val formattedTime = helsinkiTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        Log.d("LogoutActivity", "$formattedTime: $message")
    }
}