package com.warungsync.app.domain.usecase.item

import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemTrendData
import com.warungsync.app.domain.model.PriceHistory
import com.warungsync.app.domain.model.PricePoint
import com.warungsync.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GetItemPriceTrendUseCase(private val itemRepository: ItemRepository) {

    operator fun invoke(
        tokoId: String,
        item: Item,
        startTime: Long,
        endTime: Long
    ): Flow<ItemTrendData> {
        val rangeDays = (endTime - startTime) / (1000 * 60 * 60 * 24)
        val isLongRange = rangeDays > 60 // jika lebih dari 2 bulan, gunakan agregasi akhir bulan

        val dailyFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
        val monthlyFormat = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))

        return itemRepository.getPriceHistoryBetween(tokoId, item.id, startTime, endTime)
            .map { historyList ->
                val points: List<PricePoint> = if (isLongRange) {
                    aggregateMonthlyPoints(historyList, item.harga, startTime, endTime, monthlyFormat)
                } else {
                    aggregateDailyPoints(historyList, item.harga, startTime, endTime, dailyFormat)
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

    private fun aggregateDailyPoints(
        historyList: List<PriceHistory>,
        currentPrice: Double,
        startTime: Long,
        endTime: Long,
        format: SimpleDateFormat
    ): List<PricePoint> {
        val points = mutableListOf<PricePoint>()

        if (historyList.isEmpty()) {
            points.add(PricePoint(startTime, format.format(Date(startTime)), currentPrice))
            points.add(PricePoint(endTime, format.format(Date(endTime)), currentPrice))
        } else {
            val first = historyList.first()
            val initialPrice = if (first.hargaLama > 0) first.hargaLama else first.hargaBaru

            points.add(PricePoint(startTime, format.format(Date(startTime)), initialPrice))

            for (h in historyList) {
                points.add(PricePoint(h.changedAt, format.format(Date(h.changedAt)), h.hargaBaru))
            }

            val last = points.last()
            if (last.timestamp < endTime) {
                points.add(PricePoint(endTime, format.format(Date(endTime)), currentPrice))
            }
        }
        return points
    }

    private fun aggregateMonthlyPoints(
        historyList: List<PriceHistory>,
        currentPrice: Double,
        startTime: Long,
        endTime: Long,
        format: SimpleDateFormat
    ): List<PricePoint> {
        val points = mutableListOf<PricePoint>()

        val cal = Calendar.getInstance()
        cal.timeInMillis = startTime
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var runningPrice = if (historyList.isNotEmpty()) {
            val f = historyList.first()
            if (f.hargaLama > 0) f.hargaLama else f.hargaBaru
        } else {
            currentPrice
        }

        while (cal.timeInMillis <= endTime) {
            // Dapatkan akhir bulan
            val monthLabel = format.format(cal.time)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            val monthEndTimestamp = cal.timeInMillis.coerceAtMost(endTime)

            // Cari harga terakhir di bulan ini
            val changesInMonth = historyList.filter { it.changedAt <= monthEndTimestamp }
            if (changesInMonth.isNotEmpty()) {
                runningPrice = changesInMonth.last().hargaBaru
            }

            points.add(
                PricePoint(
                    timestamp = monthEndTimestamp,
                    dateLabel = monthLabel,
                    price = runningPrice
                )
            )

            // Maju ke awal bulan berikutnya
            cal.add(Calendar.MONTH, 1)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
        }

        if (points.isEmpty()) {
            points.add(PricePoint(startTime, format.format(Date(startTime)), currentPrice))
            points.add(PricePoint(endTime, format.format(Date(endTime)), currentPrice))
        }

        return points
    }
}
