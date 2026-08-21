package com.warungsync.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warungsync.app.domain.model.ItemTrendData
import com.warungsync.app.domain.model.PricePoint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PriceTrendCard(
    trendData: ItemTrendData,
    onRemoveChart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val rupiahFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    val isUp = trendData.priceChangeAmount > 0
    val isDown = trendData.priceChangeAmount < 0

    val trendColor = when {
        isUp -> Color(0xFFE53935) // Merah = harga naik (inflasi)
        isDown -> Color(0xFF43A047) // Hijau = harga turun (lebih murah)
        else -> MaterialTheme.colorScheme.primary
    }

    var selectedPoint by remember { mutableStateOf<PricePoint?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Nama Barang & Kategori + Tombol Hapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trendData.item.namaBarang,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    trendData.item.categoryNames
                        .ifEmpty { listOfNotNull(trendData.item.categoryName) }
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                        Text(
                            text = it.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge Persentase
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = trendColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isUp -> Icons.Default.TrendingUp
                                    isDown -> Icons.Default.TrendingDown
                                    else -> Icons.Default.TrendingFlat
                                },
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val sign = if (isUp) "+" else ""
                            Text(
                                text = "$sign${String.format(Locale.US, "%.1f", trendData.priceChangePercent)}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }

                    if (onRemoveChart != null) {
                        IconButton(
                            onClick = onRemoveChart,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Chart",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-header: Harga saat ini vs harga awal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Harga Sekarang",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rupiahFormat.format(trendData.currentPrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Harga Awal Periode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rupiahFormat.format(trendData.initialPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Info titik yang di-tap
            selectedPoint?.let { pt ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.extraSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tanggal: ${pt.dateLabel}", style = MaterialTheme.typography.labelSmall)
                        Text("Harga: ${rupiahFormat.format(pt.price)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart
            InteractiveLineChart(
                points = trendData.points,
                lineColor = trendColor,
                onPointSelected = { selectedPoint = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            // Sumbu X: Tanggal Awal & Akhir
            if (trendData.points.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = trendData.points.first().dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = trendData.points.last().dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveLineChart(
    points: List<PricePoint>,
    lineColor: Color,
    onPointSelected: (PricePoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val minPrice = points.minOfOrNull { it.price } ?: 0.0
    val maxPrice = points.maxOfOrNull { it.price } ?: 1.0
    val priceSpan = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { offset ->
                val width = size.width
                val stepX = width / if (points.size > 1) (points.size - 1) else 1
                val clickedIndex = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                onPointSelected(points[clickedIndex])
            }
        }
    ) {
        val width = size.width
        val height = size.height
        val paddingVertical = 16f
        val chartHeight = height - (paddingVertical * 2)

        val coords = points.mapIndexed { index, pt ->
            val x = if (points.size > 1) (index.toFloat() / (points.size - 1)) * width else width / 2f
            val yNormalized = ((pt.price - minPrice) / priceSpan).toFloat()
            val y = height - paddingVertical - (yNormalized * chartHeight)
            Offset(x, y)
        }

        // Draw Gradient Fill under line
        val fillPath = Path().apply {
            moveTo(0f, height)
            coords.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.02f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw Line
        val strokePath = Path().apply {
            coords.forEachIndexed { i, offset ->
                if (i == 0) moveTo(offset.x, offset.y)
                else lineTo(offset.x, offset.y)
            }
        }

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Circles on Points
        coords.forEach { offset ->
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = offset
            )
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = offset
            )
        }
    }
}
