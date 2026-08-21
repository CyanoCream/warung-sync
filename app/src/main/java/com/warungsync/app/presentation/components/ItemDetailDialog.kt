package com.warungsync.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.domain.model.formatUnitQuantity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailDialog(
    item: Item,
    members: List<TokoMember>,
    showUpdater: Boolean = false,
    onDismiss: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val updaterName = remember(item.updatedByDevice, members) {
        members.firstOrNull { it.deviceId == item.updatedByDevice }
            ?.deviceName
            ?.takeIf { it.isNotBlank() }
            ?: "Perangkat toko"
    }
    val updatedAt = remember(item.updatedAt) { formatHumanUpdateTime(item.updatedAt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.namaBarang, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val categoryNames = item.categoryNames.ifEmpty {
                    listOfNotNull(item.categoryName)
                }
                if (categoryNames.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryNames.forEachIndexed { index, categoryName ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(
                                    item.categoryColorsArgb.getOrNull(index)
                                        ?: item.categoryColorArgb
                                )
                            ) {
                                Text(
                                    text = categoryName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Rp %,d / %s".format(
                        item.harga.toLong(),
                        formatUnitQuantity(item.unitQuantity, item.satuan)
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (!item.deskripsi.isNullOrBlank()) {
                    Text(item.deskripsi, style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Terakhir diperbarui",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (showUpdater) {
                            "$updatedAt • oleh $updaterName"
                        } else {
                            updatedAt
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onHistoryClick) { Text("Riwayat harga") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

private fun formatHumanUpdateTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Waktu tidak diketahui"

    val locale = Locale("id", "ID")
    val now = Calendar.getInstance()
    val updated = Calendar.getInstance().apply { timeInMillis = timestamp }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dateLabel = when {
        now.isSameDay(updated) -> "Hari ini"
        yesterday.isSameDay(updated) -> "Kemarin"
        else -> SimpleDateFormat("d MMMM yyyy", locale).format(Date(timestamp))
    }
    val timeLabel = SimpleDateFormat("HH.mm z", locale).format(Date(timestamp))
    return "$dateLabel, pukul $timeLabel"
}

private fun Calendar.isSameDay(other: Calendar): Boolean =
    get(Calendar.ERA) == other.get(Calendar.ERA) &&
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
