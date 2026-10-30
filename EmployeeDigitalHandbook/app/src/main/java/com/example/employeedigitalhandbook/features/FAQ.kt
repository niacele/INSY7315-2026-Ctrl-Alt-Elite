package com.example.employeedigitalhandbook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.employeedigitalhandbook.adapters.ChatAdapter
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.data.ChatMessage
import com.example.employeedigitalhandbook.data.Faq
import com.example.employeedigitalhandbook.databinding.FragmentFaqBinding
import com.google.android.material.chip.Chip
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

        // Setup Chat RecyclerView
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

        // Load all available FAQs dynamically from PostgreSQL
        loadFaqTopicsFromDatabase()
    }

    private fun loadFaqTopicsFromDatabase() {
        lifecycleScope.launch {
            try {
                // Call GET api/Faqs to retrieve all available FAQ entries
                val response = ApiClient.apiService.getAllFaqs()
                val safeBinding = binding ?: return@launch

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && !apiResponse.data.isNullOrEmpty()) {
                        populateDynamicChips(apiResponse.data)
                    } else {
                        showNoTopicsAvailableMessage()
                    }
                } else {
                    showNoTopicsAvailableMessage()
                }
            } catch (e: Exception) {
                showNoTopicsAvailableMessage()
            }
        }
    }

    private fun populateDynamicChips(faqList: List<Faq>) {
        val safeBinding = binding ?: return
        safeBinding.presetChipGroup.removeAllViews()

        for (faq in faqList) {
            val chip = Chip(requireContext()).apply {
                text = faq.question
                isCheckable = false
                isClickable = true

                // Uses standard system color or your primary color safely
                setChipBackgroundColorResource(android.R.color.white)
                chipStrokeWidth = 2f
                chipStrokeColor = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_blue_dark)
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark))

                setOnClickListener {
                    handleTopicSelected(faq.question, faq.topicKey)
                }
            }
            safeBinding.presetChipGroup.addView(chip)
        }
    }

    private fun handleTopicSelected(displayQuestion: String, topicKey: String) {
        // 1. Append user prompt bubble
        chatAdapter.addMessage(ChatMessage(message = displayQuestion, isUser = true))
        scrollToBottom()

        // 2. Fetch answer dynamically by topicKey
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.queryFaq(topicKey)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        chatAdapter.addMessage(
                            ChatMessage(message = apiResponse.data.answer, isUser = false)
                        )
                    } else {
                        val message = apiResponse.message ?: "Information for this topic is currently unavailable."
                        chatAdapter.addMessage(ChatMessage(message = message, isUser = false))
                    }
                } else {
                    chatAdapter.addMessage(
                        ChatMessage(message = "Unable to fetch topic response.", isUser = false)
                    )
                }
                scrollToBottom()
            } catch (e: Exception) {
                chatAdapter.addMessage(
                    ChatMessage(message = "Network error: ${e.localizedMessage}", isUser = false)
                )
                scrollToBottom()
            }
        }
    }

    private fun showNoTopicsAvailableMessage() {
        chatAdapter.addMessage(
            ChatMessage(message = "Welcome to Handbook FAQ! No FAQ topics are currently available.", isUser = false)
        )
    }

    private fun scrollToBottom() {
        binding?.chatRecyclerView?.post {
            if (chatAdapter.itemCount > 0) {
                binding?.chatRecyclerView?.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}