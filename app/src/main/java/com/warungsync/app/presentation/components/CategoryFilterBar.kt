package com.warungsync.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.SortBy
import com.warungsync.app.presentation.util.formatThousandsInput
import com.warungsync.app.presentation.util.parseThousandsInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search Bar sejajar dengan Tombol Filter Modal Popup.
 * Tidak memakan banyak ruang, rapi, dan modern.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchAndFilterRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    minPrice: Double?,
    maxPrice: Double?,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    currentSort: SortBy,
    onSortChanged: (SortBy) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val hasActiveFilter = selectedCategoryId != null || minPrice != null || maxPrice != null || currentSort != SortBy.DATE_DESC

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search field di sebelah kiri
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Cari...",
            modifier = Modifier.weight(1f)
        )

        // Tombol Filter di sebelah kanan search
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (hasActiveFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier
                .size(48.dp)
                .clickable { showFilterSheet = true }
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filter",
                    tint = if (hasActiveFilter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // A plain dialog opens much more cheaply than a full-screen animated sheet.
    if (showFilterSheet) {
        var tempMinInput by remember(minPrice) { mutableStateOf(minPrice?.toLong()?.toString()?.let(::formatThousandsInput) ?: "") }
        var tempMaxInput by remember(maxPrice) { mutableStateOf(maxPrice?.toLong()?.toString()?.let(::formatThousandsInput) ?: "") }
        var tempCatId by remember(selectedCategoryId) { mutableStateOf(selectedCategoryId) }
        var tempSort by remember(currentSort) { mutableStateOf(currentSort) }

        Dialog(onDismissRequest = { showFilterSheet = false }) {
            Surface(
                modifier = Modifier.widthIn(max = 360.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Urutkan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (tempCatId != null || tempMinInput.isNotBlank() || tempMaxInput.isNotBlank() || tempSort != SortBy.DATE_DESC) {
                        TextButton(
                            onClick = {
                                tempCatId = null
                                tempMinInput = ""
                                tempMaxInput = ""
                                tempSort = SortBy.DATE_DESC
                            }
                        ) {
                            Text("Reset", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Urutkan (Sort)
                Text(
                    text = "Urutkan",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SortBy.entries.forEach { sortBy ->
                        FilterChip(
                            selected = tempSort == sortBy,
                            onClick = { tempSort = sortBy },
                            label = { Text(sortBy.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kategori
                if (categories.isNotEmpty()) {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        FilterChip(
                            selected = tempCatId == null,
                            onClick = { tempCatId = null },
                            label = { Text("Semua", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(32.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = tempCatId == cat.id,
                                onClick = { tempCatId = if (tempCatId == cat.id) null else cat.id },
                                label = { Text(cat.namaKategori, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(32.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Rentang Harga
                Text(
                    text = "Rentang Harga (Rp)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tempMinInput,
                        onValueChange = { tempMinInput = formatThousandsInput(it) },
                        label = { Text("Min") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text("—")
                    OutlinedTextField(
                        value = tempMaxInput,
                        onValueChange = { tempMaxInput = formatThousandsInput(it) },
                        label = { Text("Max") },
                        placeholder = { Text("Tak terhingga") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tombol Terapkan
                Button(
                    onClick = {
                        onCategorySelected(tempCatId)
                        onSortChanged(tempSort)
                        onPriceRangeChanged(parseThousandsInput(tempMinInput), parseThousandsInput(tempMaxInput))
                        showFilterSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Terapkan Filter", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
            }
        }
    }
}

/** Right-edge swipe drawer used by the read-only product list. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductFilterDrawer(
    categories: List<Category>,
    selectedCategoryId: String?,
    minPrice: Double?,
    maxPrice: Double?,
    currentSort: SortBy,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    onSortChanged: (SortBy) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tempCategory by remember(selectedCategoryId) { mutableStateOf(selectedCategoryId) }
    var tempMin by remember(minPrice) {
        mutableStateOf(minPrice?.toLong()?.toString()?.let(::formatThousandsInput) ?: "")
    }
    var tempMax by remember(maxPrice) {
        mutableStateOf(maxPrice?.toLong()?.toString()?.let(::formatThousandsInput) ?: "")
    }
    var tempSort by remember(currentSort) { mutableStateOf(currentSort) }

    LaunchedEffect(tempMin, tempMax) {
        delay(250)
        onPriceRangeChanged(parseThousandsInput(tempMin), parseThousandsInput(tempMax))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // When closed, use the edge-only detector below so this nested
            // drawer cannot steal the left drawer's gesture.
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Filter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                TextButton(onClick = {
                                    tempCategory = null
                                    tempMin = ""
                                    tempMax = ""
                                    tempSort = SortBy.DATE_DESC
                                    onCategorySelected(null)
                                    onPriceRangeChanged(null, null)
                                    onSortChanged(SortBy.DATE_DESC)
                                }) { Text("Reset") }
                            }

                            Text("Urutkan", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                SortBy.entries.forEach { sort ->
                                    FilterChip(
                                        selected = tempSort == sort,
                                        onClick = {
                                            tempSort = sort
                                            onSortChanged(sort)
                                        },
                                        label = { Text(sort.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(32.dp)
                                    )
                                }
                            }

                            if (categories.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text("Kategori", style = MaterialTheme.typography.labelMedium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    FilterChip(
                                        selected = tempCategory == null,
                                        onClick = {
                                            tempCategory = null
                                            onCategorySelected(null)
                                        },
                                        label = { Text("Semua", style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.height(32.dp)
                                    )
                                    categories.forEach { category ->
                                        FilterChip(
                                            selected = tempCategory == category.id,
                                            onClick = {
                                                tempCategory = category.id
                                                onCategorySelected(category.id)
                                            },
                                            label = { Text(category.namaKategori, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text("Rentang harga", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = tempMin,
                                    onValueChange = { tempMin = formatThousandsInput(it) },
                                    label = { Text("Minimum") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = tempMax,
                                    onValueChange = { tempMax = formatThousandsInput(it) },
                                    label = { Text("Maksimum") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Filter diterapkan otomatis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(drawerState) {
                            val edgeWidth = 32.dp.toPx()
                            val openThreshold = 56.dp.toPx()
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                if (down.position.x < size.width - edgeWidth) {
                                    return@awaitEachGesture
                                }

                                var horizontalDrag = 0f
                                var pointerPressed = true
                                while (pointerPressed) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    horizontalDrag += change.positionChange().x
                                    pointerPressed = change.pressed
                                    if (horizontalDrag <= -openThreshold) {
                                        change.consume()
                                        scope.launch { drawerState.open() }
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    content()
                }
            }
        }
    }
}

