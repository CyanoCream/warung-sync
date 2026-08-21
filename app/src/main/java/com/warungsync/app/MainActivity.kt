package com.warungsync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.network.sync.SyncOrchestrator
import com.warungsync.app.presentation.screen.createtoko.CreateTokoScreen
import com.warungsync.app.presentation.screen.dashboard.DashboardScreen
import com.warungsync.app.presentation.screen.history.PriceHistoryScreen
import com.warungsync.app.presentation.screen.itemlist.ItemListScreen
import com.warungsync.app.presentation.screen.jointoko.JoinTokoScreen
import com.warungsync.app.presentation.screen.master.MasterDataScreen
import com.warungsync.app.presentation.screen.onboarding.OnboardingScreen
import com.warungsync.app.presentation.screen.tokolist.TokoListScreen
import com.warungsync.app.presentation.screen.tokosettings.TokoSettingsScreen
import com.warungsync.app.presentation.theme.MulyaSyncTheme
import com.warungsync.app.presentation.viewmodel.MainViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel

enum class AppDestination {
    ONBOARDING,
    TOKO_LIST,
    CREATE_TOKO,
    JOIN_TOKO,
    TOKO_DASHBOARD,
    TOKO_SETTINGS,
    PRICE_HISTORY
}

class MainActivity : ComponentActivity() {

    private val syncOrchestrator: SyncOrchestrator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MulyaSyncTheme {
                val viewModel: MainViewModel = koinViewModel()
                val prefs = viewModel.prefs

                // Lifecycle: start P2P sync orchestrator on launch
                DisposableEffect(Unit) {
                    syncOrchestrator.start()
                    onDispose {
                        syncOrchestrator.stop()
                    }
                }

                var currentDestination by remember {
                    mutableStateOf(
                        if (prefs.isOnboardingCompleted) AppDestination.TOKO_LIST
                        else AppDestination.ONBOARDING
                    )
                }

                val myTokos by viewModel.myTokos.collectAsState()
                val activeToko by viewModel.activeToko.collectAsState()
                val items by viewModel.items.collectAsState()
                val allItems by viewModel.allItems.collectAsState()
                val categories by viewModel.categories.collectAsState()
                val filter by viewModel.filter.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()
                val lastSyncResult by viewModel.lastSyncResult.collectAsState()
                val selectedItemForHistory by viewModel.selectedItemForHistory.collectAsState()
                val priceHistories by viewModel.priceHistories.collectAsState()
                val activeTokoMembers by viewModel.activeTokoMembers.collectAsState()
                val discoveredPeers by viewModel.discoveredPeers.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val trendCharts by viewModel.trendCharts.collectAsState()
                val selectedTrendTimeframe by viewModel.selectedTrendTimeframe.collectAsState()
                val customDateRange by viewModel.customDateRange.collectAsState()

                // Logic 1: Auto open jika hanya 1 toko atau memiliki toko default aktif
                var hasCheckedAutoOpen by remember { mutableStateOf(false) }
                LaunchedEffect(myTokos) {
                    if (!hasCheckedAutoOpen && prefs.isOnboardingCompleted && myTokos.isNotEmpty()) {
                        hasCheckedAutoOpen = true
                        if (myTokos.size == 1) {
                            // Hanya punya 1 toko: langsung buka katalog toko tersebut
                            viewModel.selectToko(myTokos.first())
                            currentDestination = AppDestination.TOKO_DASHBOARD
                        } else if (prefs.autoOpenDefaultToko && prefs.defaultTokoId != null) {
                            val defaultToko = myTokos.find { it.id == prefs.defaultTokoId }
                            if (defaultToko != null) {
                                viewModel.selectToko(defaultToko)
                                currentDestination = AppDestination.TOKO_DASHBOARD
                            }
                        }
                    }
                }

                when (currentDestination) {
                    AppDestination.ONBOARDING -> {
                        OnboardingScreen(
                            onComplete = { deviceName ->
                                viewModel.setDeviceNameAndCompleteOnboarding(deviceName)
                                currentDestination = AppDestination.TOKO_LIST
                            }
                        )
                    }

                    AppDestination.TOKO_LIST -> {
                        TokoListScreen(
                            tokos = myTokos,
                            activeTokoId = activeToko?.id,
                            defaultTokoId = viewModel.defaultTokoId,
                            autoOpenDefault = viewModel.autoOpenDefault,
                            deviceName = viewModel.deviceName,
                            onSelectToko = { toko ->
                                viewModel.selectToko(toko)
                                currentDestination = AppDestination.TOKO_DASHBOARD
                            },
                            onSetDefaultToko = { tokoId -> viewModel.setDefaultToko(tokoId) },
                            onToggleAutoOpen = { enable -> viewModel.setAutoOpenDefaultToko(enable) },
                            onCreateTokoClick = { currentDestination = AppDestination.CREATE_TOKO },
                            onJoinTokoClick = { currentDestination = AppDestination.JOIN_TOKO },
                            onLeaveTokoClick = { tokoId -> viewModel.leaveToko(tokoId) }
                        )
                    }

                    AppDestination.CREATE_TOKO -> {
                        CreateTokoScreen(
                            createdTokoCount = viewModel.createdTokoCount,
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onBackClick = { currentDestination = AppDestination.TOKO_LIST },
                            onCreateToko = { namaToko, totpCode ->
                                viewModel.createToko(namaToko, totpCode) {
                                    currentDestination = AppDestination.TOKO_DASHBOARD
                                }
                            }
                        )
                    }

                    AppDestination.JOIN_TOKO -> {
                        JoinTokoScreen(
                            discoveredPeers = discoveredPeers,
                            isLoading = isLoading,
                            statusMessage = errorMessage,
                            onBackClick = { currentDestination = AppDestination.TOKO_LIST },
                            onRefreshClick = { viewModel.triggerManualSync() },
                            onJoinPeerToko = { peer ->
                                viewModel.joinPeerToko(peer) {
                                    currentDestination = AppDestination.TOKO_LIST
                                }
                            }
                        )
                    }

                    AppDestination.TOKO_SETTINGS -> {
                        activeToko?.let { toko ->
                            TokoSettingsScreen(
                                toko = toko,
                                members = activeTokoMembers,
                                isOwner = toko.myRole == MemberRole.OWNER,
                                onBackClick = { currentDestination = AppDestination.TOKO_DASHBOARD },
                                onUpdateNamaToko = { viewModel.updateNamaToko(it) },
                                onUpdateMemberRole = { memberDeviceId, newRole ->
                                    viewModel.updateMemberRole(memberDeviceId, newRole)
                                },
                                onKickMember = { memberDeviceId -> viewModel.kickMember(memberDeviceId) },
                                onDeleteToko = {
                                    viewModel.deleteCurrentToko {
                                        currentDestination = AppDestination.TOKO_LIST
                                    }
                                }
                            )
                        } ?: run {
                            currentDestination = AppDestination.TOKO_LIST
                        }
                    }

                    AppDestination.PRICE_HISTORY -> {
                        selectedItemForHistory?.let { item ->
                            PriceHistoryScreen(
                                item = item,
                                histories = priceHistories,
                                onBackClick = {
                                    viewModel.clearSelectedItemForHistory()
                                    currentDestination = AppDestination.TOKO_DASHBOARD
                                }
                            )
                        } ?: run {
                            currentDestination = AppDestination.TOKO_DASHBOARD
                        }
                    }

                    AppDestination.TOKO_DASHBOARD -> {
                        activeToko?.let { currentToko ->
                            TokoDashboard(
                                currentToko = currentToko,
                                items = items,
                                allItems = allItems,
                                categories = categories,
                                filter = filter,
                                isSyncing = isSyncing,
                                lastSyncResult = lastSyncResult,
                                trendCharts = trendCharts,
                                currentTimeframe = selectedTrendTimeframe,
                                customDateRange = customDateRange,
                                onTimeframeSelected = { viewModel.setTrendTimeframe(it) },
                                onCustomRangeSelected = { start, end -> viewModel.setCustomDateRange(start, end) },
                                onAddChartItem = { viewModel.addChartItem(it) },
                                onRemoveChartItem = { viewModel.removeChartItem(it) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onCategorySelected = { viewModel.updateCategoryFilter(it) },
                                onPriceRangeChanged = { min, max -> viewModel.updatePriceRange(min, max) },
                                onSortChanged = { viewModel.updateSortBy(it) },
                                onSyncClick = { viewModel.triggerManualSync() },
                                onBackToTokoList = { currentDestination = AppDestination.TOKO_LIST },
                                onManageTokoClick = { currentDestination = AppDestination.TOKO_SETTINGS },
                                onAddItem = { nama, deskripsi, harga, satuan, catId ->
                                    viewModel.addItem(nama, deskripsi, harga, satuan, catId)
                                },
                                onUpdateItem = { id, nama, deskripsi, harga, satuan, catId ->
                                    viewModel.updateItem(id, nama, deskripsi, harga, satuan, catId)
                                },
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onAddCategory = { viewModel.addCategory(it) },
                                onUpdateCategory = { id, nama -> viewModel.updateCategory(id, nama) },
                                onDeleteCategory = { viewModel.deleteCategory(it) },
                                onHistoryClick = { item ->
                                    viewModel.selectItemForHistory(item)
                                    currentDestination = AppDestination.PRICE_HISTORY
                                }
                            )
                        } ?: run {
                            currentDestination = AppDestination.TOKO_LIST
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokoDashboard(
    currentToko: Toko,
    items: List<com.warungsync.app.domain.model.Item>,
    allItems: List<com.warungsync.app.domain.model.Item>,
    categories: List<com.warungsync.app.domain.model.Category>,
    filter: com.warungsync.app.domain.model.ItemFilter,
    isSyncing: Boolean,
    lastSyncResult: String?,
    trendCharts: List<com.warungsync.app.domain.model.ItemTrendData>,
    currentTimeframe: com.warungsync.app.domain.model.TrendTimeframe,
    customDateRange: com.warungsync.app.domain.model.CustomDateRange?,
    onTimeframeSelected: (com.warungsync.app.domain.model.TrendTimeframe) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onAddChartItem: (com.warungsync.app.domain.model.Item) -> Unit,
    onRemoveChartItem: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    onSortChanged: (com.warungsync.app.domain.model.SortBy) -> Unit,
    onSyncClick: () -> Unit,
    onBackToTokoList: () -> Unit,
    onManageTokoClick: () -> Unit,
    onAddItem: (nama: String, deskripsi: String?, harga: Double, satuan: String, categoryId: String) -> Unit,
    onUpdateItem: (id: String, nama: String, deskripsi: String?, harga: Double, satuan: String, categoryId: String) -> Unit,
    onDeleteItem: (id: String) -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (id: String, String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    onHistoryClick: (com.warungsync.app.domain.model.Item) -> Unit
) {
    val canAccessAdminTabs = currentToko.myRole == MemberRole.OWNER || currentToko.myRole == MemberRole.ADMIN
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Katalog") },
                    label = { Text("Katalog") }
                )
                if (canAccessAdminTabs) {
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = "Master Data") },
                        label = { Text("Master Data") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ItemListScreen(
                currentToko = currentToko,
                items = items,
                categories = categories,
                filter = filter,
                isSyncing = isSyncing,
                lastSyncMessage = lastSyncResult,
                onSearchQueryChange = onSearchQueryChange,
                onCategorySelected = onCategorySelected,
                onPriceRangeChanged = onPriceRangeChanged,
                onSortChanged = onSortChanged,
                onSyncClick = onSyncClick,
                onBackToTokoList = onBackToTokoList,
                onManageTokoClick = onManageTokoClick,
                onItemClick = onHistoryClick,
                onHistoryClick = onHistoryClick
            )
            1 -> {
                if (canAccessAdminTabs) {
                    DashboardScreen(
                        currentToko = currentToko,
                        allItems = allItems,
                        categories = categories,
                        trendCharts = trendCharts,
                        currentTimeframe = currentTimeframe,
                        customRange = customDateRange,
                        onTimeframeSelected = onTimeframeSelected,
                        onCustomRangeSelected = onCustomRangeSelected,
                        onAddChartItem = onAddChartItem,
                        onRemoveChartItem = onRemoveChartItem,
                        onBackToTokoList = onBackToTokoList
                    )
                } else {
                    selectedTab = 0
                }
            }
            2 -> {
                if (canAccessAdminTabs) {
                    MasterDataScreen(
                        currentToko = currentToko,
                        items = items,
                        categories = categories,
                        filter = filter,
                        onSearchQueryChange = onSearchQueryChange,
                        onCategorySelected = onCategorySelected,
                        onPriceRangeChanged = onPriceRangeChanged,
                        onSortChanged = onSortChanged,
                        onAddItem = onAddItem,
                        onUpdateItem = onUpdateItem,
                        onDeleteItem = onDeleteItem,
                        onAddCategory = onAddCategory,
                        onUpdateCategory = onUpdateCategory,
                        onDeleteCategory = onDeleteCategory,
                        onHistoryClick = onHistoryClick,
                        onBackToTokoList = onBackToTokoList
                    )
                } else {
                    selectedTab = 0
                }
            }
        }
    }
}
