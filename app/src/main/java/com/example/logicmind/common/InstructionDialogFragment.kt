package com.example.logicmind.common

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.logicmind.R
import com.example.logicmind.additional.SymbolRaceInstructionHelper
import androidx.core.graphics.drawable.toDrawable

class InstructionDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = arguments?.getString(ARG_MESSAGE) ?: ""
        val title = arguments?.getString(ARG_TITLE) ?: getString(R.string.instructions)

        val view = layoutInflater.inflate(R.layout.dialog_instruction, null)

        val titleView = view.findViewById<TextView>(R.id.tvDialogTitle)
        titleView.text = title

        val instructionText = view.findViewById<TextView>(R.id.instructionText)
        val processedMessage = message.replace("\n", "<br/>")
        instructionText.text = SymbolRaceInstructionHelper.getSpanned(requireContext(), processedMessage)

        val btnOk = view.findViewById<Button>(R.id.btnOk)
        btnOk.setOnClickListener {
            dismiss()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
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