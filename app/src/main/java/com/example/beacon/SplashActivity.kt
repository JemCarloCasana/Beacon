package com.example.beacon

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.beacon.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val SPLASH_DELAY: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Delay splash screen visibility and then check navigation
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }

    private fun navigateToNextScreen() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("FIRST_LAUNCH", true)
        val currentUser = auth.currentUser

        when {
            isFirstLaunch -> {
                // First time launch - go to onboarding
                val intent = Intent(this, OnboardingActivity::class.java)
                startActivity(intent)
                finish()
            }
            currentUser != null -> {
                // User is already authenticated - go to main app
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else -> {
                // Not first launch but user not authenticated - go to login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}