package com.nicscreations.cryptids

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var searchLayout: TextInputLayout
    private lateinit var searchInput: TextInputEditText
    private lateinit var filterGroup: ChipGroup
    private lateinit var statsText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var errorState: TextView
    private lateinit var bottomNav: BottomNavigationView

    // Data
    private val cryptidList = mutableListOf<Cryptid>()
    private val filteredList = mutableListOf<Cryptid>()
    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = AtomicBoolean(false)
    private var currentFilter: String? = null
    private var currentSearch: String? = null

    // Component 1: DebugLogger
    private lateinit var debugLogger: DebugLogger

    // Component 2: Retrofit API
    private val apiService: CryptidApiService by lazy {
        RetrofitClient.apiService
    }

    // Component 3: Adapter
    private lateinit var adapter: CryptidAdapter

    // Component 4: Filter management
    private val activeFilterChips = mutableListOf<Chip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Component 1: DebugLogger initialization
        debugLogger = DebugLogger
        DebugLogger.init()

        // Component 2 & 3: Setup UI components
        setupToolbar()
        setupSearch()
        setupFilterChips()
        setupStats()
        setupRecyclerView()
        setupSwipeRefresh()
        setupBottomNavigation()

        // Component 5: Initial data load
        loadData()
    }

    // Component 5: Main data loading orchestration
    private fun loadData() {
        if (isLoading.get()) return
        isLoading.set(true)

        // Reset to first page on new search/filter
        if (currentSearch != null || currentFilter != null) {
            currentPage = 1
        }

        DebugLogger.log("MainActivity", "loadData: page=$currentPage filter=$currentFilter search=$currentSearch")

        // Fetch both stats and cryptid list in parallel
        fetchStats()
        fetchCryptids()
    }

    // Component 6: Stats fetching
    private fun fetchStats() {
        swipeRefresh.isRefreshing = true

        apiService.getStats().enqueue(object : retrofit2.Callback<ApiStats> {
            override fun onResponse(call: retrofit2.Call<ApiStats>, response: retrofit2.Response<ApiStats>) {
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()!!
                    statsText.text = getString(R.string.stats_format,
                        stats.total, stats.aquatic, stats.terrestrial, stats.flying)
                    DebugLogger.log("MainActivity", "Stats loaded: ${stats.total} cryptids")
                } else {
                    DebugLogger.logError("MainActivity", "Stats failed: ${response.code()}")
                }
            }

            override fun onFailure(call: retrofit2.Call<ApiStats>, t: Throwable) {
                DebugLogger.logError("MainActivity", "Stats failure", t)
            }
        })
    }

    // Component 7: Cryptid list fetching with pagination
    private fun fetchCryptids() {
        val search = currentSearch?.trim() ?: ""
        val type = currentFilter

        swipeRefresh.isRefreshing = if (currentPage == 1) true else swipeRefresh.isRefreshing

        apiService.getCryptids(
            page = currentPage,
            perPage = 50,
            type = type,
            search = search.ifEmpty { null }
        ).enqueue(object : retrofit2.Callback<CryptidList> {
            override fun onResponse(call: retrofit2.Call<CryptidList>, response: retrofit2.Response<CryptidList>) {
                isLoading.set(false)
                swipeRefresh.isRefreshing = false

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    DebugLogger.log("MainActivity", "Cryptids loaded: ${result.cryptids.size} on page ${result.page}/${result.pages}")

                    if (result.pages > 0) {
                        totalPages = result.pages
                    }

                    if (currentPage == 1) {
                        cryptidList.clear()
                    }
                    cryptidList.addAll(result.cryptids)

                    applyFilters()
                } else {
                    DebugLogger.logError("MainActivity", "Cryptids failed: ${response.code()}")
                    showErrorState()
                }
            }

            override fun onFailure(call: retrofit2.Call<CryptidList>, t: Throwable) {
                isLoading.set(false)
                swipeRefresh.isRefreshing = false
                DebugLogger.logError("MainActivity", "Cryptids failure", t)
                showErrorState()
            }
        })
    }

    // Component 8: Filter application
    private fun applyFilters() {
        filteredList.clear()

        for (cryptid in cryptidList) {
            val passesSearch = if (!currentSearch.isNullOrEmpty()) {
                cryptid.name.contains(currentSearch, ignoreCase = true) ||
                (cryptid.other_names?.contains(currentSearch, ignoreCase = true) ?: false)
            } else true

            val passesType = if (!currentFilter.isNullOrEmpty()) {
                cryptid.type?.lowercase() == currentFilter
            } else true

            if (passesSearch && passesType) {
                filteredList.add(cryptid)
            }
        }

        DebugLogger.log("MainActivity", "Filtering: ${filteredList.size}/${cryptidList.size} shown")

        adapter.submitList(filteredList)

        if (filteredList.isEmpty() && !isLoading.get()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    // Component 9: Adapter submission
    private fun submitList(list: List<Cryptid>) {
        adapter.submitList(list)
    }

    // Component 10: Search handler
    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearch = s?.toString()?.trim()?.ifEmpty { null }
                // Clear and reload with new search
                cryptidList.clear()
                filteredList.clear()
                loadData()
            }
        })
    }

    // Component 11: Filter chip handlers
    private fun setupFilterChips() {
        // Chip click handlers
        filterGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chipAquatic) -> "aquatic"
                checkedIds.contains(R.id.chipTerrestrial) -> "terrestrial"
                checkedIds.contains(R.id.chipFlying) -> "flying"
                checkedIds.isEmpty() -> null // Allow deselect all
                else -> null
            }

            DebugLogger.log("MainActivity", "Filter changed: $currentFilter")

            // If all chips deselected, default to "all" (no filter)
            if (checkedIds.isEmpty()) {
                filterGroup.check(R.id.chipAll)
                currentFilter = null
            }

            // Reload on filter change
            cryptidList.clear()
            filteredList.clear()
            loadData()
        }
    }

    // Component 12: Error state display
    private fun showErrorState() {
        errorState.visibility = View.VISIBLE
        errorState.text = getString(R.string.empty_state)
        recyclerView.visibility = View.GONE
    }

    private fun hideErrorState() {
        errorState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        errorState.visibility = View.VISIBLE
        errorState.text = getString(R.string.empty_state)
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        errorState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    // Component 13: Swipe refresh
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    // Component 14: Bottom navigation
    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_cryptids -> {
                    // Already on cryptids tab, no action needed
                    true
                }
                R.id.nav_stats -> {
                    // Navigate to stats - we'll just show/hide the list
                    // For now, simple view toggle
                    recyclerView.visibility = View.GONE
                    errorState.visibility = View.VISIBLE
                    errorState.text = statsText.text ?: getString(R.string.stats_format, 0, 0, 0, 0)
                    true
                }
                else -> false
            }
        }
    }

    // Component 15: Toolbar setup
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    // Component 16: RecyclerView setup
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = CryptidAdapter(filteredList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Click listener for individual items
        adapter.setOnItemClickListener(onCryptidClick)
    }

    // Component 17: Cryptid item click -> detail activity
    private val onCryptidClick: (Cryptid) -> Unit = { cryptid ->
        DebugLogger.log("MainActivity", "Cryptid clicked: ${cryptid.name} (id=${cryptid.id})")

        val intent = android.content.Intent(this, CryptidDetailActivity::class.java)
        intent.putExtra(CryptidDetailActivity.EXTRA_CRYPTID_ID, cryptid.id)
        startActivity(intent)
    }

    // Component 18: API call wrapper with logging
    private fun safeApiCall(description: String, call: retrofit2.Call<*>) {
        DebugLogger.logNetwork("MainActivity", description, call.request().method(), null, null)
    }

    // Component 19: Scroll to load more (optional enhancement)
    private var isScrolling = false

    // Component 19: Manual search trigger on Enter key
    private fun setupEnterKeySearch() {
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                loadData()
                true
            } else false
        }
    }
}
