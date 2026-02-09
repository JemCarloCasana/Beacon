package com.example.beacon.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.beacon.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    companion object {
        private val FULL_NAME_REGEX = "^[A-Za-z ]{2,50}$".toRegex()
        private val PHONE_REGEX = "^[+]?[0-9]{10,20}$".toRegex()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupTextWatchers()
        setupClickListeners()
        
        // Load initial data
        viewModel.loadUserProfile()
    }

    private fun setupObservers() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                populateForm(it)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "" else "Save Changes"
        }

        viewModel.updateSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                navigateBack()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Only clear errors when user starts typing in a field
                if (s?.isNotEmpty() == true) {
                    clearErrors()
                }
                updateSaveButtonState()
                
                // Special case: validate confirm password when password changes
                if (s?.isNotEmpty() == true && binding.etPhoneNumber.text.toString().trim().isNotEmpty()) {
                    validatePhoneNumber()
                }
            }
        }

        binding.etFullName.addTextChangedListener(textWatcher)
        binding.etAddress.addTextChangedListener(textWatcher)
        binding.etPhoneNumber.addTextChangedListener(textWatcher)

        // Focus change listeners for validation
        binding.etFullName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateFullName()
            }
        }

        binding.etAddress.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAddress()
            }
        }

        binding.etPhoneNumber.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validatePhoneNumber()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            if (isFormValid()) {
                saveProfile()
            }
        }

        binding.btnCancel.setOnClickListener {
            navigateBack()
        }
    }

    private fun populateForm(profile: UserProfile) {
        binding.etFullName.setText(profile.fullName)
        binding.etEmail.setText(profile.email)
        binding.etAddress.setText(profile.address)
        binding.etPhoneNumber.setText(profile.phoneNumber ?: "")
    }

    private fun validateFullName(): Boolean {
        val fullName = binding.etFullName.text.toString().trim()
        return when {
            fullName.isEmpty() -> {
                binding.tilFullName.error = "Full name is required"
                false
            }
            !FULL_NAME_REGEX.matches(fullName) -> {
                binding.tilFullName.error = "Full name can only contain letters and spaces (2-50 characters)"
                false
            }
            else -> {
                binding.tilFullName.error = null
                true
            }
        }
    }

    private fun validateAddress(): Boolean {
        val address = binding.etAddress.text.toString().trim()
        return when {
            address.isEmpty() -> {
                binding.tilAddress.error = "Address is required for emergency services"
                false
            }
            address.length < 10 || address.length > 200 -> {
                binding.tilAddress.error = "Address must be 10-200 characters"
                false
            }
            !isValidAddress(address) -> {
                binding.tilAddress.error = "Enter a complete address (street, city, state/province)"
                false
            }
            else -> {
                binding.tilAddress.error = null
                true
            }
        }
    }

    private fun isValidAddress(address: String): Boolean {
        val addressRegex = "^[A-Za-z0-9\\s,\\-.#]{10,200}$".toRegex()
        return addressRegex.matches(address)
    }

    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        return when {
            phoneNumber.isNotEmpty() && !PHONE_REGEX.matches(phoneNumber) -> {
                binding.tilPhoneNumber.error = "Enter a valid phone number"
                false
            }
            else -> {
                binding.tilPhoneNumber.error = null
                true
            }
        }
    }

    private fun isFormValid(): Boolean {
        val isNameValid = validateFullName()
        val isAddressValid = validateAddress()
        val isPhoneValid = validatePhoneNumber()
        return isNameValid && isAddressValid && isPhoneValid
    }

    private fun clearErrors() {
        binding.tilFullName.error = null
        binding.tilAddress.error = null
        binding.tilPhoneNumber.error = null
    }

    private fun updateSaveButtonState() {
        val fullName = binding.etFullName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        binding.btnSave.isEnabled = fullName.isNotEmpty() && address.isNotEmpty()
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim().ifEmpty { null }

        viewModel.updateProfile(fullName, address, phoneNumber)
    }

    private fun navigateBack() {
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}