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

@OptIn(ExperimentalMaterial3Api::class)
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
    onBackToTokoList: () -> Unit
) {
    var selectedCategoryForCount by remember { mutableStateOf<String?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    val filteredItemsCount = if (selectedCategoryForCount == null) {
        allItems.size
    } else {
        allItems.count { it.categoryId == selectedCategoryForCount }
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dashboard ${currentToko.namaToko}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RoleBadge(role = currentToko.myRole)
                        }
                        Text(
                            text = "Ringkasan Inventaris & Tren Harga",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
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
                Text(
                    text = "Ringkasan Produk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

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
                                text = "Kategori Aktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Filter Hitung Produk per Kategori
            item {
                Text(
                    text = "Filter Hitung Produk Berdasarkan Kategori:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryForCount == null,
                            onClick = { selectedCategoryForCount = null },
                            label = { Text("Semua (${allItems.size})") }
                        )
                    }
                    items(categories, key = { it.id }) { cat ->
                        val count = allItems.count { it.categoryId == cat.id }
                        FilterChip(
                            selected = selectedCategoryForCount == cat.id,
                            onClick = {
                                selectedCategoryForCount = if (selectedCategoryForCount == cat.id) null else cat.id
                            },
                            label = { Text("${cat.namaKategori} ($count)") }
                        )
                    }
                }
            }

            // Section 2: Tren Harga & Watchlist Multi-Chart
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Grafik Tren Naik-Turun Harga",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pantau pergerakan harga komoditas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddItemDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Grafik", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Timeframe Selector Chips (Bulan Ini, 3 Bulan, 6 Bulan, 1 Tahun, Custom)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(TrendTimeframe.entries.toTypedArray()) { timeframe ->
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
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // List Chart Cards
            if (trendCharts.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Belum Ada Grafik Barang",
                        message = "Klik tombol '+ Tambah Grafik' di atas untuk memantau tren pergerakan harga barang tertentu (misal: Telur, Beras, Minyak).",
                        actionLabel = "+ Tambah Grafik Barang",
                        onActionClick = { showAddItemDialog = true }
                    )
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
