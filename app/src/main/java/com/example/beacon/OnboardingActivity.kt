package com.example.beacon

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.beacon.auth.LoginActivity
import com.example.beacon.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupClickListeners()
        updateUI(binding.viewPager.currentItem)
    }

    private fun setupViewPager() {
        adapter = OnboardingAdapter(this)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateUI(position)
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnSkip.setOnClickListener {
            navigateToNext()
        }

        binding.btnContinue.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < adapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                // Last screen - mark onboarding as complete and navigate
                markOnboardingComplete()
                navigateToNext()
            }
        }
    }

    private fun updateUI(position: Int) {
        // Update dots indicator
        updateDots(position)

        // Update continue button text
        binding.btnContinue.text = if (position == adapter.itemCount - 1) {
            "Get Started"
        } else {
            "Continue"
        }

        // Hide skip button on last screen
        binding.btnSkip.visibility = if (position == adapter.itemCount - 1) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun updateDots(position: Int) {
        val dots = listOf(binding.dotsIndicator.findViewById<View>(R.id.dot1),
                         binding.dotsIndicator.findViewById<View>(R.id.dot2),
                         binding.dotsIndicator.findViewById<View>(R.id.dot3))

        for (i in dots.indices) {
            val dot = dots[i]
            dot.alpha = if (i == position) 1.0f else 0.3f
        }
    }

    private fun markOnboardingComplete() {
        val sharedPreferences = getSharedPreferences("BeaconPrefs", MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("FIRST_LAUNCH", false)
            .apply()
    }

    private fun navigateToNext() {
        // Check if user is already authenticated
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val intent = if (auth.currentUser != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}