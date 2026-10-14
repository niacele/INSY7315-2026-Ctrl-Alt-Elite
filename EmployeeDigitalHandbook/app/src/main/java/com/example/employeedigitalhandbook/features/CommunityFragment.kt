package com.example.employeedigitalhandbook.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.employeedigitalhandbook.R
import com.example.employeedigitalhandbook.adapters.EventAdapter
import com.example.employeedigitalhandbook.repositories.EventResult
import com.example.employeedigitalhandbook.viewmodel.CommunityViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class CommunityFragment : Fragment() {

    private lateinit var communityViewModel: CommunityViewModel
    private lateinit var eventAdapter: EventAdapter
    private lateinit var backArrowImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        communityViewModel = ViewModelProvider(this)[CommunityViewModel::class.java]

        // Initialize btnBack before calling setOnClickListener
        backArrowImageView = view.findViewById(R.id.backArrowImageView)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewEvents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        eventAdapter = EventAdapter(emptyList()) { selectedEvent ->
            Toast.makeText(
                requireContext(),
                "${selectedEvent.title}\n${selectedEvent.location}",
                Toast.LENGTH_SHORT
            ).show()
        }
        recyclerView.adapter = eventAdapter

        backArrowImageView.setOnClickListener {
            findNavController().navigateUp()
        }

        communityViewModel.eventsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is EventResult.Success -> {
                    eventAdapter.updateData(result.events)
                }
                is EventResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutEvents)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = when (tab?.position) {
                    1 -> "Upcoming"
                    2 -> "Social"
                    3 -> "Wellness & Team"
                    else -> "All"
                }
                communityViewModel.filterEventsByCategory(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        communityViewModel.loadEvents()
    }
}