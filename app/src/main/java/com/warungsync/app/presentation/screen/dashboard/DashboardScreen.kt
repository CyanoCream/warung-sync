package com.warungsync.app.presentation.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.CustomDateRange
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemTrendData
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TrendTimeframe
import com.warungsync.app.presentation.components.EmptyStateView
import com.warungsync.app.presentation.components.PriceTrendCard
import com.warungsync.app.presentation.screen.tokolist.RoleBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    currentToko: Toko,
    allItems: List<Item>,
    categories: List<Category>,
    trendCharts: List<ItemTrendData>,
    currentTimeframe: TrendTimeframe,
    customRange: CustomDateRange?,
    onTimeframeSelected: (TrendTimeframe) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    onAddChartItem: (Item) -> Unit,
    onRemoveChartItem: (String) -> Unit,
    onBackToTokoList: () -> Unit,
    showHeader: Boolean = true
) {
    var selectedCategoryForCount by remember { mutableStateOf<String?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasActiveFilter = selectedCategoryForCount != null || currentTimeframe != TrendTimeframe.THIS_MONTH

    val filteredItemsCount = if (selectedCategoryForCount == null) {
        allItems.size
    } else {
        allItems.count { item ->
            selectedCategoryForCount in item.categoryIds.ifEmpty { listOf(item.categoryId) }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            if (showHeader) TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Dashboard",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RoleBadge(role = currentToko.myRole)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToTokoList) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = "Ganti Toko",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter Dashboard",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Ringkasan Metrik Produk & Kategori
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card Total Produk
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$filteredItemsCount",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (selectedCategoryForCount == null) "Total Produk" else "Produk Terfilter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Card Total Kategori
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${categories.size}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Kategori",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Section 2: Tren Harga
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tren Harga",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { showFilterSheet = true }
                        ) {
                            Text(
                                text = if (currentTimeframe == TrendTimeframe.CUSTOM && customRange != null) {
                                    "${dateFormatter.format(Date(customRange.startTimestamp))} - ${dateFormatter.format(Date(customRange.endTimestamp))}"
                                } else {
                                    currentTimeframe.label
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showAddItemDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Grafik",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // List Chart Cards
            if (trendCharts.isEmpty()) {
                item {
                    EmptyStateView()
                }
            } else {
                items(trendCharts, key = { it.item.id }) { trendData ->
                    PriceTrendCard(
                        trendData = trendData,
                        onRemoveChart = { onRemoveChartItem(trendData.item.id) }
                    )
                }
            }
        }
    }

    // Modal Popup Filter Dashboard
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedCategoryForCount != null || currentTimeframe != TrendTimeframe.THIS_MONTH) {
                        TextButton(
                            onClick = {
                                selectedCategoryForCount = null
                                onTimeframeSelected(TrendTimeframe.THIS_MONTH)
                            }
                        ) {
                            Text("Reset", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Periode Grafik (Timeframe)
                Text(
                    text = "Periode Grafik",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrendTimeframe.entries.forEach { timeframe ->
                        val isSelected = currentTimeframe == timeframe
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (timeframe == TrendTimeframe.CUSTOM) {
                                    showDateRangePicker = true
                                } else {
                                    onTimeframeSelected(timeframe)
                                }
                            },
                            leadingIcon = if (timeframe == TrendTimeframe.CUSTOM) {
                                { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = {
                                if (timeframe == TrendTimeframe.CUSTOM && customRange != null && isSelected) {
                                    Text("${dateFormatter.format(Date(customRange.startTimestamp))} - ${dateFormatter.format(Date(customRange.endTimestamp))}")
                                } else {
                                    Text(timeframe.label)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                if (categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hitung Produk per Kategori
                    Text(
                        text = "Kategori Produk",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryForCount == null,
                            onClick = { selectedCategoryForCount = null },
                            label = { Text("Semua (${allItems.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        categories.forEach { cat ->
                            val count = allItems.count { item ->
                                cat.id in item.categoryIds.ifEmpty { listOf(item.categoryId) }
                            }
                            FilterChip(
                                selected = selectedCategoryForCount == cat.id,
                                onClick = {
                                    selectedCategoryForCount = if (selectedCategoryForCount == cat.id) null else cat.id
                                },
                                label = { Text("${cat.namaKategori} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Material 3 Date Range Picker Dialog
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()

        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis ?: start
                        if (start != null && end != null) {
                            onCustomRangeSelected(start, end)
                        }
                        showDateRangePicker = false
                    }
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text(text = "Pilih Rentang Tanggal Grafik", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.fillMaxWidth().height(480.dp)
            )
        }
    }

    // Dialog Tambah Barang ke Watchlist Chart
    if (showAddItemDialog) {
        AddChartItemDialog(
            items = allItems,
            existingItemIds = trendCharts.map { it.item.id }.toSet(),
            onDismiss = { showAddItemDialog = false },
            onItemSelected = { item ->
                onAddChartItem(item)
                showAddItemDialog = false
            }
        )
    }
}

@Composable
fun AddChartItemDialog(
    items: List<Item>,
    existingItemIds: Set<String>,
    onDismiss: () -> Unit,
    onItemSelected: (Item) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val availableItems = remember(items, existingItemIds, searchQuery) {
        items.filter { it.id !in existingItemIds && it.namaBarang.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Barang untuk Grafik") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari barang...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (availableItems.isEmpty()) {
                    Text(
                        text = if (items.isEmpty()) "Belum ada data barang." else "Semua barang sudah ada di grafik / tidak ditemukan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(availableItems, key = { it.id }) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemSelected(item) }
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.namaBarang, fontWeight = FontWeight.Bold)
                                        item.categoryNames
                                            .ifEmpty { listOfNotNull(item.categoryName) }
                                            .takeIf { it.isNotEmpty() }
                                            ?.let {
                                            Text(
                                                text = it.joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Rp %,d".format(item.harga.toLong()),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

