//package com.example.employeedigitalhandbook.features
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.webkit.WebChromeClient
//import android.webkit.WebView
//import android.webkit.WebViewClient
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import com.example.employeedigitalhandbook.api.ApiClient
//import com.example.employeedigitalhandbook.databinding.FragmentPoliciesBackBinding
//import kotlinx.coroutines.launch
//import java.net.URLEncoder
//
//class PoliciesBackFragment : Fragment() {
//
//    private var _binding: FragmentPoliciesBackBinding? = null
//    private val binding get() = _binding!!
//
//    private var policyId: Int = -1
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        policyId = arguments?.getInt("POLICY_ID", -1) ?: -1
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentPoliciesBackBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        setupWebView()
//        setupControls()
//
//        if (policyId != -1) {
//            fetchAndLoadPolicyDetails(policyId)
//        } else {
//            Toast.makeText(requireContext(), "Invalid Policy ID", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun setupWebView() {
//        binding.pdfWebView.apply {
//            settings.javaScriptEnabled = true
//            settings.domStorageEnabled = true
//            settings.builtInZoomControls = true
//            settings.displayZoomControls = false
//
//            webViewClient = object : WebViewClient() {
//                override fun onPageFinished(view: WebView?, url: String?) {
//                    super.onPageFinished(view, url)
//                    _binding?.pdfProgressBar?.visibility = View.GONE
//                }
//            }
//
//            webChromeClient = object : WebChromeClient() {
//                override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                    if (newProgress < 100) {
//                        _binding?.pdfProgressBar?.visibility = View.VISIBLE
//                    } else {
//                        _binding?.pdfProgressBar?.visibility = View.GONE
//                    }
//                }
//            }
//        }
//    }
//
//    private fun setupControls() {
//        binding.backArrowImageView.setOnClickListener {
//            findNavController().navigateUp()
//        }
//
//        binding.previousPageIcon.setOnClickListener {
//            if (binding.pdfWebView.canGoBack()) {
//                binding.pdfWebView.goBack()
//            }
//        }
//
//        binding.nextPageIcon.setOnClickListener {
//            if (binding.pdfWebView.canGoForward()) {
//                binding.pdfWebView.goForward()
//            }
//        }
//
//        binding.searchPdfIcon.setOnClickListener {
//            binding.pdfWebView.showFindDialog(null, true)
//        }
//    }
//
//    private fun fetchAndLoadPolicyDetails(id: Int) {
//        binding.pdfProgressBar.visibility = View.VISIBLE
//
//        lifecycleScope.launch {
//            try {
//                val response = ApiClient.apiService.getPolicyById(id)
//                val body = response.body()
//
//                if (response.isSuccessful && body != null && body.data != null) {
//                    val policy = body.data
//
//                    binding.pageTitleTextView.text = policy.category
//                    binding.breadcrumbTextView.text = policy.breadcrumbPath
//
//                    val rawPdfUrl = policy.pdfUrl
//                    if (!rawPdfUrl.isNullOrEmpty()) {
//                        val encodedUrl = URLEncoder.encode(rawPdfUrl, "UTF-8")
//                        val webViewUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
//                        binding.pdfWebView.loadUrl(webViewUrl)
//                    } else {
//                        binding.pdfProgressBar.visibility = View.GONE
//                        Toast.makeText(requireContext(), "No PDF document attached", Toast.LENGTH_SHORT).show()
//                    }
//                } else {
//                    binding.pdfProgressBar.visibility = View.GONE
//                    Toast.makeText(requireContext(), "Failed to fetch policy details", Toast.LENGTH_SHORT).show()
//                }
//            } catch (e: Exception) {
//                binding.pdfProgressBar.visibility = View.GONE
//                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}

package com.example.employeedigitalhandbook.features

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.employeedigitalhandbook.api.ApiClient
import com.example.employeedigitalhandbook.databinding.FragmentPoliciesBackBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PoliciesBackFragment : Fragment() {

    private var _binding: FragmentPoliciesBackBinding? = null
    private val binding get() = _binding!!

    private var policyId: Int = -1

    //new pdf rendering fields
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        policyId = arguments?.getInt("POLICY_ID", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoliciesBackBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupControls()

        if (policyId != -1) {
            fetchAndLoadPolicyDetails(policyId)
        } else {
            Toast.makeText(requireContext(), "Invalid Policy ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupControls() {
        binding.backArrowImageView.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.previousPageIcon.setOnClickListener {
            if (currentPageIndex > 0) {
                renderPage(currentPageIndex - 1)
            }
        }

        binding.nextPageIcon.setOnClickListener {
            val totalPages = pdfRenderer?.pageCount ?: 0
            if (currentPageIndex < totalPages - 1) {
                renderPage(currentPageIndex + 1)
            }
        }
    }

    private fun fetchAndLoadPolicyDetails(id: Int) {
        binding.pdfProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getPolicyById(id)
                val body = response.body()

                if (response.isSuccessful && body != null && body.data != null) {
                    val policy = body.data

                    binding.pageTitleTextView.text = policy.category
                    binding.breadcrumbTextView.text = policy.breadcrumbPath

                    // Download securely via API
                    downloadAndRenderPdf()
                } else {
                    binding.pdfProgressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to fetch policy details", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.pdfProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun downloadAndRenderPdf() {
        try {
            val localFile = withContext(Dispatchers.IO) {
                // Stream the PDF through your secure Cloud Run API endpoint
                val downloadResponse = ApiClient.apiService.downloadFile("api/policies/$policyId/file")

                if (!downloadResponse.isSuccessful || downloadResponse.body() == null) {
                    throw Exception("HTTP ${downloadResponse.code()}: ${downloadResponse.message()}")
                }

                // Save binary stream into private app cache
                val cacheFile = File(requireContext().cacheDir, "current_policy.pdf")
                downloadResponse.body()!!.byteStream().use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                cacheFile
            }

            // Initialize Android's native PdfRenderer
            fileDescriptor = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
            binding.pdfProgressBar.visibility = View.GONE

            if ((pdfRenderer?.pageCount ?: 0) > 0) {
                renderPage(0)
            }
        } catch (e: Exception) {
            binding.pdfProgressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "PDF Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderPage(index: Int) {
        val renderer = pdfRenderer ?: return
        if (index < 0 || index >= renderer.pageCount) return

        currentPage?.close()
        currentPage = renderer.openPage(index)
        currentPageIndex = index

        val page = currentPage!!

        //scaling
        val densityMultiplier = 2
        val bitmap = Bitmap.createBitmap(
            page.width * densityMultiplier,
            page.height * densityMultiplier,
            Bitmap.Config.ARGB_8888
        )

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        binding.pdfImageView.setImageBitmap(bitmap)

        val total = renderer.pageCount
        binding.pageNumberTextView.text = "${index + 1} / $total"
        binding.previousPageIcon.alpha = if (index > 0) 1.0f else 0.4f
        binding.previousPageIcon.isEnabled = index > 0
        binding.nextPageIcon.alpha = if (index < total - 1) 1.0f else 0.4f
        binding.nextPageIcon.isEnabled = index < total - 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentPage?.close()
        currentPage = null
        pdfRenderer?.close()
        pdfRenderer = null
        fileDescriptor?.close()
        fileDescriptor = null
        _binding = null
    }
}