package com.example.employeedigitalhandbook.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.data.ChangePasswordRequest
import com.example.employeedigitalhandbook.databinding.FragmentResetPasswordBinding
import com.example.employeedigitalhandbook.sessions.SessionManager
import kotlinx.coroutines.launch

class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backArrowImageView.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.savePasswordButton.setOnClickListener {
            attemptPasswordChange()
        }
    }

    private fun attemptPasswordChange() {
        val currentPassword = binding.currentPasswordEditText.text.toString().trim()
        val newPassword = binding.newPasswordEditText.text.toString().trim()
        val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

        // 1. Clear previous errors
        binding.currentPasswordInputLayout.error = null
        binding.newPasswordInputLayout.error = null
        binding.confirmPasswordInputLayout.error = null

        // 2. Validate inputs
        if (currentPassword.isEmpty()) {
            binding.currentPasswordInputLayout.error = "Current password is required"
            return
        }

        if (newPassword.isEmpty() || newPassword.length < 6) {
            binding.newPasswordInputLayout.error = "New password must be at least 6 characters long"
            return
        }

        if (newPassword == currentPassword) {
            binding.newPasswordInputLayout.error = "New password cannot be identical to current password"
            return
        }

        if (newPassword != confirmPassword) {
            binding.confirmPasswordInputLayout.error = "Passwords do not match"
            return
        }

        // 3. Get Active User ID via SessionManager
        val activeUserId = getLoggedUserId()
        if (activeUserId == -1) {
            Toast.makeText(requireContext(), "User session expired. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Send API request
        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val request = ChangePasswordRequest(
                    userId = activeUserId,
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )

                val response = ApiClient.apiService.changePassword(request)
                setLoadingState(false)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!

                    if (apiResponse.success) {
                        Toast.makeText(
                            requireContext(),
                            apiResponse.message ?: "Password updated successfully!",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigateUp()
                    } else {
                        val errorMsg = apiResponse.message ?: "Failed to update password."
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error: ${response.code()}"
                    Toast.makeText(requireContext(), "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.resetProgressBar.isVisible = isLoading
        binding.savePasswordButton.isEnabled = !isLoading
        binding.currentPasswordEditText.isEnabled = !isLoading
        binding.newPasswordEditText.isEnabled = !isLoading
        binding.confirmPasswordEditText.isEnabled = !isLoading
    }

    private fun getLoggedUserId(): Int {
        val sessionManager = SessionManager(requireContext().applicationContext)
        val userSession = sessionManager.getSession()

        android.util.Log.d("ResetPassword", "Active Session UserId: ${userSession?.userId}")

        val parsedId = userSession?.userId?.toIntOrNull() ?: -1

        if (parsedId <= 0) {
            val sharedPref = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE)
            return sharedPref.getInt("USER_ID", -1)
        }

        return parsedId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}