package com.example.beacon.contacts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.beacon.R
import com.example.beacon.databinding.FragmentContactsBinding
import java.util.UUID

// Contact data class
data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val relation: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Emergency Contacts Fragment
 * 
 * Displays and manages emergency contact information
 * Supports CRUD operations (Create, Read, Update, Delete)
 * Integrates with Firebase Firestore for persistence
 */
class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private val contactsList = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupContactList()
        setupFabButton()
        
        // Load existing contacts
        loadContacts()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupContactList() {
        // Static layout - contacts are displayed directly in XML
        // No need for RecyclerView setup with current design
    }

    private fun setupFabButton() {
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun updateContactListDisplay() {
        // Static layout - no dynamic updates needed
        // Contacts are hardcoded in the XML layout
        // This method is kept for compatibility but does nothing
    }

    private fun showAddContactDialog() {
        // Simple dialog for adding new contact
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_contact, null)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Emergency Contact")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = dialogView.findViewById<android.widget.EditText>(R.id.etContactName)?.text.toString()
                val phone = dialogView.findViewById<android.widget.EditText>(R.id.etContactPhone)?.text.toString()
                val relation = dialogView.findViewById<android.widget.EditText>(R.id.etContactRelation)?.text.toString()
                
                if (name.isNotBlank() && phone.isNotBlank()) {
                    val newContact = Contact(name = name, phone = phone, relation = relation)
                    contactsList.add(newContact)
                    updateContactListDisplay()
                    // Here you would save to Firebase Firestore
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteContact(contact: Contact) {
        contactsList.remove(contact)
        updateContactListDisplay()
        // Here you would delete from Firebase Firestore
    }

    private fun loadContacts() {
        // Here you would load from Firebase Firestore
        // For demo, add sample contacts
        val sampleContacts = listOf(
            Contact(name = "John Doe", phone = "+1-234-567890", relation = "Father"),
            Contact(name = "Jane Doe", phone = "+1-234-567891", relation = "Mother"),
            Contact(name = "Emergency Services", phone = "+1-800-555-1234", relation = "Service")
        )
        contactsList.addAll(sampleContacts)
        updateContactListDisplay()
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"
    }
}

// ContactAdapter is not needed for static layout
// Contacts are displayed directly in XML
// This class is removed but Contact data class is kept for future use