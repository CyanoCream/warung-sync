package com.warungsync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.network.sync.SyncOrchestrator
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.presentation.screen.createtoko.CreateTokoScreen
import com.warungsync.app.presentation.screen.dashboard.DashboardScreen
import com.warungsync.app.presentation.screen.history.PriceHistoryScreen
import com.warungsync.app.presentation.screen.itemlist.ItemListScreen
import com.warungsync.app.presentation.components.ProductFilterDrawer
import com.warungsync.app.presentation.screen.jointoko.JoinTokoScreen
import com.warungsync.app.presentation.screen.master.MasterDataScreen
import com.warungsync.app.presentation.screen.master.MasterDataSection
import com.warungsync.app.presentation.screen.onboarding.OnboardingScreen
import com.warungsync.app.presentation.screen.tokolist.TokoListScreen
import com.warungsync.app.presentation.screen.tokosettings.TokoSettingsScreen
import com.warungsync.app.presentation.theme.MulyaSyncTheme
import com.warungsync.app.presentation.theme.AppThemeMode
import com.warungsync.app.presentation.viewmodel.MainViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private val devicePreferences: DevicePreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var themeMode by remember {
                mutableStateOf(AppThemeMode.fromStored(devicePreferences.themeMode))
            }
            val useDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            MulyaSyncTheme(darkTheme = useDarkTheme) {
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
                val discoveredTokos by viewModel.discoveredTokos.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val trendCharts by viewModel.trendCharts.collectAsState()
                val selectedTrendTimeframe by viewModel.selectedTrendTimeframe.collectAsState()
                val customDateRange by viewModel.customDateRange.collectAsState()
                val isDataTransferRunning by viewModel.isDataTransferRunning.collectAsState()
                val dataTransferMessage by viewModel.dataTransferMessage.collectAsState()

                val exportCsvLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/csv")
                ) { uri ->
                    uri?.let(viewModel::exportCurrentToko)
                }
                val importCsvLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        if (myTokos.isEmpty()) {
                            viewModel.restoreBackupAsFirstToko(it) {
                                currentDestination = AppDestination.TOKO_DASHBOARD
                            }
                        } else {
                            viewModel.importIntoCurrentToko(it)
                        }
                    }
                }

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

                // BackHandler untuk tombol back hardware / gesture swipe
                BackHandler(enabled = currentDestination != AppDestination.TOKO_LIST && currentDestination != AppDestination.ONBOARDING) {
                    when (currentDestination) {
                        AppDestination.PRICE_HISTORY -> {
                            viewModel.clearSelectedItemForHistory()
                            currentDestination = AppDestination.TOKO_DASHBOARD
                        }
                        AppDestination.TOKO_SETTINGS -> {
                            currentDestination = AppDestination.TOKO_DASHBOARD
                        }
                        AppDestination.CREATE_TOKO,
                        AppDestination.JOIN_TOKO,
                        AppDestination.TOKO_DASHBOARD -> {
                            currentDestination = AppDestination.TOKO_LIST
                        }
                        else -> {
                            currentDestination = AppDestination.TOKO_LIST
                        }
                    }
                }

                when (currentDestination) {
                    AppDestination.ONBOARDING -> {
                        OnboardingScreen(
                            onDeviceNameSaved = { deviceName ->
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
                            onRestoreBackupClick = {
                                importCsvLauncher.launch(
                                    arrayOf(
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "application/vnd.ms-excel",
                                        "text/plain"
                                    )
                                )
                            },
                            isRestoringBackup = isDataTransferRunning,
                            backupStatusMessage = dataTransferMessage,
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
                            discoveredTokos = discoveredTokos,
                            isLoading = isLoading,
                            statusMessage = errorMessage,
                            onBackClick = { currentDestination = AppDestination.TOKO_LIST },
                            onRefreshClick = { viewModel.refreshDiscoveredTokos() },
                            onJoinToko = { toko ->
                                viewModel.joinPeerToko(toko) {
                                    currentDestination = AppDestination.TOKO_DASHBOARD
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
                                onExportData = {
                                    val safeName = currentToko.namaToko
                                        .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                                        .trim('_')
                                        .ifBlank { "Toko" }
                                        .take(40)
                                    val date = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    exportCsvLauncher.launch("WarungSync_${safeName}_$date.csv")
                                },
                                onImportData = {
                                    importCsvLauncher.launch(
                                        arrayOf(
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "application/vnd.ms-excel",
                                            "text/plain"
                                        )
                                    )
                                },
                                isDataTransferRunning = isDataTransferRunning,
                                dataTransferMessage = dataTransferMessage,
                                onDataTransferMessageShown = viewModel::clearDataTransferMessage,
                                onBackToTokoList = { currentDestination = AppDestination.TOKO_LIST },
                                onManageTokoClick = { currentDestination = AppDestination.TOKO_SETTINGS },
                                onAddItem = { nama, deskripsi, harga, unitQuantity, satuan, catId ->
                                    viewModel.addItem(nama, deskripsi, harga, unitQuantity, satuan, catId)
                                },
                                onUpdateItem = { id, nama, deskripsi, harga, unitQuantity, satuan, catId ->
                                    viewModel.updateItem(id, nama, deskripsi, harga, unitQuantity, satuan, catId)
                                },
                                onDeleteItem = { viewModel.deleteItem(it) },
                                onAddCategory = { nama, color -> viewModel.addCategory(nama, color) },
                                onUpdateCategory = { id, nama, color -> viewModel.updateCategory(id, nama, color) },
                                onDeleteCategory = { viewModel.deleteCategory(it) },
                                themeMode = themeMode,
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    devicePreferences.themeMode = mode.name
                                },
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

private enum class StoreSection { CATALOG, DASHBOARD, ITEMS, CATEGORIES }

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
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    isDataTransferRunning: Boolean,
    dataTransferMessage: String?,
    onDataTransferMessageShown: () -> Unit,
    onBackToTokoList: () -> Unit,
    onManageTokoClick: () -> Unit,
    onAddItem: (nama: String, deskripsi: String?, harga: Double, unitQuantity: Double, satuan: String, categoryId: String) -> Unit,
    onUpdateItem: (id: String, nama: String, deskripsi: String?, harga: Double, unitQuantity: Double, satuan: String, categoryId: String) -> Unit,
    onDeleteItem: (id: String) -> Unit,
    onAddCategory: (String, Int) -> Unit,
    onUpdateCategory: (id: String, String, Int) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onHistoryClick: (com.warungsync.app.domain.model.Item) -> Unit
) {
    val canAccessAdminTabs = currentToko.myRole == MemberRole.OWNER || currentToko.myRole == MemberRole.ADMIN
    var selectedSection by remember(currentToko.id) { mutableStateOf(StoreSection.CATALOG) }
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(dataTransferMessage) {
        dataTransferMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            onDataTransferMessageShown()
        }
    }

    fun select(section: StoreSection) {
        selectedSection = section
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = currentToko.namaToko,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Text(
                        text = "${currentToko.myRole.name} • ${currentToko.ownerDeviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    NavigationDrawerItem(
                        label = { Text("Daftar Produk") },
                        selected = selectedSection == StoreSection.CATALOG,
                        onClick = { select(StoreSection.CATALOG) },
                        icon = { Icon(Icons.Default.ListAlt, null) }
                    )
                    if (canAccessAdminTabs) {
                        NavigationDrawerItem(
                            label = { Text("Dashboard") },
                            selected = selectedSection == StoreSection.DASHBOARD,
                            onClick = { select(StoreSection.DASHBOARD) },
                            icon = { Icon(Icons.Default.Analytics, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Barang") },
                            selected = selectedSection == StoreSection.ITEMS,
                            onClick = { select(StoreSection.ITEMS) },
                            icon = { Icon(Icons.Default.Inventory2, null) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Kategori") },
                            selected = selectedSection == StoreSection.CATEGORIES,
                            onClick = { select(StoreSection.CATEGORIES) },
                            icon = { Icon(Icons.Default.Category, null) }
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Tema",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text(if (isSyncing) "Menyinkronkan…" else "Sinkronkan sekarang") },
                        selected = false,
                        onClick = {
                            onSyncClick()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Sync, null) }
                    )
                    NavigationDrawerItem(
                        label = { Text(if (isDataTransferRunning) "Memproses data…" else "Export backup CSV") },
                        selected = false,
                        onClick = {
                            if (!isDataTransferRunning) {
                                onExportData()
                                scope.launch { drawerState.close() }
                            }
                        },
                        icon = { Icon(Icons.Default.FileDownload, null) }
                    )
                    if (canAccessAdminTabs) {
                        NavigationDrawerItem(
                            label = { Text("Import & gabungkan CSV") },
                            selected = false,
                            onClick = {
                                if (!isDataTransferRunning) {
                                    onImportData()
                                    scope.launch { drawerState.close() }
                                }
                            },
                            icon = { Icon(Icons.Default.FileUpload, null) }
                        )
                    }
                    if (currentToko.myRole == MemberRole.OWNER) {
                        NavigationDrawerItem(
                            label = { Text("Pengaturan toko") },
                            selected = false,
                            onClick = onManageTokoClick,
                            icon = { Icon(Icons.Default.Settings, null) }
                        )
                    }
                    NavigationDrawerItem(
                        label = { Text("Ganti toko") },
                        selected = false,
                        onClick = onBackToTokoList,
                        icon = { Icon(Icons.Default.Store, null) }
                    )
                    lastSyncResult?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            when (selectedSection) {
            StoreSection.CATALOG -> ProductFilterDrawer(
                categories = categories,
                selectedCategoryId = filter.categoryId,
                minPrice = filter.minHarga,
                maxPrice = filter.maxHarga,
                currentSort = filter.sortBy,
                onCategorySelected = onCategorySelected,
                onPriceRangeChanged = onPriceRangeChanged,
                onSortChanged = onSortChanged
            ) { ItemListScreen(
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
                onHistoryClick = onHistoryClick,
                showHeader = false,
                searchOnly = true
            ) }
            StoreSection.DASHBOARD -> {
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
                        onBackToTokoList = onBackToTokoList,
                        showHeader = false
                    )
                } else {
                    selectedSection = StoreSection.CATALOG
                }
            }
            StoreSection.ITEMS, StoreSection.CATEGORIES -> {
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
                        onBackToTokoList = onBackToTokoList,
                        section = if (selectedSection == StoreSection.ITEMS) MasterDataSection.ITEMS else MasterDataSection.CATEGORIES,
                        showHeader = false
                    )
                } else {
                    selectedSection = StoreSection.CATALOG
                }
            }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )

        }
    }
}
