package com.example.beacon.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.beacon.MainActivity
import com.example.beacon.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import kotlin.jvm.java

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Automatically navigate if user is already logged in
        if (auth.currentUser != null) {
            navigateToMain()
        }

        setupTextWatchers()
        setupClickListeners()
        updateLoginButtonState()
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(createTextWatcher())
        binding.etPassword.addTextChangedListener(createTextWatcher())
        
        // Add focus listeners for validation on field exit
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateEmail()
            }
        }
        
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePassword()
            }
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Only clear errors when user starts typing in a field
                if (s?.isNotEmpty() == true) {
                    clearErrors()
                }
                updateLoginButtonState()
            }
        }
    }

    private fun validateEmail() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
        } else {
            binding.tilEmail.error = null
        }
    }

    private fun validatePassword() {
        val password = binding.etPassword.text.toString().trim()
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
        } else if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
        } else {
            binding.tilPassword.error = null
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
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
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

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
