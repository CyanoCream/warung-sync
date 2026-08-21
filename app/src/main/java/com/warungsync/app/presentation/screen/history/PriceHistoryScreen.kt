package com.warungsync.app.presentation.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.PriceHistory
import com.warungsync.app.domain.model.formatUnitQuantity
import com.warungsync.app.presentation.components.EmptyStateView
import com.warungsync.app.presentation.theme.Emerald40
import com.warungsync.app.presentation.theme.EmeraldBgLight
import com.warungsync.app.presentation.theme.PriceGreen
import com.warungsync.app.presentation.theme.PriceRed
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceHistoryScreen(
    item: Item?,
    histories: List<PriceHistory>,
    onBackClick: () -> Unit
) {
    val rupiahFormatter = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Perubahan Harga", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Item Summary Header Card
            item?.let { currentItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldBgLight
                        ) {
                            Text(
                                text = currentItem.categoryName ?: "Kategori",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Emerald40
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentItem.namaBarang,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Harga Sekarang: ${rupiahFormatter.format(currentItem.harga)} / ${formatUnitQuantity(currentItem.unitQuantity, currentItem.satuan)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald40
                        )
                    }
                }
            }

            // Timeline List
            if (histories.isEmpty()) {
                EmptyStateView(
                    title = "Belum ada riwayat",
                    message = "Riwayat akan tercatat otomatis setiap ada perubahan harga atau satuan.",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(histories, key = { _, h -> h.id }) { index, history ->
                        val isInitial = history.hargaLama == 0.0 && index == histories.lastIndex
                        val isPriceUp = history.hargaBaru > history.hargaLama && !isInitial
                        val isPriceDown = history.hargaBaru < history.hargaLama && !isInitial

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
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Indicator Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isInitial -> EmeraldBgLight
                                                isPriceUp -> PriceRed.copy(alpha = 0.15f)
                                                isPriceDown -> PriceGreen.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isInitial -> Icons.Default.History
                                            isPriceUp -> Icons.Default.ArrowUpward
                                            isPriceDown -> Icons.Default.ArrowDownward
                                            else -> Icons.Default.History
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = when {
                                            isInitial -> Emerald40
                                            isPriceUp -> PriceRed
                                            isPriceDown -> PriceGreen
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    if (isInitial) {
                                        Text(
                                            text = "Data Pertama Dibuat",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${rupiahFormatter.format(history.hargaBaru)} / ${history.satuanBaru}",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Emerald40
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = rupiahFormatter.format(history.hargaLama),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "  ➔  ",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = rupiahFormatter.format(history.hargaBaru),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPriceUp) PriceRed else PriceGreen
                                            )
                                        }
                                        if (history.satuanLama != history.satuanBaru) {
                                            Text(
                                                text = "Satuan: ${history.satuanLama} ➔ ${history.satuanBaru}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dateFormatter.format(Date(history.changedAt)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        if (history.changedByDevice.isNotBlank() && !history.changedByDevice.contains("-") && history.changedByDevice.length <= 20) {
                                            Text(
                                                text = " • ${history.changedByDevice}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
    }
}
