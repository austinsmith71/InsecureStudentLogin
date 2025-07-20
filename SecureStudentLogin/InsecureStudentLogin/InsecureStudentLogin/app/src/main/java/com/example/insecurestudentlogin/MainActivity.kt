package com.example.insecurestudentlogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    lateinit var name_input: EditText
    lateinit var class_input: EditText
    lateinit var password_input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecurityHelper.init(applicationContext)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        name_input = findViewById(R.id.name_input)
        class_input = findViewById(R.id.class_input)
        password_input = findViewById(R.id.password_input)

        val loginButton = findViewById<View>(R.id.login_button)
        val biometricButton = findViewById<View>(R.id.biometric_login_button)

        loginButton.isEnabled = false
        biometricButton.isEnabled = true

        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val nameFilled = name_input.text.toString().trim().isNotEmpty()
                val classFilled = class_input.text.toString().trim().isNotEmpty()
                val passwordFilled = password_input.text.toString().trim().isNotEmpty()
                val formComplete = nameFilled && classFilled && passwordFilled

                loginButton.isEnabled = formComplete
                biometricButton.isEnabled = formComplete
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        name_input.addTextChangedListener(watcher)
        class_input.addTextChangedListener(watcher)
        password_input.addTextChangedListener(watcher)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun login_click(view: View?) {
        sendLoginRequest(
            name_input.text.toString().trim(),
            class_input.text.toString().trim(),
            password_input.text.toString().trim()
        )
    }

    private fun showBiometricPrompt(token: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                sendMessageWithToken(token)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use your fingerprint to log in")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    fun biometricLoginClick(view: View) {
        val token = SecurityHelper.getString("auth_token")
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "No saved login token, please login manually.", Toast.LENGTH_SHORT).show()
            return
        }

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> showBiometricPrompt(token)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Toast.makeText(this, "No biometric features available on this device.", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Toast.makeText(this, "Biometric features are currently unavailable.", Toast.LENGTH_SHORT).show()
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Toast.makeText(this, "No biometric credentials enrolled.", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendLoginRequest(name: String, className: String, password: String) {
        Thread {
            try {
                val url = URL("https://10.0.2.2:9999/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                val jsonBody = JSONObject()
                jsonBody.put("name", name)
                jsonBody.put("class", className)
                jsonBody.put("password", password)

                val output = connection.outputStream
                output.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                output.flush()
                output.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val response = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

                runOnUiThread {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val responseJson = JSONObject(response)
                        val token = responseJson.getString("token")

                        SecurityHelper.saveString("auth_token", token)
                        SecurityHelper.putBoolean("is_logged_out", false)
                        SecurityHelper.saveString("username", name)
                        SecurityHelper.saveString("class", className)

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java).apply {
                            putExtra("username", name)
                            putExtra("class", className)
                        })
                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: $response", Toast.LENGTH_SHORT).show()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    fun sendMessageWithToken(token: String) {
        Thread {
            try {
                val url = URL("https://10.0.2.2:9999/validate-token")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = connection.responseCode
                runOnUiThread {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(this, "Biometric login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        SecurityHelper.saveString("auth_token", "")
                        SecurityHelper.putBoolean("is_logged_out", true)
                        Toast.makeText(this, "Session expired, please login manually", Toast.LENGTH_SHORT).show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
