package com.example.insecurestudentlogin

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    // Declares input fields for name, class, and password
    lateinit var name_input: EditText
    lateinit var class_input: EditText
    lateinit var password_input: EditText



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initializes the input fields
        name_input = findViewById(R.id.name_input)
        class_input = findViewById(R.id.class_input)
        password_input = findViewById(R.id.password_input)

        // Handles window insets (status bar, nav bar) so UI elements don’t get cut off
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets


        }
    }

    // Triggered when the user clicks the login button
    // Takes data from the input fields and sends it to the sendMessage function
    fun login_click(view: View?){
        var login_info : String
        login_info = "NAME:" + name_input.text.toString().trim() + "&CLASS:" + class_input.text.toString().trim() + "&PASSWORD:" + password_input.text.toString().trim()
        sendMessage(login_info,"10.0.2.2") // 10.0.2.2 = host machine from Android emulator
    }

    //Called in the login_click function
    // Sends the message via HTTP POST to the given IP address
    fun sendMessage(message: String, ipAddress: String) {
        Thread {
            try {
                // Opens HTTP connection to server
                val url = URL("http://$ipAddress:9999/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "text/plain")

                // Writes the message to the request body
                val output = connection.outputStream
                output.write(message.toByteArray())
                output.flush()
                output.close()

                // Reads the response from the server
                val responseCode = connection.responseCode
                val inputStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

                // Update UI with the result on the main thread
                runOnUiThread {

                    // Initializes connection to SharedPreferences
                    val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    val editor = sharedPref.edit()

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        //alerts the student to the response from the server and store the result in SharedPreferences
                        if (response == "pass") {
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            editor.putString("last_result", "Success")
                        } else {
                            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                            editor.putString("last_result", "Failure")
                        }

                        //Stores the last username/class/password message sent to the server
                        // with a timestamp in SharedPreferences
                        editor.putString("last_message", message)
                        editor.putLong("last_timestamp", System.currentTimeMillis())
                        editor.apply()
                    } else {
                        //Alerts the student to a failed connection error with the server
                        Toast.makeText(this, "Failed. Code: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

                //closes the connection
                connection.disconnect()
            } catch (e: IOException) {
                // Displays error if connection or response failed
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start() // Runs the network call in a background thread
    }

}