package com.example.tolets.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tolets.R

class AdapterAdminRiwayat(
    private val userList: List<Pair<String, String>>, // (uid, fullName)
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AdapterAdminRiwayat.UidViewHolder>() {

    inner class UidViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.txtuser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UidViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_uid, parent, false)
        return UidViewHolder(view)
    }

    override fun onBindViewHolder(holder: UidViewHolder, position: Int) {
        val (uid, fullName) = userList[position]
        holder.tvName.text = fullName
        holder.itemView.setOnClickListener {
            onClick(uid)
        }
    }

    override fun getItemCount(): Int = userList.size
}

