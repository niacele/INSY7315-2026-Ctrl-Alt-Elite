package com.example.employeedigitalhandbook.homePages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.employeedigitalhandbook.MainActivity
import com.example.employeedigitalhandbook.R
import com.example.employeedigitalhandbook.adapters.DoctorAdapter
import com.example.employeedigitalhandbook.data.Doctor
import com.example.employeedigitalhandbook.repositories.DoctorResult
import com.example.employeedigitalhandbook.viewmodel.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar

class HomePageFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        val tvGreetingPrefix = view.findViewById<TextView>(R.id.tvGreetingPrefix)
        val tvUserName = view.findViewById<TextView>(R.id.tvUserName)
        val txtAppVersion = view.findViewById<TextView>(R.id.txtAppVersion)
        val btnMenu = view.findViewById<ImageView>(R.id.btnMenu)

        //app version
        val currentVersion = try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            pInfo.versionName ?: "v1.0.0-beta.2"
        } catch (e: Exception) {
            "v1.0.0-beta.2"
        }

        txtAppVersion.text = "Version $currentVersion"
        tvGreetingPrefix.text = getGreetingPrefix()

        val loggedInUser = arguments?.getString("USER_NAME")
        if (!loggedInUser.isNullOrBlank()) {
            tvUserName.text = "$loggedInUser!"
        } else {
            tvUserName.text = "User!"
        }

        // card listeners
        view.findViewById<CardView>(R.id.cardPolicies).setOnClickListener {
            findNavController().navigate(R.id.action_homePageFragment_to_policiesFrontFragment)
        }
        view.findViewById<CardView>(R.id.cardBragBook).setOnClickListener {
            findNavController().navigate(R.id.action_homePageFragment_to_bragBookFragment)
        }
        view.findViewById<CardView>(R.id.cardCommunity).setOnClickListener {
            findNavController().navigate(R.id.action_homePageFragment_to_communityFragment)
        }
        view.findViewById<CardView>(R.id.cardCPD).setOnClickListener {
            findNavController().navigate(R.id.action_homePageFragment_to_resourcesFrontFragment)
        }

        //menu button
        view.findViewById<View>(R.id.btnMenu).setOnClickListener {
            (activity as? MainActivity)?.openMenu()
        }

        val doctorsRecyclerView = view.findViewById<RecyclerView>(R.id.doctorsRecyclerView)
        doctorsRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        homeViewModel.doctorsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is DoctorResult.ListSuccess -> {
                    doctorsRecyclerView.adapter = DoctorAdapter(result.doctors) { doctor ->
                        val bundle = androidx.core.os.bundleOf("doctorId" to doctor.doctorId)
                        findNavController().navigate(
                            R.id.action_homePageFragment_to_doctorFragment,
                            bundle
                        )
                    }
                }
                is DoctorResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }

        homeViewModel.loadDoctors()

        // bottom nav
//        val bottomNavigation = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)
//        bottomNavigation.selectedItemId = R.id.nav_home
//        bottomNavigation.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.nav_home -> true
//                R.id.nav_menu -> {
//                    findNavController().navigate(R.id.action_homePageFragment_to_settingsFragment)
//                    true
//                }
//                else -> false
//            }
//        }
    }

    //greeting
    private fun getGreetingPrefix(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning, "
            in 12..16 -> "Good afternoon, "
            else -> "Good evening, "
        }
    }
}