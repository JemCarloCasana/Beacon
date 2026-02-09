package com.example.beacon.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.beacon.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupTextWatchers()
        setupClickListeners()
        updateResetButtonState()
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(createTextWatcher())
        
        // Add focus listener for validation on field exit
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateEmail()
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
                updateResetButtonState()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnResetPassword.setOnClickListener {
            if (isFormValid()) {
                val email = binding.etEmail.text.toString().trim()
                sendPasswordResetEmail(email)
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
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

    private fun isFormEmpty(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        return email.isEmpty()
    }

    private fun isFormValid(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            isValid = false
        }

        return isValid
    }

    private fun updateResetButtonState() {
        binding.btnResetPassword.isEnabled = !isFormEmpty()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.isEnabled = !isLoading
        binding.btnResetPassword.text = if (isLoading) "" else "Send Reset Email"
    }

    private fun sendPasswordResetEmail(email: String) {
        showLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent to $email",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Navigate back to login after successful email send
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to send reset email: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}