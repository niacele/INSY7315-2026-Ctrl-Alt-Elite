package com.example.employeedigitalhandbook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.employeedigitalhandbook.adapters.ChatAdapter
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.data.ChatMessage
import com.example.employeedigitalhandbook.databinding.FragmentFaqBinding
import kotlinx.coroutines.launch

class FAQ : Fragment() {

    private var _binding: FragmentFaqBinding? = null
    private val binding get() = _binding

    private val chatAdapter = ChatAdapter(mutableListOf())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFaqBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val safeBinding = binding ?: return

        // Configure Chat RecyclerView
        safeBinding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // Navigation
        safeBinding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Chip Clicks mapped to PostgreSQL TopicKey values
        safeBinding.chipAdverseDrug.setOnClickListener {
            handleTopicSelected("What is the protocol for Adverse Drug Reactions?", "adverse_drug")
        }

        safeBinding.chipCpdPoints.setOnClickListener {
            handleTopicSelected("How do CPD points work?", "cpd_points")
        }

        safeBinding.chipTheatreSOP.setOnClickListener {
            handleTopicSelected("Theatre Slate Breaks", "theatre_sop")
        }

        safeBinding.chipContactAdmin.setOnClickListener {
            handleTopicSelected("Doctor Extensions", "doctor_extensions")
        }
    }

    private fun handleTopicSelected(displayQuestion: String, topicKey: String) {
        // 1. Append user prompt bubble
        chatAdapter.addMessage(ChatMessage(message = displayQuestion, isUser = true))
        binding?.chatRecyclerView?.smoothScrollToPosition(chatAdapter.itemCount - 1)

        // 2. Fetch answer from ASP.NET Core API
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.queryFaq(topicKey)
                val safeBinding = binding ?: return@launch

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        chatAdapter.addMessage(
                            ChatMessage(message = apiResponse.data.answer, isUser = false)
                        )
                    } else {
                        val message = apiResponse.message ?: "No FAQ entry found for this topic."
                        chatAdapter.addMessage(ChatMessage(message = message, isUser = false))
                    }
                } else {
                    chatAdapter.addMessage(
                        ChatMessage(message = "Unable to fetch answer (HTTP ${response.code()}).", isUser = false)
                    )
                }
                safeBinding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
            } catch (e: Exception) {
                chatAdapter.addMessage(
                    ChatMessage(message = "Network error: ${e.localizedMessage}", isUser = false)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}