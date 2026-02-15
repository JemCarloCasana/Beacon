package com.example.beacon.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.beacon.R
import com.example.beacon.databinding.ItemAllContactBinding

class AllContactsAdapter(
    private val onSetPrimary: (Contact) -> Unit,
    private val onEdit: (Contact) -> Unit,
    private val onDelete: (Contact) -> Unit
) : ListAdapter<Contact, AllContactsAdapter.ViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAllContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAllContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.apply {
                tvName.text = contact.name
                tvRelation.text = contact.relation

                val initials = contact.name.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                tvAvatar.text = initials

                btnMore.setOnClickListener { view -> showPopupMenu(view, contact) }
            }
        }

        private fun showPopupMenu(view: View, contact: Contact) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_all_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_set_primary -> {
                        onSetPrimary(contact)
                        true
                    }
                    R.id.action_edit -> {
                        onEdit(contact)
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
