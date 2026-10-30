package com.example.employeedigitalhandbook.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.employeedigitalhandbook.R
import com.example.employeedigitalhandbook.data.CommunityEvent
import com.google.android.material.card.MaterialCardView

class EventAdapter(
    private var events: List<CommunityEvent>,
    private val onItemClick: (CommunityEvent) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardEvent)
        val title: TextView = itemView.findViewById(R.id.txtEventTitle)
        val date: TextView = itemView.findViewById(R.id.txtEventDate)
        val time: TextView = itemView.findViewById(R.id.txtEventTime)
        val location: TextView = itemView.findViewById(R.id.txtEventLocation)
        val description: TextView = itemView.findViewById(R.id.txtDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.title.text = if (event.title.isNotBlank()) event.title else "Untitled Event"

        holder.date.text = event.formattedDate
        holder.time.text = event.formattedTime
        holder.location.text = if (event.location.isNotBlank()) event.location else "N/A"

        // Bind description
        holder.description.text = if (event.description.isNotBlank()) event.description else "No description available"

        val colorHex = when (position % 3) {
            0 -> "#2690CF"
            1 -> "#8BC34A"
            else -> "#F57C00"
        }
        holder.card.setCardBackgroundColor(Color.parseColor(colorHex))

        holder.itemView.setOnClickListener { onItemClick(event) }
    }

    override fun getItemCount(): Int = events.size

    fun updateData(newEvents: List<CommunityEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}