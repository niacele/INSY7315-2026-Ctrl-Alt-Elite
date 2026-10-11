package com.example.employeedigitalhandbook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.databinding.FragmentResourcesBackWebsiteBinding
import kotlinx.coroutines.launch

class ResourcesBackFragment : Fragment() {

    private var _binding: FragmentResourcesBackWebsiteBinding? = null
    private val binding get() = _binding!!

    private var resourceId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resourceId = arguments?.getInt("RESOURCE_ID", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResourcesBackWebsiteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWebView()
        setupControls()

        if (resourceId != -1) {
            fetchAndLoadResource(resourceId)
        } else {
            Toast.makeText(requireContext(), "Invalid Resource ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView() {
        binding.resourceWebView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    _binding?.resourceProgressBar?.visibility = View.GONE
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        _binding?.resourceProgressBar?.visibility = View.VISIBLE
                    } else {
                        _binding?.resourceProgressBar?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupControls() {
        binding.backArrowImageView.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.previousPageIcon.setOnClickListener {
            if (binding.resourceWebView.canGoBack()) {
                binding.resourceWebView.goBack()
            }
        }

        binding.nextPageIcon.setOnClickListener {
            if (binding.resourceWebView.canGoForward()) {
                binding.resourceWebView.goForward()
            }
        }

        binding.searchDocIcon.setOnClickListener {
            binding.resourceWebView.showFindDialog(null, true)
        }
    }

    private fun fetchAndLoadResource(id: Int) {
        binding.resourceProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getResourceById(id)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!

                    if (apiResponse.success && apiResponse.data != null) {
                        val resource = apiResponse.data //unpack data

                        _binding?.let { b ->
                            b.pageTitleTextView.text = resource.title
                            b.breadcrumbTextView.text = resource.breadcrumbPath
                                ?: "../${resource.category}/${resource.title}"

                            val targetUrl = resource.resourceUrl

                            if (!targetUrl.isNullOrEmpty()) {
                                val urlToLoad = if (targetUrl.endsWith(".pdf", ignoreCase = true) || targetUrl.contains(".pdf?")) {
                                    val encodedUrl = java.net.URLEncoder.encode(targetUrl, "UTF-8")
                                    "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                                } else {
                                    targetUrl
                                }

                                b.resourceProgressBar.visibility = View.GONE
                                b.resourceWebView.loadUrl(urlToLoad)
                            } else {
                                b.resourceProgressBar.visibility = View.GONE
                                context?.let { ctx ->
                                    Toast.makeText(ctx, "No URL configured", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        _binding?.let { b -> b.resourceProgressBar.visibility = View.GONE }
                        context?.let { ctx ->
                            Toast.makeText(ctx, apiResponse.message ?: "Failed to load details", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    _binding?.let { b -> b.resourceProgressBar.visibility = View.GONE }
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Failed to load details", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                _binding?.let { b -> b.resourceProgressBar.visibility = View.GONE }
                context?.let { ctx ->
                    Toast.makeText(ctx, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}