package com.warungsync.app.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.SortBy
import com.warungsync.app.presentation.util.formatThousandsInput
import com.warungsync.app.presentation.util.parseThousandsInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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
    val scope = rememberCoroutineScope()
    val drawerProgress = remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val drawerVisible by remember {
        derivedStateOf { drawerProgress.floatValue > 0.001f }
    }

    fun settleDrawer(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = drawerProgress.floatValue,
                targetValue = target,
                animationSpec = tween(220)
            ) { value, _ ->
                drawerProgress.floatValue = value
            }
        }
    }
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

    BackHandler(enabled = drawerVisible) {
        settleDrawer(0f)
    }

    val density = LocalDensity.current
    val maxDrawerWidthPx = with(density) { 300.dp.toPx() }
    val edgeWidthPx = with(density) { 32.dp.toPx() }
    val flingVelocityPx = with(density) { 500.dp.toPx() }
    val touchSlop = LocalViewConfiguration.current.touchSlop

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(maxDrawerWidthPx, edgeWidthPx, touchSlop) {
                val gestureDrawerWidthPx = minOf(maxDrawerWidthPx, size.width.toFloat())
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startedOpen = drawerProgress.floatValue > 0.001f
                    if (!startedOpen && down.position.x < size.width - edgeWidthPx) {
                        return@awaitEachGesture
                    }

                    settleJob?.cancel()
                    val velocityTracker = VelocityTracker().apply {
                        addPosition(down.uptimeMillis, down.position)
                    }
                    var lastPosition = down.position
                    var totalX = 0f
                    var totalY = 0f
                    var dragging = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                            ?: break
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        val delta = change.position - lastPosition
                        lastPosition = change.position
                        totalX += delta.x
                        totalY += delta.y

                        if (!dragging) {
                            if (kotlin.math.abs(totalY) > touchSlop &&
                                kotlin.math.abs(totalY) > kotlin.math.abs(totalX)
                            ) {
                                break
                            }
                            if (kotlin.math.abs(totalX) > touchSlop &&
                                kotlin.math.abs(totalX) > kotlin.math.abs(totalY)
                            ) {
                                if (!startedOpen && totalX > 0f) break
                                dragging = true
                            }
                        }

                        if (dragging) {
                            change.consume()
                            drawerProgress.floatValue =
                                (drawerProgress.floatValue - delta.x / gestureDrawerWidthPx)
                                    .coerceIn(0f, 1f)
                        }

                        if (!change.pressed) break
                    }

                    if (dragging) {
                        val velocityX = velocityTracker.calculateVelocity().x
                        val target = when {
                            velocityX <= -flingVelocityPx -> 1f
                            velocityX >= flingVelocityPx -> 0f
                            drawerProgress.floatValue >= 0.5f -> 1f
                            else -> 0f
                        }
                        settleDrawer(target)
                    }
                }
            }
    ) {
        val drawerWidth = minOf(300.dp, maxWidth)
        val drawerWidthPx = with(density) { drawerWidth.toPx() }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }

        if (drawerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = drawerProgress.floatValue * 0.32f
                    }
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable {
                        settleDrawer(0f)
                    }
            )
        }

        ModalDrawerSheet(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(drawerWidth)
                .graphicsLayer {
                    translationX = drawerWidthPx * (1f - drawerProgress.floatValue)
                }
        ) {
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

