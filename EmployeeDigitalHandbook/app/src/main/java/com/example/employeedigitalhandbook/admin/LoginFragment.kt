package com.example.employeedigitalhandbook.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.employeedigitalhandbook.api.ApiClient
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.employeedigitalhandbook.R
import com.example.employeedigitalhandbook.sessions.SessionManager
import com.example.employeedigitalhandbook.viewmodel.LoginViewModel
import com.example.employeedigitalhandbook.viewmodel.LoginViewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext().applicationContext)
        val factory = LoginViewModelFactory(sessionManager)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        val emailEditText = view.findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.passwordEditText)
        val loginButton = view.findViewById<MaterialButton>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text?.toString().orEmpty()
            val password = passwordEditText.text?.toString().orEmpty()

            loginButton.isEnabled = false
            viewModel.login(email, password)
        }

        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            loginButton.isEnabled = true

            when (result) {
                is AuthResult.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Welcome, ${result.user.fullName}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    ApiClient.clearCache()

                    findNavController().navigate(R.id.action_loginFragment_to_homePageFragment)
                    viewModel.clearState()
                }
                is AuthResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    viewModel.clearState()
                }
                null -> {

                }
            }
        }
    }
}