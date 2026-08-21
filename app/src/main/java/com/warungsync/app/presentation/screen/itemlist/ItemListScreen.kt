package com.warungsync.app.presentation.screen.itemlist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.SortBy
import com.warungsync.app.domain.model.SyncStatus
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.presentation.components.EmptyStateView
import com.warungsync.app.presentation.components.ItemCard
import com.warungsync.app.presentation.components.ItemDetailDialog
import com.warungsync.app.presentation.components.SearchAndFilterRow
import com.warungsync.app.presentation.components.SearchBar
import com.warungsync.app.presentation.components.SpotlightSearch
import com.warungsync.app.presentation.components.spotlightPullGesture
import com.warungsync.app.presentation.components.wheelScrollMotion
import com.warungsync.app.presentation.screen.tokolist.RoleBadge
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    currentToko: Toko,
    items: List<Item>,
    categories: List<Category>,
    filter: ItemFilter,
    syncStatus: SyncStatus = SyncStatus(),
    isSyncing: Boolean,
    lastSyncMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onPriceRangeChanged: (Double?, Double?) -> Unit,
    onSortChanged: (SortBy) -> Unit,
    onSyncClick: () -> Unit,
    onBackToTokoList: () -> Unit,
    onManageTokoClick: () -> Unit,
    onHistoryClick: (Item) -> Unit,
    members: List<TokoMember> = emptyList(),
    showHeader: Boolean = true,
    searchOnly: Boolean = false,
    wheelAnimationEnabled: Boolean = true
) {
    var selectedItemForDetail by remember { mutableStateOf<Item?>(null) }
    var showSpotlightSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusFilteredResults = wheelAnimationEnabled && (
        filter.searchQuery.isNotBlank() ||
            filter.categoryId != null ||
            filter.minHarga != null ||
            filter.maxHarga != null
        )

    LaunchedEffect(focusFilteredResults, items.firstOrNull()?.id) {
        if (focusFilteredResults && items.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }
    val maxElasticOffset = with(LocalDensity.current) { 72.dp.toPx() }
    var elasticOffset by remember { mutableFloatStateOf(0f) }
    val elasticScrollConnection = remember(maxElasticOffset) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isReturning = (elasticOffset > 0f && available.y < 0f) ||
                    (elasticOffset < 0f && available.y > 0f)
                if (!isReturning || source != NestedScrollSource.UserInput) return Offset.Zero

                val consumedY = if (abs(available.y) >= abs(elasticOffset)) {
                    -elasticOffset
                } else {
                    available.y
                }
                elasticOffset += consumedY
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    elasticOffset = (elasticOffset + available.y * 0.45f)
                        .coerceIn(-maxElasticOffset, maxElasticOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (elasticOffset != 0f) {
                    Animatable(elasticOffset).animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) {
                        elasticOffset = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            if (showHeader) TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentToko.namaToko,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        RoleBadge(role = currentToko.myRole)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToTokoList) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = "Pilih Toko",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    // Tombol Sync: icon berputar mulus saat syncing
                    IconButton(onClick = onSyncClick) {
                        SyncIcon(isSyncing)
                    }

                    if (currentToko.myRole == MemberRole.OWNER) {
                        IconButton(onClick = onManageTokoClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan Toko",
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
            // Search Bar sejajar dengan Filter Popup Modal
            if (searchOnly) {
                if (!wheelAnimationEnabled) {
                    SearchBar(
                        query = filter.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
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
            }

            // List Items
            if (items.isEmpty()) {
                val hasActiveFilter = filter.searchQuery.isNotBlank() ||
                        filter.categoryId != null ||
                        filter.minHarga != null ||
                        filter.maxHarga != null

                if (hasActiveFilter) {
                    EmptyStateView(
                        actionLabel = "Reset Filter",
                        onActionClick = {
                            onSearchQueryChange("")
                            onCategorySelected(null)
                            onPriceRangeChanged(null, null)
                        }
                    )
                } else {
                    EmptyStateView()
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val focusedEdgePadding = maxOf(24.dp, maxHeight / 2 - 56.dp)

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (wheelAnimationEnabled) {
                                    Modifier
                                        .spotlightPullGesture(
                                            listState = listState,
                                            enabled = !showSpotlightSearch,
                                            onOpen = { showSpotlightSearch = true }
                                        )
                                        .nestedScroll(elasticScrollConnection)
                                        .graphicsLayer {
                                            val stretchFraction = if (size.height > 0) {
                                                abs(elasticOffset) / size.height
                                            } else {
                                                0f
                                            }
                                            translationY = elasticOffset * 0.75f
                                            scaleY = 1f + stretchFraction * 0.35f
                                            transformOrigin = TransformOrigin(
                                                pivotFractionX = 0.5f,
                                                pivotFractionY = if (elasticOffset >= 0f) 0f else 1f
                                            )
                                        }
                                } else {
                                    Modifier
                                }
                            ),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = if (wheelAnimationEnabled) focusedEdgePadding else 14.dp,
                            end = 16.dp,
                            bottom = if (wheelAnimationEnabled) focusedEdgePadding else 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = items,
                            key = { it.id },
                            contentType = { "product" }
                        ) { item ->
                            ItemCard(
                                item = item,
                                onItemClick = { selectedItemForDetail = item },
                                modifier = Modifier.wheelScrollMotion(
                                    listState = listState,
                                    itemKey = item.id,
                                    enabled = wheelAnimationEnabled
                                ),
                                onEditClick = null,
                                onHistoryClick = { onHistoryClick(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedItemForDetail?.let { item ->
        ItemDetailDialog(
            item = item,
            members = members,
            onDismiss = { selectedItemForDetail = null },
            onHistoryClick = {
                selectedItemForDetail = null
                onHistoryClick(item)
            }
        )
    }

    if (showSpotlightSearch) {
        SpotlightSearch(
            query = filter.searchQuery,
            onQueryChange = onSearchQueryChange,
            onDismiss = { showSpotlightSearch = false }
        )
    }
}

@Composable
private fun SyncIcon(isSyncing: Boolean) {
    // Do not keep an infinite animation clock alive while the app is idle.
    if (!isSyncing) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "Sync",
            tint = MaterialTheme.colorScheme.onPrimary
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "sync_rotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )
    Icon(
        imageVector = Icons.Default.Sync,
        contentDescription = "Sedang sinkronisasi",
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.rotate(angle)
    )
}
