package com.example.beacon.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.beacon.R
import com.example.beacon.databinding.ItemPrimaryContactBinding

class PrimaryContactsAdapter(
    private val onCallClick: (Contact) -> Unit,
    private val onEdit: (Contact) -> Unit,
    private val onRemoveFromPrimary: (Contact) -> Unit,
    private val onDelete: (Contact) -> Unit
) : ListAdapter<Contact, PrimaryContactsAdapter.ViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPrimaryContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPrimaryContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.apply {
                tvName.text = contact.name
                tvRelation.text = "${contact.relation} • ${contact.phone}"

                val initials = contact.name.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                tvAvatar.text = initials

                btnCall.setOnClickListener { onCallClick(contact) }

                btnMore.setOnClickListener { view -> showPopupMenu(view, contact) }
            }
        }

        private fun showPopupMenu(view: View, contact: Contact) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_primary_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEdit(contact)
                        true
                    }
                    R.id.action_remove_primary -> {
                        onRemoveFromPrimary(contact)
                        true
                    }
                    R.id.action_delete -> {
                        onDelete(contact)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
