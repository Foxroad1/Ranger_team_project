package com.example.bethonworkercompanion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId

class ProfileActivity : AppCompatActivity() {
    private lateinit var clockTextView: TextView
    private lateinit var qrScanLauncher: ActivityResultLauncher<Intent>
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

        qrScanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val result: IntentResult = IntentIntegrator.parseActivityResult(result.resultCode, data)
                if (result.contents == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
                } else {
                    logStartTime(result.contents)
                }
            }
        }

        fetchUserProfile()

        val scanQrButton: Button = findViewById(R.id.scanQrButton)
        scanQrButton.setOnClickListener {
            startQrCodeScanner()
        }

        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logEndTime()
        }

        clockTextView = findViewById(R.id.clockTextView)
    }

    private fun fetchUserProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val token = prefs.getString("auth_token", "") ?: ""
                Log.d("ProfileActivity", "Retrieved Token: $token")
                if (token.isNotEmpty()) {
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
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "Token is missing", Toast.LENGTH_LONG).show()
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
    }

    private fun startQrCodeScanner() {
        val qrScanIntegrator = IntentIntegrator(this)
        qrScanIntegrator.setPrompt("Scan a QR code")
        qrScanIntegrator.setOrientationLocked(false)
        val intent = qrScanIntegrator.createScanIntent()
        qrScanLauncher.launch(intent)
    }

    private fun logStartTime(qrCode: String) {
        val qrCodeMap = mapOf("qrCode" to qrCode)
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            val utcTime = Instant.now()
            val helsinkiTime = utcTime.atZone(ZoneId.of("Europe/Helsinki"))

            Log.d("ProfileActivity", "Helsinki time: $helsinkiTime")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.logStartTime("Bearer $token", qrCodeMap)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProfileActivity, "Start time logged", Toast.LENGTH_LONG).show()
                            startTime = helsinkiTime.toInstant().toEpochMilli()
                            handler.post(clockRunnable)
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("ProfileActivity", "Failed to log start time: $errorBody")
                            Toast.makeText(this@ProfileActivity, "Failed to log start time: $errorBody", Toast.LENGTH_LONG).show()
                            handler.removeCallbacks(clockRunnable)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("ProfileActivity", "Error logging start time: ${e.message}", e)
                        Toast.makeText(this@ProfileActivity, "Error logging start time: ${e.message}", Toast.LENGTH_LONG).show()
                        handler.removeCallbacks(clockRunnable)
                    }
                }
            }
        } else {
            Toast.makeText(this, "Token is missing!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logEndTime() {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response: Response<Void> = RetrofitClient.instance.logEndTime("Bearer $token")
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProfileActivity, "End time logged successfully", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("ProfileActivity", "Failed to log end time: $errorBody")
                            Toast.makeText(this@ProfileActivity, "Failed to log end time: $errorBody", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("ProfileActivity", "Error logging end time: ${e.message}", e)
                        Toast.makeText(this@ProfileActivity, "Error logging end time: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Token is missing!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockRunnable)
    }
}