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
import com.example.beacon.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()
        private val FULL_NAME_REGEX = "^[a-zA-Z\\s'-]{2,50}$".toRegex()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupTextWatchers()
        setupClickListeners()
        updateSignupButtonState()
    }

    private fun setupTextWatchers() {
        binding.etFullName.addTextChangedListener(createTextWatcher())
        binding.etEmail.addTextChangedListener(createTextWatcher())
        binding.etPassword.addTextChangedListener(createTextWatcher())
        binding.etConfirmPassword.addTextChangedListener(createTextWatcher())
        
        // Add focus listeners for validation on field exit
        binding.etFullName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateFullName()
            }
        }
        
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
        
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateConfirmPassword()
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
                updateSignupButtonState()
                
                // Special case: validate confirm password when password changes
                if (s?.isNotEmpty() == true && binding.etConfirmPassword.text.toString().trim().isNotEmpty()) {
                    validateConfirmPassword()
                }
            }
        }
    }

    private fun validateFullName() {
        val fullName = binding.etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
        } else if (!isFullNameValid(fullName)) {
            binding.tilFullName.error = "Full name can only contain letters, spaces, hyphens, and apostrophes (2-50 characters)"
        } else {
            binding.tilFullName.error = null
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
        } else if (!isPasswordValid(password)) {
            binding.tilPassword.error = "Password must be at least 8 characters with uppercase, lowercase, and number"
        } else {
            binding.tilPassword.error = null
        }
        
        // Also validate confirm password if it has content
        if (binding.etConfirmPassword.text.toString().trim().isNotEmpty()) {
            validateConfirmPassword()
        }
    }

    private fun validateConfirmPassword() {
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm your password"
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
        } else {
            binding.tilConfirmPassword.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            if (isFormValid()) {
                val fullName = binding.etFullName.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                val confirmPassword = binding.etConfirmPassword.text.toString().trim()
                registerUser(fullName, email, password, confirmPassword)
            }
        }

        binding.tvLoginAction.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun clearErrors() {
        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun isPasswordValid(password: String): Boolean {
        return PASSWORD_REGEX.matches(password)
    }

    private fun isFullNameValid(fullName: String): Boolean {
        return FULL_NAME_REGEX.matches(fullName.trim())
    }

    private fun isFormEmpty(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        return fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
    }

    private fun isFormValid(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        
        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        } else if (!isFullNameValid(fullName)) {
            binding.tilFullName.error = "Full name can only contain letters, spaces, hyphens, and apostrophes (2-50 characters)"
            isValid = false
        }

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
        } else if (!isPasswordValid(password)) {
            binding.tilPassword.error = "Password must be at least 8 characters with uppercase, lowercase, and number"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm your password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    private fun updateSignupButtonState() {
        binding.btnSignup.isEnabled = !isFormEmpty()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignup.isEnabled = !isLoading
        binding.btnSignup.text = if (isLoading) "" else "Sign Up"
    }

    private fun registerUser(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        // Create Firestore document for user
                        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val userDoc = hashMapOf(
                            "uid" to user.uid,
                            "email" to user.email,
                            "displayName" to fullName,
                            "address" to "",
                            "phoneNumber" to "",
                            "profileImageUrl" to "",
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        
                        firestore.collection("users").document(user.uid)
                            .set(userDoc)
                            .addOnSuccessListener {
                                android.util.Log.d("SignupActivity", "Firestore document created for user: ${user.uid}")
                                showLoading(false)
                                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("SignupActivity", "Failed to create Firestore document", e)
                                showLoading(false)
                                val errorMessage = when {
                                    e.message?.contains("permission-denied") == true -> 
                                        "Permission denied creating profile."
                                    e.message?.contains("unavailable") == true -> 
                                        "Network error creating profile."
                                    else -> "Error creating profile: ${e.message}"
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                            }
                    } else {
                        showLoading(false)
                        val errorMessage = when {
                            profileTask.exception?.message?.contains("email-already-in-use") == true -> 
                                "Email is already registered."
                            profileTask.exception?.message?.contains("weak-password") == true -> 
                                "Password is too weak."
                            profileTask.exception?.message?.contains("invalid-email") == true -> 
                                "Invalid email address."
                            else -> "Profile update failed: ${profileTask.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
                } else {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Signup failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Signup failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
