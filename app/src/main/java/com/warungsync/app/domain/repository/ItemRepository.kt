package com.warungsync.app.domain.repository

import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.PriceHistory
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    fun getFilteredItems(tokoId: String, filter: ItemFilter): Flow<List<Item>>
    suspend fun getItemById(id: String): Item?
    suspend fun addItem(
        tokoId: String,
        namaBarang: String,
        deskripsi: String?,
        harga: Double,
        satuan: String,
        categoryId: String
    ): Result<Item>

    suspend fun updateItem(
        tokoId: String,
        id: String,
        namaBarang: String,
        deskripsi: String?,
        harga: Double,
        satuan: String,
        categoryId: String
    ): Result<Item>

    suspend fun deleteItem(id: String): Result<Unit>
    fun getPriceHistoryForItem(tokoId: String, itemId: String): Flow<List<PriceHistory>>
}
