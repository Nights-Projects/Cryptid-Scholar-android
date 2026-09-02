package com.nicscreations.cryptids

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CryptidAdapter(
    private val cryptids: List<Cryptid>
) : RecyclerView.Adapter<CryptidViewHolder>() {

    private var onItemClick: ((Cryptid) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CryptidViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cryptid, parent, false)
        return CryptidViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: CryptidViewHolder, position: Int) {
        holder.bind(cryptids[position])
    }

    override fun getItemCount(): Int = cryptids.size

    fun setOnItemClickListener(listener: (Cryptid) -> Unit) {
        onItemClick = listener
    }

    fun submitList(newList: List<Cryptid>) {
        val oldSize = cryptids.size
        // Create new backing list
        val updated = ArrayList<Cryptid>(newList)
        val diff = newList.size - oldSize
        cryptids = updated
        notifyItemRangeChanged(0, newList.size)
        if (diff > 0) {
            notifyItemRangeInserted(oldSize, diff)
        } else if (diff < 0) {
            notifyItemRangeRemoved(newList.size, -diff)
        }
    }
}

class CryptidViewHolder(
    view: View,
    private val onItemClick: ((Cryptid) -> Unit)?
) : RecyclerView.ViewHolder(view) {

    private val nameText: TextView = view.findViewById(R.id.cryptidName)
    private val typeText: TextView = view.findViewById(R.id.cryptidType)
    private val countryText: TextView = view.findViewById(R.id.cryptidCountry)
    private val imageView: ImageView = view.findViewById(R.id.cryptidImage)

    fun bind(cryptid: Cryptid) {
        nameText.text = cryptid.name
        typeText.text = cryptid.type?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        countryText.text = cryptid.country ?: "Unknown"

        // Load image with Glide
        if (!cryptid.image_url.isNullOrEmpty()) {
            Glide.with(imageView.context)
                .load(cryptid.image_url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(imageView)
        }

        // Click handling
        itemView.setOnClickListener { onItemClick?.invoke(cryptid) }
    }
}
