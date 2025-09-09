package com.example.logicmind.common

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.logicmind.R

/**
 * Fragment wyświetlający instrukcje w formie dialogu.
 * Umożliwia ustawienie tytułu i treści wiadomości oraz używa niestandardowego wyglądu.
 */
class InstructionDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = arguments?.getString(ARG_MESSAGE) ?: ""
        val title = arguments?.getString(ARG_TITLE) ?: getString(R.string.instructions)

        val view = layoutInflater.inflate(R.layout.dialog_instruction, null)
        view.findViewById<TextView>(R.id.instructionText).text = message

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_round)

        return dialog
    }

    companion object {
        private const val ARG_MESSAGE = "ARG_MESSAGE"
        private const val ARG_TITLE = "ARG_TITLE"

        fun newInstance(title: String, message: String): InstructionDialogFragment {
            val fragment = InstructionDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
            }
            return fragment
        }
    }
}