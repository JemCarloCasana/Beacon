package com.example.beacon.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.beacon.MainActivity
import com.example.beacon.api.ApiClient
import com.example.beacon.api.models.DeviceRegisterRequest
import com.example.beacon.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            loadMeThenNavigate()
            return
        }

        setupTextWatchers()
        setupClickListeners()
        updateLoginButtonState()
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(createTextWatcher())
        binding.etPassword.addTextChangedListener(createTextWatcher())

        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail()
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePassword()
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.isNotEmpty() == true) clearErrors()
                updateLoginButtonState()
            }
        }
    }

    private fun validateEmail() {
        val email = binding.etEmail.text.toString().trim()
        when {
            email.isEmpty() -> binding.tilEmail.error = "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.tilEmail.error = "Enter a valid email"
            else -> binding.tilEmail.error = null
        }
    }

    private fun validatePassword() {
        val password = binding.etPassword.text.toString().trim()
        when {
            password.isEmpty() -> binding.tilPassword.error = "Password is required"
            password.length < 6 -> binding.tilPassword.error = "Password must be at least 6 characters"
            else -> binding.tilPassword.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            if (isFormValid()) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                loginUser(email, password)
            }
        }

        binding.tvSignupAction.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    private fun isFormEmpty(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        return email.isEmpty() || password.isEmpty()
    }

    private fun isFormValid(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun updateLoginButtonState() {
        binding.btnLogin.isEnabled = !isFormEmpty()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLogin.text = if (isLoading) "" else "Sign In"
    }

    private fun loginUser(email: String, password: String) {
        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    // Optional: keep for Postman debugging
                    auth.currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            Log.d("ID_TOKEN_DEBUG", result.token ?: "NULL TOKEN")
                        }
                        ?.addOnFailureListener { e ->
                            Log.e("ID_TOKEN_DEBUG", "Failed to get token", e)
                        }

                    loadMeThenNavigate()
                } else {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Login failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadMeThenNavigate() {
        val firebaseUser = auth.currentUser ?: run {
            showLoading(false)
            return
        }

        showLoading(true)

        firebaseUser.getIdToken(true)
            .addOnSuccessListener { result ->
                val token = result.token
                if (token.isNullOrBlank()) {
                    showLoading(false)
                    Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val authHeader = "Bearer $token"

                lifecycleScope.launch {
                    try {
                        val me = ApiClient.api.me(authHeader)
                        Log.d("API_ME", "Loaded Postgres profile id=${me.id}")

                        // ✅ NEW: register device with REAL FCM token, then continue
                        registerDeviceThenNavigate(authHeader)

                    } catch (e: HttpException) {
                        showLoading(false)

                        if (e.code() == 404) {
                            Toast.makeText(
                                this@LoginActivity,
                                "Profile not found. Please sign up again or complete your profile.",
                                Toast.LENGTH_LONG
                            ).show()

                            auth.signOut()
                            startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
                            finish()
                        } else {
                            Log.e("API_ME", "GET /me failed code=${e.code()}", e)
                            Toast.makeText(
                                this@LoginActivity,
                                "Server error. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        showLoading(false)
                        Log.e("API_ME", "Network/Unknown error", e)
                        Toast.makeText(
                            this@LoginActivity,
                            "Network error. Check connection and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e("ID_TOKEN", "Failed to get token", e)
                Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_LONG).show()
            }
    }

    private fun registerDeviceThenNavigate(authHeader: String) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcmToken ->
                Log.d("FCM_TOKEN", "token_len=${fcmToken.length}")

                lifecycleScope.launch {
                    try {
                        val r = ApiClient.api.registerDevice(
                            authHeader = authHeader,
                            body = DeviceRegisterRequest(fcm_token = fcmToken)
                        )
                        Log.d("DEVICE_REG", "Registered device id=${r.id}")
                    } catch (e: Exception) {
                        Log.e("DEVICE_REG", "Failed to register device", e)
                        // Don't block login on device registration failure
                    } finally {
                        showLoading(false)
                        navigateToMain()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM_TOKEN", "Failed to get FCM token", e)
                showLoading(false)
                navigateToMain()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
