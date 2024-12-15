package com.example.bethonworkercompanion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LogoutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logout)

        // Log the logout time
        logLogoutTime()
    }

    private fun logLogoutTime() {
        val apiService = ApiClient.createService(ApiService::class.java)
        val call = apiService.logout()

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d("LogoutActivity", "Logout time logged successfully")
                } else {
                    Log.e("LogoutActivity", "Failed to log logout time")
                }
                navigateToLogin()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("LogoutActivity", "Error logging logout time", t)
                navigateToLogin()
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}