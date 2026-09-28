package com.sashtech.mehndidesignsimple.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sashtech.mehndidesignsimple.R
import com.sashtech.mehndidesignsimple.data.model.MehndiDesign
import com.sashtech.mehndidesignsimple.utils.MehndiImageLoader

/**
 * RecyclerView Adapter demonstrating dynamic image loading from Firebase Storage / Firestore
 * using Coil with placeholder() and error() handling for "No Image Available".
 */
class MehndiDesignAdapter(
    private val onDesignClick: (MehndiDesign) -> Unit,
    private val onFavoriteClick: ((MehndiDesign) -> Unit)? = null
) : ListAdapter<MehndiDesign, MehndiDesignAdapter.MehndiDesignViewHolder>(DesignDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MehndiDesignViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mehndi_design_card, parent, false)
        return MehndiDesignViewHolder(view, onDesignClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: MehndiDesignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MehndiDesignViewHolder(
        itemView: View,
        private val onDesignClick: (MehndiDesign) -> Unit,
        private val onFavoriteClick: ((MehndiDesign) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView? = itemView.findViewById(R.id.imageDesign)
        private val titleTextView: TextView? = itemView.findViewById(R.id.textDesignTitle)
        private val categoryTextView: TextView? = itemView.findViewById(R.id.textCategory)
        private val favoriteButton: View? = itemView.findViewById(R.id.btnFavorite)

        fun bind(design: MehndiDesign) {
            titleTextView?.text = design.title.ifEmpty { "Mehndi Pattern" }
            categoryTextView?.text = design.categoryName.ifEmpty { design.category }

            // Dynamic Image Loading via Coil with neutral fallback/error graphic
            imageView?.let { iv ->
                val dynamicUrl = design.getGridImageUrl()
                MehndiImageLoader.loadImage(
                    imageView = iv,
                    imageUrl = dynamicUrl,
                    crossfadeDurationMs = 200
                )
            }

            itemView.setOnClickListener { onDesignClick(design) }
            favoriteButton?.setOnClickListener { onFavoriteClick?.invoke(design) }
        }
    }

    object DesignDiffCallback : DiffUtil.ItemCallback<MehndiDesign>() {
        override fun areItemsTheSame(oldItem: MehndiDesign, newItem: MehndiDesign): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MehndiDesign, newItem: MehndiDesign): Boolean =
            oldItem == newItem
    }
}
