package com.warungsync.app.presentation.screen.itemlist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.SortBy
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.presentation.components.EmptyStateView
import com.warungsync.app.presentation.components.EnhancedFilterBar
import com.warungsync.app.presentation.components.ItemCard
import com.warungsync.app.presentation.components.RealtimeSearchBar
import com.warungsync.app.presentation.components.SyncStatusBanner
import com.warungsync.app.presentation.screen.tokolist.RoleBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    currentToko: Toko,
    items: List<Item>,
    categories: List<Category>,
    filter: ItemFilter,
    isSyncing: Boolean,
    lastSyncMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    onSortChanged: (SortBy) -> Unit,
    onSyncClick: () -> Unit,
    onBackToTokoList: () -> Unit,
    onManageTokoClick: () -> Unit,
    onItemClick: (Item) -> Unit,
    onHistoryClick: (Item) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentToko.namaToko,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            RoleBadge(role = currentToko.myRole)
                        }
                        Text(
                            text = "Katalog Harga Barang",
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
                actions = {
                    IconButton(onClick = onSyncClick, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync Data",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    if (currentToko.myRole == MemberRole.OWNER) {
                        IconButton(onClick = onManageTokoClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Kelola Toko",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Sync status banner
            SyncStatusBanner(
                isSyncing = isSyncing,
                lastSyncMessage = lastSyncMessage,
                onSyncClick = onSyncClick
            )

            // Search Bar Interaktif
            RealtimeSearchBar(
                query = filter.searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Enhanced Filter & Sort Bar (Category + Min/Max Price + Sort)
            EnhancedFilterBar(
                categories = categories,
                selectedCategoryId = filter.categoryId,
                onCategorySelected = onCategorySelected,
                minPrice = filter.minHarga,
                maxPrice = filter.maxHarga,
                onPriceRangeChanged = onPriceRangeChanged,
                currentSort = filter.sortBy,
                onSortChanged = onSortChanged
            )

            // Total barang info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${items.size} barang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // List Items
            if (items.isEmpty()) {
                val hasActiveFilter = filter.searchQuery.isNotBlank() ||
                        filter.categoryId != null ||
                        filter.minHarga != null ||
                        filter.maxHarga != null

                EmptyStateView(
                    title = if (hasActiveFilter) "Barang Tidak Ditemukan" else "Belum Ada Data Barang",
                    message = if (hasActiveFilter) {
                        "Coba ubah kata kunci pencarian, rentang harga, atau filter kategori."
                    } else {
                        "Data barang di toko ini masih kosong."
                    },
                    actionLabel = if (hasActiveFilter) "Reset Filter" else null,
                    onActionClick = if (hasActiveFilter) {
                        {
                            onSearchQueryChange("")
                            onCategorySelected(null)
                            onPriceRangeChanged(null, null)
                        }
                    } else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onItemClick = { onItemClick(item) },
                            onHistoryClick = { onHistoryClick(item) }
                        )
                    }
                }
            }
        }
    }
}
