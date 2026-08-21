package com.warungsync.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.warungsync.app.data.backup.CsvBackupManager
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.CustomDateRange
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.ItemTrendData
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.PriceHistory
import com.warungsync.app.domain.model.SortBy
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.domain.model.TrendTimeframe
import com.warungsync.app.domain.usecase.category.AddCategoryUseCase
import com.warungsync.app.domain.usecase.category.DeleteCategoryUseCase
import com.warungsync.app.domain.usecase.category.GetAllCategoriesUseCase
import com.warungsync.app.domain.usecase.category.UpdateCategoryUseCase
import com.warungsync.app.domain.usecase.item.AddItemUseCase
import com.warungsync.app.domain.usecase.item.DeleteItemUseCase
import com.warungsync.app.domain.usecase.item.GetFilteredItemsUseCase
import com.warungsync.app.domain.usecase.item.GetItemPriceTrendUseCase
import com.warungsync.app.domain.usecase.item.GetPriceHistoryUseCase
import com.warungsync.app.domain.usecase.item.UpdateItemUseCase
import com.warungsync.app.domain.usecase.toko.CreateTokoUseCase
import com.warungsync.app.domain.usecase.toko.DeleteTokoUseCase
import com.warungsync.app.domain.usecase.toko.GetMyTokosUseCase
import com.warungsync.app.domain.usecase.toko.GetTokoMembersUseCase
import com.warungsync.app.domain.usecase.toko.KickMemberUseCase
import com.warungsync.app.domain.usecase.toko.LeaveTokoUseCase
import com.warungsync.app.domain.usecase.toko.UpdateMemberRoleUseCase
import com.warungsync.app.domain.usecase.toko.UpdateNamaTokoUseCase
import com.warungsync.app.network.discovery.DiscoveredPeer
import com.warungsync.app.network.discovery.DiscoveredToko
import com.warungsync.app.network.discovery.NsdDiscoveryManager
import com.warungsync.app.network.sync.SyncClient
import com.warungsync.app.network.sync.SyncOrchestrator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(
    private val getMyTokosUseCase: GetMyTokosUseCase,
    private val createTokoUseCase: CreateTokoUseCase,
    private val updateNamaTokoUseCase: UpdateNamaTokoUseCase,
    private val deleteTokoUseCase: DeleteTokoUseCase,
    private val leaveTokoUseCase: LeaveTokoUseCase,
    private val getTokoMembersUseCase: GetTokoMembersUseCase,
    private val updateMemberRoleUseCase: UpdateMemberRoleUseCase,
    private val kickMemberUseCase: KickMemberUseCase,
    private val getFilteredItemsUseCase: GetFilteredItemsUseCase,
    private val getItemPriceTrendUseCase: GetItemPriceTrendUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val getPriceHistoryUseCase: GetPriceHistoryUseCase,
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val syncOrchestrator: SyncOrchestrator,
    private val nsdManager: NsdDiscoveryManager,
    private val syncClient: SyncClient,
    private val csvBackupManager: CsvBackupManager,
    val prefs: DevicePreferences
) : ViewModel() {

    val deviceName: String get() = prefs.deviceName
    val createdTokoCount: Int get() = prefs.createdTokoCount
    val defaultTokoId: String? get() = prefs.defaultTokoId
    val autoOpenDefault: Boolean get() = prefs.autoOpenDefaultToko

    // List of Tokos joined by this device
    val myTokos: StateFlow<List<Toko>> = getMyTokosUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Selected Toko
    private val _activeToko = MutableStateFlow<Toko?>(null)
    val activeToko: StateFlow<Toko?> = _activeToko.asStateFlow()

    // Filter & Sort State
    private val _filter = MutableStateFlow(ItemFilter())
    val filter: StateFlow<ItemFilter> = _filter.asStateFlow()

    // Keep text input immediate, but avoid rebuilding a Room query for every
    // keystroke. Non-search filters still apply immediately.
    private val effectiveFilter = _filter
        .debounce { state -> if (state.searchQuery.isEmpty()) 0L else 250L }
        .distinctUntilChanged()

    // Items for active toko with filter
    val items: StateFlow<List<Item>> = combine(_activeToko, effectiveFilter) { toko, filterState ->
        Pair(toko, filterState)
    }.flatMapLatest { (toko, filterState) ->
        if (toko == null) flowOf(emptyList())
        else getFilteredItemsUseCase(toko.id, filterState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Items without filter (for Dashboard counts & search)
    val allItems: StateFlow<List<Item>> = _activeToko.flatMapLatest { toko ->
        if (toko == null) flowOf(emptyList())
        else getFilteredItemsUseCase(toko.id, ItemFilter())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Categories for active toko
    val categories: StateFlow<List<Category>> = _activeToko.flatMapLatest { toko ->
        if (toko == null) flowOf(emptyList())
        else getAllCategoriesUseCase(toko.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Members for active toko
    val activeTokoMembers: StateFlow<List<TokoMember>> = _activeToko.flatMapLatest { toko ->
        if (toko == null) flowOf(emptyList())
        else getTokoMembersUseCase(toko.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected item for price history
    private val _selectedItemForHistory = MutableStateFlow<Item?>(null)
    val selectedItemForHistory: StateFlow<Item?> = _selectedItemForHistory.asStateFlow()

    val priceHistories: StateFlow<List<PriceHistory>> = combine(_activeToko, _selectedItemForHistory) { toko, item ->
        Pair(toko, item)
    }.flatMapLatest { (toko, item) ->
        if (toko == null || item == null) flowOf(emptyList())
        else getPriceHistoryUseCase(toko.id, item.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard & Price Trend Watchlist ---
    private val _selectedTrendTimeframe = MutableStateFlow(TrendTimeframe.THIS_MONTH)
    val selectedTrendTimeframe: StateFlow<TrendTimeframe> = _selectedTrendTimeframe.asStateFlow()

    private val _customDateRange = MutableStateFlow<CustomDateRange?>(null)
    val customDateRange: StateFlow<CustomDateRange?> = _customDateRange.asStateFlow()

    private val _chartItemIds = MutableStateFlow<List<String>>(emptyList())
    val chartItemIds: StateFlow<List<String>> = _chartItemIds.asStateFlow()

    val trendCharts: StateFlow<List<ItemTrendData>> = combine(
        _activeToko,
        allItems,
        _chartItemIds,
        _selectedTrendTimeframe,
        _customDateRange
    ) { toko, itemsList, chartIds, timeframe, customRange ->
        if (toko == null || itemsList.isEmpty()) return@combine emptyList<ItemTrendData>()

        val targetIds = if (chartIds.isEmpty()) itemsList.take(3).map { it.id } else chartIds
        val targetItems = itemsList.filter { it.id in targetIds }

        val (startTime, endTime) = if (timeframe == TrendTimeframe.CUSTOM && customRange != null) {
            Pair(customRange.startTimestamp, customRange.endTimestamp)
        } else {
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.MONTH, -timeframe.monthsBack)
            val start = cal.timeInMillis
            Pair(start, end)
        }

        targetItems.map { item ->
            ItemTrendData(
                item = item,
                initialPrice = item.harga,
                currentPrice = item.harga,
                priceChangeAmount = 0.0,
                priceChangePercent = 0.0,
                points = emptyList()
            )
        }
    }.flatMapLatest { placeholderList ->
        val toko = _activeToko.value
        val itemsList = allItems.value
        val chartIds = _chartItemIds.value
        val timeframe = _selectedTrendTimeframe.value
        val customRange = _customDateRange.value

        if (toko == null || itemsList.isEmpty()) return@flatMapLatest flowOf(emptyList())

        val targetIds = if (chartIds.isEmpty()) itemsList.take(3).map { it.id } else chartIds
        val targetItems = itemsList.filter { it.id in targetIds }

        val (startTime, endTime) = if (timeframe == TrendTimeframe.CUSTOM && customRange != null) {
            Pair(customRange.startTimestamp, customRange.endTimestamp)
        } else {
            val cal = Calendar.getInstance()
            val end = cal.timeInMillis
            cal.add(Calendar.MONTH, -timeframe.monthsBack)
            val start = cal.timeInMillis
            Pair(start, end)
        }

        val flows = targetItems.map { getItemPriceTrendUseCase(toko.id, it, startTime, endTime) }
        if (flows.isEmpty()) flowOf(emptyList())
        else combine(flows) { it.toList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Discovered NSD peers for joining
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = nsdManager.discoveredPeers
    private val discoveryRefreshVersion = MutableStateFlow(0)
    val discoveredTokos: StateFlow<List<DiscoveredToko>> = combine(
        discoveredPeers,
        discoveryRefreshVersion
    ) { peers, _ -> peers }
        .mapLatest { peers ->
            coroutineScope {
                peers.map { peer ->
                    async {
                        val info = syncClient.fetchDeviceInfo(peer.hostAddress, peer.port).getOrNull()
                            ?: return@async emptyList()
                        if (info.deviceId == prefs.deviceId) return@async emptyList()

                        info.servedTokos.map { toko ->
                            DiscoveredToko(
                                tokoId = toko.id,
                                namaToko = toko.namaToko,
                                ownerName = toko.ownerName,
                                peer = peer
                            )
                        }
                    }
                }.awaitAll()
                    .flatten()
                    .distinctBy { it.tokoId }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Loading & Messages
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isDataTransferRunning = MutableStateFlow(false)
    val isDataTransferRunning: StateFlow<Boolean> = _isDataTransferRunning.asStateFlow()

    private val _dataTransferMessage = MutableStateFlow<String?>(null)
    val dataTransferMessage: StateFlow<String?> = _dataTransferMessage.asStateFlow()

    val isSyncing: StateFlow<Boolean> = syncOrchestrator.isSyncing
    val lastSyncResult: StateFlow<String?> = syncOrchestrator.lastSyncResult

    init {
        // Observe and automatically resolve active toko
        viewModelScope.launch {
            myTokos.collect { tokos ->
                if (tokos.isNotEmpty()) {
                    val savedTokoId = prefs.activeTokoId
                    val matched = tokos.find { it.id == savedTokoId } ?: tokos.first()
                    _activeToko.value = matched
                    prefs.activeTokoId = matched.id
                } else {
                    _activeToko.value = null
                    prefs.activeTokoId = null
                }
            }
        }
    }

    fun selectToko(toko: Toko) {
        _activeToko.value = toko
        prefs.activeTokoId = toko.id
        _filter.value = ItemFilter()
        _chartItemIds.value = emptyList()
    }

    fun setDefaultToko(tokoId: String?) {
        prefs.defaultTokoId = tokoId
    }

    fun setAutoOpenDefaultToko(enable: Boolean) {
        prefs.autoOpenDefaultToko = enable
    }

    fun setDeviceNameAndCompleteOnboarding(name: String) {
        prefs.deviceName = name
        prefs.isOnboardingCompleted = true
    }

    // Toko Operations
    fun createToko(namaToko: String, totpCode: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            createTokoUseCase(namaToko, totpCode).fold(
                onSuccess = { newToko ->
                    _isLoading.value = false
                    selectToko(newToko)
                    onSuccess()
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.message ?: "Gagal membuat toko"
                }
            )
        }
    }

    fun updateNamaToko(namaToko: String) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            updateNamaTokoUseCase(tokoId, namaToko)
        }
    }

    fun deleteCurrentToko(onDeleted: () -> Unit) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            deleteTokoUseCase(tokoId).fold(
                onSuccess = { onDeleted() },
                onFailure = { _errorMessage.value = it.message }
            )
        }
    }

    fun leaveToko(tokoId: String) {
        viewModelScope.launch {
            leaveTokoUseCase(tokoId)
        }
    }

    fun updateMemberRole(memberDeviceId: String, newRole: MemberRole) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            updateMemberRoleUseCase(tokoId, memberDeviceId, newRole)
        }
    }

    fun kickMember(memberDeviceId: String) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            kickMemberUseCase(tokoId, memberDeviceId)
        }
    }

    // Join Toko via Peer
    fun refreshDiscoveredTokos() {
        discoveryRefreshVersion.update { it + 1 }
    }

    fun exportCurrentToko(uri: Uri) {
        val toko = _activeToko.value ?: return
        viewModelScope.launch {
            _isDataTransferRunning.value = true
            csvBackupManager.exportToko(toko.id, uri).fold(
                onSuccess = { result ->
                    _dataTransferMessage.value =
                        "Backup ${result.itemCount} produk dan ${result.categoryCount} kategori tersimpan sebagai " +
                        "${result.fileName} di lokasi yang kamu pilih."
                },
                onFailure = { error ->
                    _dataTransferMessage.value = "Export gagal: ${error.message ?: "file tidak dapat dibuat"}"
                }
            )
            _isDataTransferRunning.value = false
        }
    }

    fun importIntoCurrentToko(uri: Uri) {
        val toko = _activeToko.value ?: return
        if (toko.myRole == MemberRole.USER) {
            _dataTransferMessage.value = "Hanya OWNER atau ADMIN yang dapat mengimpor data."
            return
        }
        viewModelScope.launch {
            _isDataTransferRunning.value = true
            csvBackupManager.importIntoToko(toko.id, uri).fold(
                onSuccess = { result ->
                    _dataTransferMessage.value =
                        "Import ${result.fileName} selesai: ${result.insertedCount} data baru, " +
                        "${result.updatedCount} diperbarui, ${result.skippedCount} dilewati karena sudah lebih baru."
                },
                onFailure = { error ->
                    _dataTransferMessage.value = "Import gagal: ${error.message ?: "format file tidak valid"}"
                }
            )
            _isDataTransferRunning.value = false
        }
    }

    fun restoreBackupAsFirstToko(uri: Uri, onSuccess: () -> Unit) {
        if (_activeToko.value != null) {
            importIntoCurrentToko(uri)
            return
        }
        viewModelScope.launch {
            _isDataTransferRunning.value = true
            val metadata = csvBackupManager.inspectBackup(uri).getOrElse { error ->
                _dataTransferMessage.value = "Pemulihan gagal: ${error.message ?: "format file tidak valid"}"
                _isDataTransferRunning.value = false
                return@launch
            }
            val newToko = createTokoUseCase(metadata.sourceTokoName, null).getOrElse { error ->
                _dataTransferMessage.value = "Pemulihan gagal: ${error.message ?: "toko tidak dapat dibuat"}"
                _isDataTransferRunning.value = false
                return@launch
            }
            selectToko(newToko)
            csvBackupManager.importIntoToko(newToko.id, uri).fold(
                onSuccess = { result ->
                    _dataTransferMessage.value =
                        "Backup ${result.fileName} dipulihkan: ${result.insertedCount} data masuk, " +
                        "${result.updatedCount} diperbarui."
                },
                onFailure = { error ->
                    _dataTransferMessage.value =
                        "Toko sudah dibuat, tetapi import gagal: ${error.message}. Coba import ulang dari sidebar."
                }
            )
            _isDataTransferRunning.value = false
            onSuccess()
        }
    }

    fun clearDataTransferMessage() {
        _dataTransferMessage.value = null
    }

    fun joinPeerToko(target: DiscoveredToko, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val joinResult = syncClient.requestJoinToko(
                target.tokoId,
                target.peer.hostAddress,
                target.peer.port
            )
            joinResult.fold(
                onSuccess = { resp ->
                    if (resp.success && resp.toko != null) {
                        // Await Room's joined-toko query so navigation can never see
                        // a temporary empty list and show the create-store screen.
                        val joinedToko = getMyTokosUseCase()
                            .first { tokos -> tokos.any { it.id == resp.toko.id } }
                            .first { it.id == resp.toko.id }
                        selectToko(joinedToko)
                        _isLoading.value = false
                        onSuccess()
                    } else {
                        _isLoading.value = false
                        _errorMessage.value = resp.message
                    }
                },
                onFailure = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err.message ?: "Gagal bergabung ke toko"
                }
            )
        }
    }

    // Dashboard Watchlist Chart Operations
    fun setTrendTimeframe(timeframe: TrendTimeframe) {
        _selectedTrendTimeframe.value = timeframe
    }

    fun setCustomDateRange(startTimestamp: Long, endTimestamp: Long) {
        _customDateRange.value = CustomDateRange(startTimestamp, endTimestamp)
        _selectedTrendTimeframe.value = TrendTimeframe.CUSTOM
    }

    fun addChartItem(item: Item) {
        val current = _chartItemIds.value.toMutableList()
        if (current.isEmpty()) {
            val defaultIds = allItems.value.take(3).map { it.id }
            current.addAll(defaultIds)
        }
        if (item.id !in current) {
            current.add(item.id)
            _chartItemIds.value = current
        }
    }

    fun removeChartItem(itemId: String) {
        val current = _chartItemIds.value.toMutableList()
        if (current.isEmpty()) {
            val defaultIds = allItems.value.take(3).map { it.id }
            current.addAll(defaultIds)
        }
        current.remove(itemId)
        _chartItemIds.value = current
    }

    // Filter & Search Operations
    fun updateSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun updateCategoryFilter(categoryId: String?) {
        _filter.value = _filter.value.copy(categoryId = categoryId)
    }

    fun updatePriceRange(min: Double?, max: Double?) {
        _filter.value = _filter.value.copy(minHarga = min, maxHarga = max)
    }

    fun updateSortBy(sortBy: SortBy) {
        _filter.value = _filter.value.copy(sortBy = sortBy)
    }

    // Item Operations
    fun addItem(nama: String, deskripsi: String?, harga: Double, unitQuantity: Double, satuan: String, categoryIds: List<String>) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            addItemUseCase(tokoId, nama, deskripsi, harga, unitQuantity, satuan, categoryIds).onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun updateItem(id: String, nama: String, deskripsi: String?, harga: Double, unitQuantity: Double, satuan: String, categoryIds: List<String>) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            updateItemUseCase(tokoId, id, nama, deskripsi, harga, unitQuantity, satuan, categoryIds).onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun deleteItem(id: String) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            deleteItemUseCase(tokoId, id).onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    // Category Operations
    fun addCategory(nama: String, colorArgb: Int) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            addCategoryUseCase(tokoId, nama, colorArgb).onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun updateCategory(id: String, nama: String, colorArgb: Int) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            updateCategoryUseCase(tokoId, id, nama, colorArgb).onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun deleteCategory(id: String) {
        val tokoId = _activeToko.value?.id ?: return
        viewModelScope.launch {
            deleteCategoryUseCase(tokoId, id).onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    // Price History
    fun selectItemForHistory(item: Item) {
        _selectedItemForHistory.value = item
    }

    fun clearSelectedItemForHistory() {
        _selectedItemForHistory.value = null
    }

    // Sync
    fun triggerManualSync() {
        syncOrchestrator.triggerSync()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
