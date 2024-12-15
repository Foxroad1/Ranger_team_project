package com.example.bethonworkercompanion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response

class QRCodeScannerActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_scanner)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationPermissionCode)
        } else {
            // Permissions already granted, proceed with initializing the QR code scanner
            initQRCodeScanner()
        }
    }

    private fun initQRCodeScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a QR code")
        integrator.setCameraId(0)  // Use a specific camera of the device
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show()
                // Permissions granted, proceed with initializing the QR code scanner
                initQRCodeScanner()
            } else {
                Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val qrCodeData = result.contents
                // Validate the QR code and location
                validateQRCodeAndLocation(qrCodeData)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun validateQRCodeAndLocation(qrCodeData: String) {
        // Parse the QR code data
        val qrCodeObject = JSONObject(qrCodeData)
        val locations = qrCodeObject.getJSONArray("locations")
        val companyId = qrCodeObject.getString("company_id")

        // Validate the QR code
        if (companyId != "Bethon Worker") {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_LONG).show()
            return
        }

        // Get the current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
            return
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            val currentLatitude = location.latitude
            val currentLongitude = location.longitude

            // Check if the current location is near any of the valid locations
            for (i in 0 until locations.length()) {
                val validLocation = locations.getJSONObject(i)
                val validLatitude = validLocation.getDouble("latitude")
                val validLongitude = validLocation.getDouble("longitude")

                if (isNearLocation(currentLatitude, currentLongitude, validLatitude, validLongitude)) {
                    // Log start time if the location is valid
                    logStartTime(qrCodeData)
                    return
                }
            }

            Toast.makeText(this, "Location is not valid", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Could not get current location", Toast.LENGTH_LONG).show()
        }
    }

    private fun isNearLocation(currentLatitude: Double, currentLongitude: Double, validLatitude: Double, validLongitude: Double): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(currentLatitude, currentLongitude, validLatitude, validLongitude, results)
        return results[0] < 100 // Example: 100 meters radius
    }

    private fun logStartTime(qrCodeData: String) {
        // Create a map to pass to the logStartTime method
        val qrCodeMap = mapOf("qrCode" to qrCodeData)

        // Retrieve the token from SharedPreferences (adjust the key as needed)
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            // Call the logStartTime method with the token and the map
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.logStartTime("Bearer $token", qrCodeMap)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@QRCodeScannerActivity, "Start time logged", Toast.LENGTH_LONG).show()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(this@QRCodeScannerActivity, "Failed to log start time: $errorBody", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QRCodeScannerActivity, "Error logging start time: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Token is missing!", Toast.LENGTH_SHORT).show()
        }
    }
}