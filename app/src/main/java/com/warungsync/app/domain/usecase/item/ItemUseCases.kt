package com.warungsync.app.domain.usecase.item

import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.ItemFilter
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.PriceHistory
import com.warungsync.app.domain.repository.ItemRepository
import com.warungsync.app.domain.repository.TokoRepository
import kotlinx.coroutines.flow.Flow

class GetFilteredItemsUseCase(private val itemRepository: ItemRepository) {
    operator fun invoke(tokoId: String, filter: ItemFilter): Flow<List<Item>> =
        itemRepository.getFilteredItems(tokoId, filter)
}

class AddItemUseCase(
    private val itemRepository: ItemRepository,
    private val tokoRepository: TokoRepository
) {
    suspend operator fun invoke(
        tokoId: String,
        namaBarang: String,
        deskripsi: String?,
        harga: Double,
        satuan: String,
        categoryId: String
    ): Result<Item> {
        val role = tokoRepository.getMyRole(tokoId)
        if (role == MemberRole.USER) {
            return Result.failure(IllegalStateException("User biasa tidak memiliki izin untuk menambah barang"))
        }
        return itemRepository.addItem(tokoId, namaBarang, deskripsi, harga, satuan, categoryId)
    }
}

class UpdateItemUseCase(
    private val itemRepository: ItemRepository,
    private val tokoRepository: TokoRepository
) {
    suspend operator fun invoke(
        tokoId: String,
        id: String,
        namaBarang: String,
        deskripsi: String?,
        harga: Double,
        satuan: String,
        categoryId: String
    ): Result<Item> {
        val role = tokoRepository.getMyRole(tokoId)
        if (role == MemberRole.USER) {
            return Result.failure(IllegalStateException("User biasa tidak memiliki izin untuk mengedit barang"))
        }
        return itemRepository.updateItem(tokoId, id, namaBarang, deskripsi, harga, satuan, categoryId)
    }
}

class DeleteItemUseCase(
    private val itemRepository: ItemRepository,
    private val tokoRepository: TokoRepository
) {
    suspend operator fun invoke(tokoId: String, id: String): Result<Unit> {
        val role = tokoRepository.getMyRole(tokoId)
        if (role == MemberRole.USER) {
            return Result.failure(IllegalStateException("User biasa tidak memiliki izin untuk menghapus barang"))
        }
        return itemRepository.deleteItem(id)
    }
}

class GetPriceHistoryUseCase(private val itemRepository: ItemRepository) {
    operator fun invoke(tokoId: String, itemId: String): Flow<List<PriceHistory>> =
        itemRepository.getPriceHistoryForItem(tokoId, itemId)
}
