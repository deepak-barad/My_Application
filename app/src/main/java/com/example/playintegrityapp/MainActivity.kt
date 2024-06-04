package com.deepakbarad.playintegrityapp

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest


class MainActivity : AppCompatActivity() {

    lateinit var integrityTokenProvider: StandardIntegrityTokenProvider
    lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupPlayIntegrity()
        tvResult = findViewById<TextView>(R.id.tvResult)

        findViewById<TextView>(R.id.tvTapHere).setOnClickListener {
            if(!::integrityTokenProvider.isInitialized) {
                tvResult.text = "integrityTokenProvider is not initialized"
                return@setOnClickListener
            }
            val requestHash = "2cp24z..."
            val integrityTokenResponse =
                integrityTokenProvider.request(
                    StandardIntegrityTokenRequest.builder()
                        .setRequestHash(requestHash)
                        .build()
                )
            integrityTokenResponse
                .addOnSuccessListener { response: StandardIntegrityToken ->
                    sendToServer(
                        response.token()
                    )
                }
                .addOnFailureListener { exception: java.lang.Exception? -> handleError(exception) }
        }
    }

    private fun sendToServer(token: String) {
        tvResult.text = token
    }

    private fun setupPlayIntegrity() {
        val standardIntegrityManager =
            IntegrityManagerFactory.createStandard(applicationContext)

        val cloudProjectNumber = 652911097673
        standardIntegrityManager.prepareIntegrityToken(
            PrepareIntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .build()
        )
            .addOnSuccessListener { tokenProvider: StandardIntegrityTokenProvider ->
                integrityTokenProvider = tokenProvider
            }
            .addOnFailureListener { exception: Exception? -> handleError(exception) }
    }

    private fun handleError(exception: Exception?) {
        println(exception)
        tvResult.text = "Exception $exception"
    }
}