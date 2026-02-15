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
import com.example.beacon.api.models.BootstrapRequest
import com.example.beacon.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import retrofit2.HttpException

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$".toRegex()
        private val FULL_NAME_REGEX = "^[a-zA-Z\\s'-]{2,50}$".toRegex()
        private val PHONE_REGEX = "^((\\+63)|0)\\d{10}$".toRegex()
        private const val TAG = "SignupActivity"
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
        binding.etPhone.addTextChangedListener(createTextWatcher())
        binding.etConfirmPassword.addTextChangedListener(createTextWatcher())

        binding.etFullName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateFullName()
        }
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail()
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePassword()
        }
        binding.etPhone.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validatePhone()
        }
        binding.etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateConfirmPassword()
        }
    }

    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.isNotEmpty() == true) clearErrors()
                updateSignupButtonState()

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
            binding.tilFullName.error =
                "Full name can only contain letters, spaces, hyphens, and apostrophes (2-50 characters)"
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

    private fun validatePhone() {
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
        } else if (!isPhoneValid(phone)) {
            binding.tilPhone.error = "Enter a valid Philippine phone number (e.g., 09123456789 or +639123456789)"
        } else {
            binding.tilPhone.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            if (isFormValid()) {
                val fullName = binding.etFullName.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()
                val confirmPassword = binding.etConfirmPassword.text.toString().trim()
                registerUser(fullName, email, phone, password, confirmPassword)
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
        binding.tilPhone.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun isPasswordValid(password: String): Boolean = PASSWORD_REGEX.matches(password)

    private fun isFullNameValid(fullName: String): Boolean = FULL_NAME_REGEX.matches(fullName.trim())

    private fun isPhoneValid(phone: String): Boolean = PHONE_REGEX.matches(phone)

    private fun isFormEmpty(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        return fullName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || confirmPassword.isEmpty()
    }

    private fun isFormValid(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        var isValid = true

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            isValid = false
        } else if (!isFullNameValid(fullName)) {
            binding.tilFullName.error =
                "Full name can only contain letters, spaces, hyphens, and apostrophes (2-50 characters)"
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

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            isValid = false
        } else if (!isPhoneValid(phone)) {
            binding.tilPhone.error = "Enter a valid Philippine phone number (e.g., 09123456789 or +639123456789)"
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
        phone: String,
        password: String,
        confirmPassword: String
    ) {
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "Signup failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                val user = auth.currentUser
                if (user == null) {
                    showLoading(false)
                    Toast.makeText(this, "Signup failed: user session missing", Toast.LENGTH_LONG).show()
                    return@addOnCompleteListener
                }

                // Optional: set Firebase display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener {
                        // Even if this fails, we still proceed to bootstrap Postgres
                        bootstrapPostgresProfileAndNavigate(fullName, phone)
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

    /**
     * ✅ This is the correct place to call POST /me/bootstrap
     * because Signup has the real full_name.
     */
    private fun bootstrapPostgresProfileAndNavigate(fullName: String, phone: String) {
        val user = auth.currentUser ?: run {
            showLoading(false)
            Toast.makeText(this, "Signup failed: user session missing", Toast.LENGTH_LONG).show()
            return
        }

        user.getIdToken(true)
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
                        val created = ApiClient.api.bootstrap(
                            authHeader = authHeader,
                            body = BootstrapRequest(
                                full_name = fullName,
                                phone_number = phone
                            )
                        )

                        Log.d(TAG, "Bootstrapped Postgres profile id=${created.id}")
                        showLoading(false)
                        Toast.makeText(this@SignupActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                        navigateToMain()

                    } catch (e: HttpException) {
                        Log.e(TAG, "Bootstrap failed code=${e.code()}", e)
                        showLoading(false)
                        Toast.makeText(this@SignupActivity, "Profile setup failed (server).", Toast.LENGTH_LONG).show()

                    } catch (e: Exception) {
                        Log.e(TAG, "Bootstrap failed", e)
                        showLoading(false)
                        Toast.makeText(this@SignupActivity, "Profile setup failed (network).", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get token", e)
                showLoading(false)
                Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
