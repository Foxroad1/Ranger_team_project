package com.example.bethonworkercompanion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        apiService = RetrofitClient.instance

        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.editTextTextPassword)
        val loginButton = findViewById<Button>(R.id.login_button)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Debugging logs
            Log.d("LoginActivity", "Username: $username")
            Log.d("LoginActivity", "Password: $password")

            lifecycleScope.launch {
                try {
                    val loginRequest = LoginRequest(username, password) // Create LoginRequest object
                    val response = withContext(Dispatchers.IO) {
                        apiService.login(loginRequest) // Pass LoginRequest to API call
                    }

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        loginResponse?.let {
                            // Debugging log
                            Log.d("LoginActivity", "Token: ${it.token}")
                            // Store token in SharedPreferences
                            val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            sharedPrefs.edit().putString("auth_token", it.token).apply()

                            // Verify token storage
                            val storedToken = sharedPrefs.getString("auth_token", null)
                            if (storedToken == it.token) {
                                Log.d("LoginActivity", "Token successfully stored in SharedPreferences")
                                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                                // Navigate to ProfileActivity
                                val intent = Intent(this@LoginActivity, ProfileActivity::class.java)
                                startActivity(intent)
                                finish() // Optional: Call finish() if you don't want to keep the login activity in the back stack
                            } else {
                                Log.e("LoginActivity", "Token verification failed")
                                Toast.makeText(this@LoginActivity, "Token verification failed", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Log.e("LoginActivity", "Login failed: Response body is null")
                            Toast.makeText(this@LoginActivity, "Login failed: Response body is null", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("LoginActivity", "Login failed: $errorBody")
                        Toast.makeText(this@LoginActivity, "Login failed: $errorBody", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: HttpException) {
                    Log.e("LoginActivity", "HTTP error: ${e.message}", e)
                    Toast.makeText(this@LoginActivity, "HTTP error: ${e.message}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error: ${e.message}", e)
                    Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}