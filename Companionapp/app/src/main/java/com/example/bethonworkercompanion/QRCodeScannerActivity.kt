package com.example.bethonworkercompanion

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QRCodeScannerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code_scanner)

        // Start the QR code scanner
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan a QR code")
        integrator.setCameraId(0)  // Use a specific camera of the device
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(true)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val qrCodeData = result.contents
                // Handle the scanned QR code
                validateQRCodeAndLocation(qrCodeData)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun validateQRCodeAndLocation(qrCodeData: String) {
        // Create a map to pass to the logStartTime method
        val qrCodeMap = mapOf("qrCode" to qrCodeData)

        // Call the logStartTime method with the map
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = logStartTime(qrCodeMap)
                if (response.isSuccessful) {
                    Toast.makeText(this@QRCodeScannerActivity, "Start time logged successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@QRCodeScannerActivity, "Failed to log start time: ${response.errorBody()?.string()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@QRCodeScannerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun logStartTime(qrCodeMap: Map<String, String>): Response<Void> {
        return withContext(Dispatchers.IO) {
            RetrofitClient.instance.logStartTime(qrCodeMap)
        }
    }
}