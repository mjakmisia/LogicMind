package com.example.logicmind.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.logicmind.databinding.DialogGameOverBinding

class GameOverDialogFragment : DialogFragment() {

    private var _binding: DialogGameOverBinding? = null
    private val binding get() = _binding!!

    // Callbacki do obsługi przycisków
    var onRestartListener: (() -> Unit)? = null
    var onExitListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGameOverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Blokada zamykania dialogu przez kliknięcie obok lub przycisk wstecz
        isCancelable = false

        // Pobieranie danych z argumentów
        val score = arguments?.getInt(ARG_SCORE) ?: 0
        val time = arguments?.getString(ARG_TIME) ?: "0s"
        val bestScore = arguments?.getInt(ARG_BEST_SCORE) ?: 0

        // Ustawianie tekstów
        binding.tvCurrentScore.text = score.toString()
        binding.tvTimeTaken.text = time
        binding.tvBestScore.text = bestScore.toString()

        // Obsługa przycisków
        binding.btnPlayAgain.setOnClickListener {
            dismiss()
            onRestartListener?.invoke()
        }

        binding.btnExitGame.setOnClickListener {
            dismiss()
            onExitListener?.invoke()
        }
    }

    override fun onStart() {
        super.onStart()
        // Ustawienie przezroczystego tła okna, żeby zaokrąglone rogi CardView były widoczne
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        // Ustawienie szerokości dialogu (opcjonalne, ale wygląda lepiej)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SCORE = "score"
        private const val ARG_TIME = "time"
        private const val ARG_BEST_SCORE = "best_score"

        fun newInstance(
            score: Int,
            timeFormatted: String,
            bestScore: Int
        ): GameOverDialogFragment {
            val fragment = GameOverDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SCORE, score)
            args.putString(ARG_TIME, timeFormatted)
            args.putInt(ARG_BEST_SCORE, bestScore)
            fragment.arguments = args
            return fragment
        }
    }
}