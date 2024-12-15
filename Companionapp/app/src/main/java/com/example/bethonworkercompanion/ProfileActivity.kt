package com.example.bethonworkercompanion

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var clockTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0

    private val clockRunnable = object : Runnable {
        override fun run() {
            val elapsedTime = System.currentTimeMillis() - startTime
            val seconds = (elapsedTime / 1000) % 60
            val minutes = (elapsedTime / (1000 * 60)) % 60
            val hours = (elapsedTime / (1000 * 60 * 60)) % 24
            clockTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Fetch user profile data when the activity is created
        fetchUserProfile()

        // Set up the QR code scanner button
        val scanQrButton: Button = findViewById(R.id.scanQrButton)
        scanQrButton.setOnClickListener {
            startQrCodeScanner()
        }

        // Set up the logout button
        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logEndTime()
            // Optionally, navigate back to the login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Initialize the clock TextView
        clockTextView = findViewById(R.id.clockTextView)
    }

    private fun fetchUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Assuming the token is stored in SharedPreferences
                val token = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("auth_token", "") ?: ""
                val response = RetrofitClient.instance.getUserProfile("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val user = response.body()
                        Log.d("ProfileActivity", "User data: $user")
                        updateUI(user)
                    } else {
                        Log.e("ProfileActivity", "Failed to fetch user profile: ${response.errorBody()?.string()}")
                        Toast.makeText(this@ProfileActivity, "Failed to fetch user profile", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ProfileActivity", "Error: ${e.message}", e)
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateUI(user: User?) {
        findViewById<TextView>(R.id.nameTextView).text = user?.name
        findViewById<TextView>(R.id.workerIdTextView).text = user?.id.toString()
        findViewById<TextView>(R.id.jobTitleTextView).text = user?.title
        // Update other UI elements with user data
    }

    private fun startQrCodeScanner() {
        val qrScanIntegrator = IntentIntegrator(this)
        qrScanIntegrator.setPrompt("Scan a QR code")
        qrScanIntegrator.setOrientationLocked(false)
        qrScanIntegrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                // Send request to server to log start time
                logStartTime(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun logStartTime(qrCode: String) {
        // Create a map to pass to the logStartTime method
        val qrCodeMap = mapOf("qrCode" to qrCode)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.logStartTime(qrCodeMap)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Start time logged", Toast.LENGTH_LONG).show()
                        // Start the clock from 0:00:00
                        startTime = System.currentTimeMillis()
                        handler.post(clockRunnable)
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ProfileActivity", "Failed to log start time: $errorBody")
                        Toast.makeText(this@ProfileActivity, "Failed to log start time: $errorBody", Toast.LENGTH_LONG).show()
                        // Reset the clock
                        handler.removeCallbacks(clockRunnable)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ProfileActivity", "Error logging start time: ${e.message}", e)
                    Toast.makeText(this@ProfileActivity, "Error logging start time: ${e.message}", Toast.LENGTH_LONG).show()
                    // Reset the clock
                    handler.removeCallbacks(clockRunnable)
                }
            }
        }
    }

    private fun logEndTime() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.logEndTime()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "End time logged", Toast.LENGTH_LONG).show()
                        // Stop the clock
                        handler.removeCallbacks(clockRunnable)
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to log end time", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
    }
}