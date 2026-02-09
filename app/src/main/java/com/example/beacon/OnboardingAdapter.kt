package com.example.beacon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.beacon.databinding.FragmentOnboardingAlertsBinding
import com.example.beacon.databinding.FragmentOnboardingReportBinding
import com.example.beacon.databinding.FragmentOnboardingSosBinding

class OnboardingAdapter(private val activity: OnboardingActivity) : RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    companion object {
        private const val TYPE_SOS = 0
        private const val TYPE_REPORT = 1
        private const val TYPE_ALERTS = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_SOS
            1 -> TYPE_REPORT
            2 -> TYPE_ALERTS
            else -> TYPE_SOS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        return when (viewType) {
            TYPE_SOS -> {
                val binding = FragmentOnboardingSosBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SOSViewHolder(binding)
            }
            TYPE_REPORT -> {
                val binding = FragmentOnboardingReportBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReportViewHolder(binding)
            }
            TYPE_ALERTS -> {
                val binding = FragmentOnboardingAlertsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                AlertsViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        // Data binding is done in XML, so no additional binding needed here
    }

    override fun getItemCount(): Int = 3

    abstract class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SOSViewHolder(binding: FragmentOnboardingSosBinding) : OnboardingViewHolder(binding.root)

    class ReportViewHolder(binding: FragmentOnboardingReportBinding) : OnboardingViewHolder(binding.root)

    class AlertsViewHolder(binding: FragmentOnboardingAlertsBinding) : OnboardingViewHolder(binding.root)
}