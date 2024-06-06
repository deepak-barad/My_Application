package com.deepakbarad.playintegrityapp

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import org.jose4j.jwe.JsonWebEncryption
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwx.JsonWebStructure
import org.jose4j.lang.JoseException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


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
            requestAndDecode()
        }
    }

    private fun makeStandardRequest() {
        if (!::integrityTokenProvider.isInitialized) {
            tvResult.text = "integrityTokenProvider is not initialized"
            return
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
                    response
                )
                response
            }
            .addOnFailureListener { exception: java.lang.Exception? -> handleError(exception) }
    }

    private fun sendToServer(token: StandardIntegrityToken) {
        tvResult.text = token.token() + "\r\n\r\n" + token.toString()
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

    private fun requestAndDecode() {
        val TAG = "MainActivity"


// this code is put inside a function that
// can be called on a click event
// the code executes asynchronously
// watch the linked video for better clarity

// nonce must be base64 encoded
// details https://developer.android.com/google/play/integrity/verdict
// hardcoded only for tutorial but can be generated on your server
// and obtained by a secure http request
        val nonce = "MTIzNDU2Nzg5MDEyMzQ1Ng=="


// DECRYPTION_KEY, VERIFICATION_KEY are hard-coded for tutorial
// but can be stored in a safer way, for example on a server
// and obtained by a secure http request
        val DECRYPTION_KEY = "/ftM+CA15Ynz0yWs+4F7nf72a/J4A99T6iqy8iC3HxU="
        val VERIFICATION_KEY="MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAENtAjpnR5o/L82k6PHOOFE2HQqpGgRpAVqMn2fxmQQGcxRdX7IKe50WQC5EVWWmP3XVnL4T1GGowIzB6IogQKGQ=="


// Create an instance of a manager.
        val integrityManager =
            IntegrityManagerFactory.create(applicationContext)


// Request the integrity token by providing a nonce
        val integrityTokenResponse =
            integrityManager
                .requestIntegrityToken(
                    IntegrityTokenRequest.builder()
                        .setNonce(nonce)
                        .setCloudProjectNumber(652911097673)
                        .build()
                )
                .addOnSuccessListener(
                    (OnSuccessListener<IntegrityTokenResponse> { response: IntegrityTokenResponse ->
                        val integrityToken = response.token()
                        Log.d(TAG, integrityToken)

                        val decryptionKeyBytes: ByteArray =
                            Base64.decode(DECRYPTION_KEY, Base64.DEFAULT)

                        // SecretKey
                        val decryptionKey: SecretKey =
                            SecretKeySpec(
                                decryptionKeyBytes,
                                0,
                                decryptionKeyBytes.size,
                                "AES"
                            )

                        val encodedVerificationKey: ByteArray =
                            Base64.decode(VERIFICATION_KEY, Base64.DEFAULT)

                        // PublicKey
                        var verificationKey: PublicKey? = null

                        try {
                            verificationKey = KeyFactory.getInstance("EC")
                                .generatePublic(X509EncodedKeySpec(encodedVerificationKey))
                        } catch (e: InvalidKeySpecException) {
                            Log.d(TAG, e.message!!)
                        } catch (e: NoSuchAlgorithmException) {
                            Log.d(TAG, e.message!!)
                        }

                        // some error occurred so return
                        if (null == verificationKey) {
                            return@OnSuccessListener
                        }

                        // JsonWebEncryption
                        var jwe: JsonWebEncryption? = null
                        try {
                            jwe = JsonWebStructure
                                .fromCompactSerialization(integrityToken) as JsonWebEncryption
                        } catch (e: JoseException) {
                            e.printStackTrace()
                        }

                        // some error occurred so return
                        if (null == jwe) {
                            return@OnSuccessListener
                        }

                        jwe.key = decryptionKey

                        var compactJws: String? = null

                        try {
                            compactJws = jwe.payload
                        } catch (e: JoseException) {
                            Log.d(TAG, e.message!!)
                        }

                        // JsonWebSignature
                        var jws: JsonWebSignature? = null

                        try {
                            jws = JsonWebStructure
                                .fromCompactSerialization(compactJws) as JsonWebSignature
                        } catch (e: JoseException) {
                            Log.d(TAG, e.message!!)
                        }

                        // some error occurred so return
                        if (null == jws) {
                            return@OnSuccessListener
                        }

                        jws.key = verificationKey

                        // get the json human readable string
                        var jsonPlainVerdict: String? = ""

                        try {
                            jsonPlainVerdict = jws.payload
                        } catch (e: JoseException) {
                            Log.d(TAG, e.message!!)

                            return@OnSuccessListener
                        }

                        // payload is available in json format
                        // plain text, can be processed as per needs
                        Log.d(TAG, jsonPlainVerdict)
                        tvResult.text = jsonPlainVerdict
                    } as OnSuccessListener<IntegrityTokenResponse>)!!
                )
                .addOnFailureListener((OnFailureListener {
                    ex: java.lang.Exception? -> {
                        tvResult.text = "Exception $ex"
                }
                } as OnFailureListener)!!)
    }
}