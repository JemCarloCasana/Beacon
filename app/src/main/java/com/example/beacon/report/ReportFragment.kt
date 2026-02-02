package com.example.beacon.report

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.beacon.databinding.FragmentReportBinding

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDropdown()
        setupSubmitButton()
    }

    private fun setupDropdown() {
        // Populate the dropdown with incident types
        val incidentTypes = listOf("Theft", "Harassment", "Suspicious Activity", "Accident", "Fire", "Medical Emergency", "Other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, incidentTypes)
        binding.dropdownIncidentType.setAdapter(adapter)
    }

    private fun setupSubmitButton() {
        binding.btnSubmitReport.setOnClickListener {
            val type = binding.dropdownIncidentType.text.toString()
            val description = binding.etDescription.text.toString()

            // 1. Basic Validation
            if (description.isBlank()) {
                binding.etDescription.error = "Please describe the incident"
                return@setOnClickListener
            }

            // 2. Simulate "Sending" (UI Interaction Test)
            showLoading(true)

            // Fake delay of 2 seconds to simulate network call
            Handler(Looper.getMainLooper()).postDelayed({
                showLoading(false)

                // Success Feedback
                Toast.makeText(context, "Report sent successfully! (UI Demo)", Toast.LENGTH_SHORT).show()

                // Clear the form
                binding.etDescription.text?.clear()
                binding.dropdownIncidentType.setText("Suspicious Activity", false)

            }, 2000)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmitReport.isEnabled = false
            binding.btnSubmitReport.text = "Sending..."
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnSubmitReport.isEnabled = true
            binding.btnSubmitReport.text = "Submit Report"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}