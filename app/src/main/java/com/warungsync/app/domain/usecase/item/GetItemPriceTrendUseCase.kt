package com.warungsync.app.domain.usecase.item

import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemTrendData
import com.warungsync.app.domain.model.PricePoint
import com.warungsync.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GetItemPriceTrendUseCase(private val itemRepository: ItemRepository) {

    operator fun invoke(
        tokoId: String,
        item: Item,
        startTime: Long,
        endTime: Long
    ): Flow<ItemTrendData> {
        val dateFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))

        return itemRepository.getPriceHistoryBetween(tokoId, item.id, startTime, endTime)
            .map { historyList ->
                val points = mutableListOf<PricePoint>()

                if (historyList.isEmpty()) {
                    // Jika belum ada perubahan di rentang ini, buat 2 titik baseline dengan harga saat ini
                    points.add(
                        PricePoint(
                            timestamp = startTime,
                            dateLabel = dateFormat.format(Date(startTime)),
                            price = item.harga
                        )
                    )
                    points.add(
                        PricePoint(
                            timestamp = endTime,
                            dateLabel = dateFormat.format(Date(endTime)),
                            price = item.harga
                        )
                    )
                } else {
                    // Titik awal
                    val firstHistory = historyList.first()
                    points.add(
                        PricePoint(
                            timestamp = firstHistory.changedAt,
                            dateLabel = dateFormat.format(Date(firstHistory.changedAt)),
                            price = if (firstHistory.hargaLama > 0) firstHistory.hargaLama else firstHistory.hargaBaru
                        )
                    )

                    // Titik-titik pergerakan
                    for (h in historyList) {
                        points.add(
                            PricePoint(
                                timestamp = h.changedAt,
                                dateLabel = dateFormat.format(Date(h.changedAt)),
                                price = h.hargaBaru
                            )
                        )
                    }

                    // Titik penutup jika perubahan terakhir bukan hari ini
                    val lastPoint = points.last()
                    if (lastPoint.timestamp < endTime) {
                        points.add(
                            PricePoint(
                                timestamp = endTime,
                                dateLabel = dateFormat.format(Date(endTime)),
                                price = item.harga
                            )
                        )
                    }
                }

                val initial = points.firstOrNull()?.price ?: item.harga
                val current = item.harga
                val diff = current - initial
                val percent = if (initial > 0) (diff / initial) * 100.0 else 0.0

                ItemTrendData(
                    item = item,
                    initialPrice = initial,
                    currentPrice = current,
                    priceChangeAmount = diff,
                    priceChangePercent = percent,
                    points = points
                )
            }
    }
}
