package com.nicscreations.cryptids

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.io.File

class CryptidDetailActivity : AppCompatActivity() {

    private val cryptidId: Int by lazy {
        intent.getIntExtra(EXTRA_CRYPTID_ID, 0)
    }

    private lateinit var backButton: ImageButton
    private lateinit var detailTitle: TextView
    private lateinit var cryptidImage: ImageView
    private lateinit var typeBadge: TextView
    private lateinit var locationText: TextView
    private lateinit var otherNamesText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var factText: TextView
    private lateinit var tipsText: TextView
    private lateinit var sourceLabel: TextView
    private lateinit var sourceUrl: TextView
    private lateinit var emptyState: TextView
    private lateinit var imageCard: MaterialCardView

    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cryptid_detail)

        // Initialize views
        backButton = findViewById(R.id.backButton)
        detailTitle = findViewById(R.id.detailTitle)
        cryptidImage = findViewById(R.id.cryptidImage)
        typeBadge = findViewById(R.id.typeBadge)
        locationText = findViewById(R.id.locationText)
        otherNamesText = findViewById(R.id.otherNamesText)
        descriptionText = findViewById(R.id.descriptionText)
        factText = findViewById(R.id.factText)
        tipsText = findViewById(R.id.tipsText)
        sourceLabel = findViewById(R.id.sourceLabel)
        sourceUrl = findViewById(R.id.sourceUrl)
        emptyState = findViewById(R.id.emptyState)
        imageCard = findViewById(R.id.imageCard)

        // DebugLogger init
        DebugLogger.init()
        DebugLogger.log("CryptidDetailActivity", "onCreate: cryptidId=$cryptidId")

        // Back button
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load cryptid data
        loadCryptid(cryptidId)
    }

    private fun loadCryptid(id: Int) {
        RetrofitClient.apiService.getCryptid(id).enqueue(object : retrofit2.Callback<Cryptid> {
            override fun onResponse(call: retrofit2.Call<Cryptid>, response: retrofit2.Response<Cryptid>) {
                isLoading = false
                if (response.isSuccessful && response.body() != null) {
                    val cryptid = response.body()!!
                    displayCryptid(cryptid)
                    DebugLogger.log("CryptidDetailActivity", "onResponse: loaded cryptid ${cryptid.name}")
                } else {
                    showError("Cryptid not found")
                    DebugLogger.logError("CryptidDetailActivity", "onResponse: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<Cryptid>, t: Throwable) {
                isLoading = false
                showError("Network error: ${t.message}")
                DebugLogger.logError("CryptidDetailActivity", "onFailure", t)
            }
        })
    }

    private fun displayCryptid(cryptid: Cryptid) {
        // Title
        detailTitle.text = cryptid.name

        // Type badge with color
        val typeColors = mapOf(
            "aquatic" to "#3498db",
            "terrestrial" to "#e74c3c",
            "flying" to "#9b59b6"
        )
        val typeColor = typeColors[cryptid.type?.lowercase()] ?: "#625FA6"
        typeBadge.text = cryptid.type?.replaceFirstChar { it.uppercase() } ?: "Unknown"
        typeBadge.setBackgroundColor(android.graphics.Color.parseColor(typeColor))

        // Location
        if (cryptid.country != null || cryptid.location != null) {
            locationText.visibility = View.VISIBLE
            val parts = mutableListOf<String>()
            cryptid.country?.let { parts.add(it) }
            cryptid.location?.let { parts.add(it) }
            locationText.text = parts.joinToString(", ")
        } else {
            locationText.visibility = View.GONE
        }

        // Other names
        otherNamesText.text = cryptid.other_names ?: getString(R.string.no_other_names)

        // Description
        descriptionText.text = cryptid.description ?: getString(R.string.no_description)

        // Fact
        factText.text = cryptid.fact ?: getString(R.string.no_fact)

        // Tips
        tipsText.text = cryptid.tips ?: getString(R.string.no_tips)

        // Source URL
        if (!cryptid.source_url.isNullOrEmpty()) {
            sourceLabel.visibility = View.VISIBLE
            sourceUrl.visibility = View.VISIBLE
            sourceUrl.text = cryptid.source_url
            sourceUrl.setOnClickListener {
                try {
                    val uri = Uri.parse(cryptid.source_url)
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    DebugLogger.logError("CryptidDetailActivity", "Failed to open source URL", e)
                }
            }
        } else {
            sourceLabel.visibility = View.GONE
            sourceUrl.visibility = View.GONE
        }

        // Load image
        loadImage(cryptid.image_url)
    }

    private fun loadImage(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(cryptidImage)

            // Set content description
            cryptidImage.contentDescription = "${detailTitle.text} image"
        } else {
            // No image - hide card or show placeholder
            imageCard.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        emptyState.visibility = View.VISIBLE
        emptyState.text = message
        detailTitle.text = getString(R.string.error_loading)
        imageCard.visibility = View.GONE
    }
}
