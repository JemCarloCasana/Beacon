package com.example.beacon.contacts

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.beacon.R
import com.google.android.material.button.MaterialButton

class AddContactDialogFragment : DialogFragment() {

    private var listener: OnContactAddedListener? = null

    interface OnContactAddedListener {
        fun onContactAdded(contact: Contact)
    }

    fun setOnContactAddedListener(listener: OnContactAddedListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Beacon_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_contact, container, false)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.6).toInt()
            dialog.window?.setLayout(width, height)
            dialog.window?.setGravity(Gravity.CENTER)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnAdd)?.setOnClickListener {
            val name = view.findViewById<android.widget.EditText>(R.id.etContactName)?.text.toString()
            val phone = view.findViewById<android.widget.EditText>(R.id.etContactPhone)?.text.toString()
            val relation = view.findViewById<android.widget.EditText>(R.id.etContactRelation)?.text.toString()

            if (name.isNotBlank() && phone.isNotBlank()) {
                val newContact = Contact(name = name, phone = phone, relation = relation)
                listener?.onContactAdded(newContact)
            }
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "AddContactDialogFragment"

        fun newInstance(): AddContactDialogFragment {
            return AddContactDialogFragment()
        }
    }
}
