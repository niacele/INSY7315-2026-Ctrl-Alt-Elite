package com.example.employeedigitalhandbook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.data.Resource
import com.example.employeedigitalhandbook.databinding.FragmentResourcesBackPdfBinding
import com.example.employeedigitalhandbook.databinding.FragmentResourcesBackWebsiteBinding
import kotlinx.coroutines.launch
import java.net.URLEncoder

class ResourcesBackFragment : Fragment() {

    private var pdfBinding: FragmentResourcesBackPdfBinding? = null
    private var websiteBinding: FragmentResourcesBackWebsiteBinding? = null

    private var resourceId: Int = -1
    private var isPdfMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resourceId = arguments?.getInt("RESOURCE_ID", -1) ?: -1
        isPdfMode = arguments?.getBoolean("IS_PDF_MODE", false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (isPdfMode) {
            pdfBinding = FragmentResourcesBackPdfBinding.inflate(inflater, container, false)
            pdfBinding!!.root
        } else {
            websiteBinding = FragmentResourcesBackWebsiteBinding.inflate(inflater, container, false)
            websiteBinding!!.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupControls()

        if (!isPdfMode) {
            setupWebView()
        }

        if (resourceId != -1) {
            fetchAndLoadResource(resourceId)
        } else {
            Toast.makeText(requireContext(), "Invalid Resource ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView() {
        websiteBinding?.resourceWebView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    websiteBinding?.resourceProgressBar?.isVisible = false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    websiteBinding?.resourceProgressBar?.isVisible = newProgress < 100
                }
            }
        }
    }

    private fun setupControls() {
        if (isPdfMode) {
            pdfBinding?.apply {
                backArrowImageView.setOnClickListener { findNavController().navigateUp() }
            }
        } else {
            websiteBinding?.apply {
                backArrowImageView.setOnClickListener { findNavController().navigateUp() }
                previousPageIcon.setOnClickListener {
                    if (resourceWebView.canGoBack()) resourceWebView.goBack()
                }
                nextPageIcon.setOnClickListener {
                    if (resourceWebView.canGoForward()) resourceWebView.goForward()
                }
                searchDocIcon.setOnClickListener {
                    resourceWebView.showFindDialog(null, true)
                }
            }
        }
    }

    private fun fetchAndLoadResource(id: Int) {
        setLoadingState(true)

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getResourceById(id)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!

                    if (apiResponse.success && apiResponse.data != null) {
                        val resource: Resource = apiResponse.data
                        val targetUrl = resource.resourceUrl ?: ""

                        if (isPdfMode) {
                            pdfBinding?.apply {
                                pageTitleTextView.text = resource.title
                                breadcrumbTextView.text = resource.breadcrumbPath
                                    ?: "../${resource.category}/${resource.title}"

                                resourceProgressBar.isVisible = false
                            }
                        } else {
                            websiteBinding?.apply {
                                pageTitleTextView.text = resource.title
                                breadcrumbTextView.text = resource.breadcrumbPath
                                    ?: "../${resource.category}/${resource.title}"

                                if (targetUrl.isNotEmpty()) {
                                    val urlToLoad = if (targetUrl.endsWith(".pdf", ignoreCase = true) || targetUrl.contains(".pdf?")) {
                                        val encodedUrl = URLEncoder.encode(targetUrl, "UTF-8")
                                        "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                                    } else {
                                        targetUrl
                                    }
                                    resourceWebView.loadUrl(urlToLoad)
                                } else {
                                    resourceProgressBar.isVisible = false
                                    Toast.makeText(requireContext(), "No URL configured", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        setLoadingState(false)
                        Toast.makeText(requireContext(), apiResponse.message ?: "Failed to load resource", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    setLoadingState(false)
                    Toast.makeText(requireContext(), "Failed to load resource", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                setLoadingState(false)
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isPdfMode) {
            pdfBinding?.resourceProgressBar?.isVisible = isLoading
        } else {
            websiteBinding?.resourceProgressBar?.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfBinding = null
        websiteBinding = null
    }
}