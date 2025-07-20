package com.example.insecurestudentlogin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val username = intent.getStringExtra("username") ?: "Student"
        findViewById<TextView>(R.id.welcome_text).text = "Welcome, $username!"

        findViewById<Button>(R.id.logout_button).setOnClickListener {
            SecurityHelper.clearTokenAndLogout()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.view_grades_button).setOnClickListener {
            Toast.makeText(this, "View Grades clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.assignments_button).setOnClickListener {
            Toast.makeText(this, "Assignments clicked", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.settings_button).setOnClickListener {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        val token = SecurityHelper.getString("auth_token")

        // forces th login to prompt when clsosing/re-opening the app - this would normally check to
        // see if the session has expired but for brevity, we are just going to force the redirect
        if (token.isNullOrEmpty() || !isFirstLoad) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        isFirstLoad = false
    }

}
