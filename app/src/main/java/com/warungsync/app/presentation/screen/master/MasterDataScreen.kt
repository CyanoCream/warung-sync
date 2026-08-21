package com.warungsync.app.presentation.screen.master

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.warungsync.app.presentation.screen.additem.AddEditItemSheet
import com.warungsync.app.presentation.screen.tokolist.RoleBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MasterDataScreen(
    currentToko: Toko,
    items: List<Item>,
    categories: List<Category>,
    filter: ItemFilter,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    onSortChanged: (SortBy) -> Unit,
    onAddItem: (nama: String, deskripsi: String?, harga: Double, satuan: String, categoryId: String) -> Unit,
    onUpdateItem: (id: String, nama: String, deskripsi: String?, harga: Double, satuan: String, categoryId: String) -> Unit,
    onDeleteItem: (id: String) -> Unit,
    onAddCategory: (String) -> Unit,
    onUpdateCategory: (id: String, String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    onHistoryClick: (Item) -> Unit,
    onBackToTokoList: () -> Unit
) {
    val canEdit = currentToko.myRole == MemberRole.OWNER || currentToko.myRole == MemberRole.ADMIN

    // Jika role USER: Tampilkan Access Restricted Gate
    if (!canEdit) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Master Data") },
                    navigationIcon = {
                        IconButton(onClick = onBackToTokoList) {
                            Icon(Icons.Default.Store, contentDescription = "Ganti Toko")
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Akses Terbatas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Role Anda saat ini adalah USER (hanya lihat). Anda tidak dapat menambah, mengedit, atau menghapus data barang/kategori.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minta pemilik toko (${currentToko.ownerDeviceName}) untuk menaikkan role Anda menjadi ADMIN jika ingin mengelola data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    var showAddItemSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kelola Data: ${currentToko.namaToko}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            RoleBadge(role = currentToko.myRole)
                        }
                        Text(
                            text = "Tambah, edit, dan hapus master data",
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
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddItemSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Tambah Barang") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAddCategoryDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Tambah Kategori") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Master Barang (${items.size})") },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Master Kategori (${categories.size})") },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> MasterItemsTab(
                        items = items,
                        categories = categories,
                        filter = filter,
                        onSearchQueryChange = onSearchQueryChange,
                        onCategorySelected = onCategorySelected,
                        onPriceRangeChanged = onPriceRangeChanged,
                        onSortChanged = onSortChanged,
                        onEditItem = { itemToEdit = it },
                        onDeleteItem = { itemToDelete = it },
                        onHistoryClick = onHistoryClick
                    )
                    1 -> MasterCategoriesTab(
                        categories = categories,
                        onEditCategory = { categoryToEdit = it },
                        onDeleteCategory = { categoryToDelete = it }
                    )
                }
            }
        }
    }

    // Add / Edit Item Sheet
    if (showAddItemSheet || itemToEdit != null) {
        AddEditItemSheet(
            categories = categories,
            editingItem = itemToEdit,
            onDismiss = {
                showAddItemSheet = false
                itemToEdit = null
            },
            onSave = { nama, deskripsi, harga, satuan, categoryId ->
                if (itemToEdit != null) {
                    onUpdateItem(itemToEdit!!.id, nama, deskripsi, harga, satuan, categoryId)
                } else {
                    onAddItem(nama, deskripsi, harga, satuan, categoryId)
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Item
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Barang?") },
            text = { Text("Apakah Anda yakin ingin menghapus '${item.namaBarang}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteItem(item.id)
                        itemToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Tambah/Edit Kategori
    if (showAddCategoryDialog || categoryToEdit != null) {
        var inputName by remember(categoryToEdit) { mutableStateOf(categoryToEdit?.namaKategori ?: "") }
        var catError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                categoryToEdit = null
            },
            title = { Text(if (categoryToEdit != null) "Edit Kategori" else "Tambah Kategori") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            inputName = it
                            catError = null
                        },
                        label = { Text("Nama Kategori") },
                        placeholder = { Text("Contoh: Sembako") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    catError?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isBlank()) {
                            catError = "Nama kategori tidak boleh kosong"
                        } else {
                            if (categoryToEdit != null) {
                                onUpdateCategory(categoryToEdit!!.id, inputName.trim())
                            } else {
                                onAddCategory(inputName.trim())
                            }
                            showAddCategoryDialog = false
                            categoryToEdit = null
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategoryDialog = false
                        categoryToEdit = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Konfirmasi Hapus Kategori
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Hapus Kategori?") },
            text = { Text("Hapus kategori '${category.namaKategori}'? Kategori hanya bisa dihapus jika tidak ada barang terkait.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(category.id)
                        categoryToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MasterItemsTab(
    items: List<Item>,
    categories: List<Category>,
    filter: ItemFilter,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    onSortChanged: (SortBy) -> Unit,
    onEditItem: (Item) -> Unit,
    onDeleteItem: (Item) -> Unit,
    onHistoryClick: (Item) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter
        RealtimeSearchBar(
            query = filter.searchQuery,
            onQueryChange = onSearchQueryChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

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

        if (items.isEmpty()) {
            EmptyStateView(
                title = "Tidak Ada Barang",
                message = "Klik tombol 'Tambah Barang' di bawah untuk menambahkan barang baru."
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
                        onItemClick = { onEditItem(item) },
                        onEditClick = { onEditItem(item) },
                        onDeleteClick = { onDeleteItem(item) },
                        onHistoryClick = { onHistoryClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun MasterCategoriesTab(
    categories: List<Category>,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    if (categories.isEmpty()) {
        EmptyStateView(
            title = "Belum Ada Kategori",
            message = "Tambahkan kategori terlebih dahulu sebelum menambah barang."
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.namaKategori,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            IconButton(onClick = { onEditCategory(category) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Kategori",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteCategory(category) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Kategori",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
