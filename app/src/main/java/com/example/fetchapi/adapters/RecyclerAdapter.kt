package com.example.fetchapi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fetchapi.R

class RecyclerAdapter(
    private var ids: List<Int>,
    private var listIds: List<Int>,
    private var names: List<String>
) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>()
{

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val itemListId: TextView = itemView.findViewById(R.id.list_id)
        val itemName: TextView = itemView.findViewById(R.id.object_name)
        val itemId: TextView = itemView.findViewById(R.id.object_id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ViewHolder
    {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.ViewHolder, position: Int)
    {
        holder.itemListId.text = "List Id: " + listIds[position].toString()
        holder.itemName.text = "Name: " + names[position]
        holder.itemId.text = "ID: " + ids[position]
    }

    override fun getItemCount(): Int
    {
        return listIds.size
    }

}