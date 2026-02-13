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
import com.example.beacon.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
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

        // If user is already logged in, still sync profile from Postgres, then continue.
        if (auth.currentUser != null) {
            syncPostgresProfileThenNavigate()
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
                showLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    // ✅ After login, sync Postgres profile then continue
                    syncPostgresProfileThenNavigate()
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

    /**
     * Correct logic:
     * 1) Get Firebase ID token
     * 2) Call GET /me
     * 3) If 404 -> POST /me/bootstrap (first-time only)
     * 4) Navigate to main
     *
     * NOTE: Bootstrap needs full_name. On login you don't have it.
     * For now we use a placeholder; later move bootstrap to Signup flow with the real full name.
     */
    private fun syncPostgresProfileThenNavigate() {
        val firebaseUser = auth.currentUser ?: run {
            navigateToMain()
            return
        }

        showLoading(true)

        firebaseUser.getIdToken(true)
            .addOnSuccessListener { result ->
                val token = result.token
                if (token.isNullOrBlank()) {
                    showLoading(false)
                    navigateToMain()
                    return@addOnSuccessListener
                }

                val authHeader = "Bearer $token"

                lifecycleScope.launch {
                    try {
                        // Try loading profile
                        val me = ApiClient.api.me(authHeader)
                        Log.d("API_ME", "Loaded Postgres profile id=${me.id}")
                        showLoading(false)
                        navigateToMain()

                    } catch (e: HttpException) {
                        if (e.code() == 404) {
                            // First-time user profile missing -> bootstrap
                            try {
                                val created = ApiClient.api.bootstrap(
                                    authHeader = authHeader,
                                    body = BootstrapRequest(
                                        full_name = "Temp User",
                                        phone_number = null
                                    )
                                )
                                Log.d("API_BOOTSTRAP", "Created Postgres profile id=${created.id}")
                            } catch (ex: Exception) {
                                Log.e("API_BOOTSTRAP", "Bootstrap failed", ex)
                            } finally {
                                showLoading(false)
                                navigateToMain()
                            }
                        } else {
                            Log.e("API_ME", "GET /me failed code=${e.code()}", e)
                            showLoading(false)
                            navigateToMain()
                        }
                    } catch (e: Exception) {
                        Log.e("API_ME", "Network/Unknown error", e)
                        showLoading(false)
                        navigateToMain()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ID_TOKEN", "Failed to get token", e)
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
