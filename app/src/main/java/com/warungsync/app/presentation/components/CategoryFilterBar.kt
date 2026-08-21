package com.warungsync.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.SortBy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnhancedFilterBar(
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
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var minInput by remember(minPrice) { mutableStateOf(minPrice?.toInt()?.toString() ?: "") }
    var maxInput by remember(maxPrice) { mutableStateOf(maxPrice?.toInt()?.toString() ?: "") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Baris 1: Kategori horizontal scroll + Tombol Filter & Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text("Semua") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = {
                            if (selectedCategoryId == category.id) {
                                onCategorySelected(null)
                            } else {
                                onCategorySelected(category.id)
                            }
                        },
                        label = { Text(category.namaKategori) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Dropdown button
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { sortMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentSort.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    SortBy.entries.forEach { sortBy ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sortBy.label,
                                    fontWeight = if (sortBy == currentSort) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                onSortChanged(sortBy)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                Icon(
                    imageVector = if (showAdvancedFilters) Icons.Default.ArrowDropUp else Icons.Default.FilterList,
                    contentDescription = "Toggle Price Filter",
                    tint = if (minPrice != null || maxPrice != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Baris 2: Advanced Price Range Filter (Collapsible)
        AnimatedVisibility(visible = showAdvancedFilters) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Rentang Harga (Rp)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minInput,
                            onValueChange = {
                                minInput = it.filter { char -> char.isDigit() }
                                onPriceRangeChanged(minInput.toDoubleOrNull(), maxInput.toDoubleOrNull())
                            },
                            label = { Text("Min") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Text("—", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = maxInput,
                            onValueChange = {
                                maxInput = it.filter { char -> char.isDigit() }
                                onPriceRangeChanged(minInput.toDoubleOrNull(), maxInput.toDoubleOrNull())
                            },
                            label = { Text("Max") },
                            placeholder = { Text("Tak terhingga") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        if (minInput.isNotBlank() || maxInput.isNotBlank()) {
                            IconButton(onClick = {
                                minInput = ""
                                maxInput = ""
                                onPriceRangeChanged(null, null)
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear price filter")
                            }
                        }
                    }
                }
            }
        }
    }
}
