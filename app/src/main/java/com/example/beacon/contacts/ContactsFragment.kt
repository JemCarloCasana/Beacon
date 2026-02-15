package com.example.beacon.contacts

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.beacon.R
import com.example.beacon.databinding.FragmentContactsBinding
import com.google.android.material.button.MaterialButton
import java.util.UUID

data class Contact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val relation: String,
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class ContactsFragment : Fragment(), 
    AddContactDialogFragment.OnContactAddedListener,
    EditContactDialogFragment.OnContactEditedListener {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!

    private val primaryContacts = mutableListOf<Contact>()
    private val allContacts = mutableListOf<Contact>()

    private lateinit var primaryAdapter: PrimaryContactsAdapter
    private lateinit var allAdapter: AllContactsAdapter

    companion object {
        private const val MAX_PRIMARY_CONTACTS = 3
    }

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
        setupRecyclerViews()
        setupFabButton()
        loadContacts()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerViews() {
        primaryAdapter = PrimaryContactsAdapter(
            onCallClick = { contact -> makePhoneCall(contact) },
            onEdit = { contact -> editContact(contact) },
            onRemoveFromPrimary = { contact -> removeFromPrimary(contact) },
            onDelete = { contact -> deleteContact(contact) }
        )

        allAdapter = AllContactsAdapter(
            onSetPrimary = { contact -> showPrimarySwapDialog(contact) },
            onEdit = { contact -> editContact(contact) },
            onDelete = { contact -> deleteContact(contact) }
        )

        binding.rvPrimaryContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = primaryAdapter
        }

        binding.rvAllContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = allAdapter
        }
    }

    private fun setupFabButton() {
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun showAddContactDialog() {
        val dialog = AddContactDialogFragment.newInstance()
        dialog.setOnContactAddedListener(this)
        dialog.show(childFragmentManager, AddContactDialogFragment.TAG)
    }

    private fun updateContactListDisplay() {
        primaryAdapter.submitList(primaryContacts.toList())
        allAdapter.submitList(allContacts.toList())
        
        binding.tvPrimaryCount.text = "${primaryContacts.size}/$MAX_PRIMARY_CONTACTS"
    }

    override fun onContactAdded(contact: Contact) {
        if (primaryContacts.size < MAX_PRIMARY_CONTACTS) {
            val newPrimary = contact.copy(isPrimary = true)
            primaryContacts.add(newPrimary)
        } else {
            allContacts.add(contact)
        }
        updateContactListDisplay()
    }

    private fun showPrimarySwapDialog(contact: Contact) {
        if (primaryContacts.isEmpty()) {
            Toast.makeText(requireContext(), "No primary contacts to replace", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_select_primary, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.rgPrimaryContacts)
        
        primaryContacts.forEachIndexed { index, primary ->
            val radioButton = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = "${primary.name} (${primary.relation})"
                tag = index
                textSize = 14f
                setPadding(8, 16, 8, 16)
            }
            radioGroup.addView(radioButton)
        }

        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRadioButton = dialogView.findViewById<RadioButton>(selectedId)
                val selectedIndex = selectedRadioButton.tag as Int
                swapPrimaryContact(selectedIndex, contact)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select a primary contact to replace", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun swapPrimaryContact(primaryIndex: Int, newPrimary: Contact) {
        val oldPrimary = primaryContacts[primaryIndex]
        
        primaryContacts[primaryIndex] = newPrimary.copy(isPrimary = true)
        
        allContacts.add(oldPrimary.copy(isPrimary = false))
        
        updateContactListDisplay()
    }

    private fun removeFromPrimary(contact: Contact) {
        primaryContacts.remove(contact)
        allContacts.add(contact.copy(isPrimary = false))
        updateContactListDisplay()
    }

    private fun deleteContact(contact: Contact) {
        if (contact.isPrimary) {
            primaryContacts.remove(contact)
        } else {
            allContacts.remove(contact)
        }
        updateContactListDisplay()
    }

    private fun editContact(contact: Contact) {
        val dialog = EditContactDialogFragment.newInstance(contact)
        dialog.setOnContactEditedListener(this)
        dialog.show(childFragmentManager, EditContactDialogFragment.TAG)
    }

    override fun onContactEdited(updatedContact: Contact) {
        if (updatedContact.isPrimary) {
            val index = primaryContacts.indexOfFirst { it.id == updatedContact.id }
            if (index != -1) {
                primaryContacts[index] = updatedContact
            }
        } else {
            val index = allContacts.indexOfFirst { it.id == updatedContact.id }
            if (index != -1) {
                allContacts[index] = updatedContact
            }
        }
        updateContactListDisplay()
    }

    private fun makePhoneCall(contact: Contact) {
        Toast.makeText(requireContext(), "Calling ${contact.name}", Toast.LENGTH_SHORT).show()
    }

    private fun loadContacts() {
        val samplePrimary = listOf(
            Contact(name = "Maria Rivera", phone = "(555) 012-3456", relation = "Mother", isPrimary = true),
            Contact(name = "David Chen", phone = "(555) 987-6543", relation = "Partner", isPrimary = true),
            Contact(name = "Sarah Miller", phone = "(555) 456-7890", relation = "Best Friend", isPrimary = true)
        )

        val sampleAll = listOf(
            Contact(name = "Dr. James Wilson", phone = "(555) 111-2222", relation = "Primary Doctor"),
            Contact(name = "Neighbor Mike", phone = "(555) 333-4444", relation = "Neighbor")
        )

        primaryContacts.clear()
        allContacts.clear()
        
        primaryContacts.addAll(samplePrimary)
        allContacts.addAll(sampleAll)
        
        updateContactListDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
