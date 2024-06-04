package com.deepakbarad.playintegrityapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider


class MainActivity : AppCompatActivity() {
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
    }

    private fun setupPlayIntegrity() {
        val standardIntegrityManager =
            IntegrityManagerFactory.createStandard(applicationContext)
        var integrityTokenProvider: StandardIntegrityTokenProvider
        val cloudProjectNumber = 0L
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
    }
}