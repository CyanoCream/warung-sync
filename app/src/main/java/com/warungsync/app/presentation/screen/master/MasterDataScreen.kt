package com.warungsync.app.presentation.screen.master

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.SortBy
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.formatUnitQuantity
import com.warungsync.app.presentation.components.EmptyStateView
import com.warungsync.app.presentation.components.ItemCard
import com.warungsync.app.presentation.components.SearchAndFilterRow
import com.warungsync.app.presentation.screen.additem.AddEditItemSheet
import com.warungsync.app.presentation.screen.tokolist.RoleBadge
import com.warungsync.app.presentation.theme.CategoryColorPalette
import com.warungsync.app.domain.model.DEFAULT_CATEGORY_COLOR_ARGB
import kotlinx.coroutines.launch

enum class MasterDataSection { BOTH, ITEMS, CATEGORIES }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    onAddItem: (nama: String, deskripsi: String?, harga: Double, unitQuantity: Double, satuan: String, categoryId: String) -> Unit,
    onUpdateItem: (id: String, nama: String, deskripsi: String?, harga: Double, unitQuantity: Double, satuan: String, categoryId: String) -> Unit,
    onDeleteItem: (id: String) -> Unit,
    onAddCategory: (String, Int) -> Unit,
    onUpdateCategory: (id: String, String, Int) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    onHistoryClick: (Item) -> Unit,
    onBackToTokoList: () -> Unit,
    section: MasterDataSection = MasterDataSection.BOTH,
    showHeader: Boolean = true
) {
    val canEdit = currentToko.myRole == MemberRole.OWNER || currentToko.myRole == MemberRole.ADMIN

    // Jika role USER: Tampilkan Access Restricted Gate
    if (!canEdit) {
        Scaffold(
            topBar = {
                if (showHeader) TopAppBar(
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
    val showingItems = section == MasterDataSection.ITEMS ||
        (section == MasterDataSection.BOTH && pagerState.currentPage == 0)

    var showAddItemSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            if (showHeader) TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kelola Data",
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
                    IconButton(
                        onClick = {
                            if (showingItems) {
                                if (categories.isEmpty()) {
                                    showAddCategoryDialog = true
                                } else {
                                    showAddItemSheet = true
                                }
                            } else {
                                showAddCategoryDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Data",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (showingItems) {
                        if (categories.isEmpty()) {
                            showAddCategoryDialog = true
                        } else {
                            showAddItemSheet = true
                        }
                    } else {
                        showAddCategoryDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (section) {
                MasterDataSection.ITEMS -> MasterItemsTab(
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
                MasterDataSection.CATEGORIES -> MasterCategoriesTab(
                    categories = categories,
                    onEditCategory = { categoryToEdit = it },
                    onDeleteCategory = { categoryToDelete = it }
                )
                MasterDataSection.BOTH -> {
                    TabRow(selectedTabIndex = pagerState.currentPage) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            text = { Text("Barang (${items.size})") }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            text = { Text("Kategori (${categories.size})") }
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
            onSave = { nama, deskripsi, harga, unitQuantity, satuan, categoryId ->
                if (itemToEdit != null) {
                    onUpdateItem(itemToEdit!!.id, nama, deskripsi, harga, unitQuantity, satuan, categoryId)
                } else {
                    onAddItem(nama, deskripsi, harga, unitQuantity, satuan, categoryId)
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
        var selectedColor by remember(categoryToEdit) {
            mutableIntStateOf(categoryToEdit?.colorArgb ?: DEFAULT_CATEGORY_COLOR_ARGB)
        }
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
                    Spacer(Modifier.height(12.dp))
                    Text("Warna label", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryColorPalette.forEach { argb ->
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { selectedColor = argb },
                                shape = CircleShape,
                                color = Color(argb),
                                border = if (selectedColor == argb) {
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                                } else null
                            ) {}
                        }
                    }
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
                                onUpdateCategory(categoryToEdit!!.id, inputName.trim(), selectedColor)
                            } else {
                                onAddCategory(inputName.trim(), selectedColor)
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
        val relatedItemCount = items.count { it.categoryId == category.id }
        val categoryIsUsed = relatedItemCount > 0
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = {
                if (categoryIsUsed) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            },
            title = {
                Text(if (categoryIsUsed) "Kategori Masih Digunakan" else "Hapus Kategori?")
            },
            text = {
                Text(
                    if (categoryIsUsed) {
                        "Kategori '${category.namaKategori}' tidak dapat dihapus karena masih digunakan oleh " +
                            "$relatedItemCount produk. Pindahkan atau hapus produk tersebut terlebih dahulu."
                    } else {
                        "Hapus kategori '${category.namaKategori}'?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!categoryIsUsed) onDeleteCategory(category.id)
                        categoryToDelete = null
                    }
                ) {
                    Text(
                        text = if (categoryIsUsed) "Mengerti" else "Hapus",
                        color = if (categoryIsUsed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            },
            dismissButton = {
                if (!categoryIsUsed) {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("Batal")
                    }
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
        // Search & Filter sejajar dengan modal popup
        SearchAndFilterRow(
            searchQuery = filter.searchQuery,
            onSearchQueryChange = onSearchQueryChange,
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
            EmptyStateView()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.namaBarang,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                item.categoryName?.let { cat ->
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Rp %,d per %s".format(
                                        item.harga.toLong(),
                                        formatUnitQuantity(item.unitQuantity, item.satuan)
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEditItem(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDeleteItem(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
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
}

@Composable
fun MasterCategoriesTab(
    categories: List<Category>,
    onEditCategory: (Category) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    if (categories.isEmpty()) {
        EmptyStateView()
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(category.colorArgb)
                        ) {
                            Text(
                                text = category.namaKategori,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

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
