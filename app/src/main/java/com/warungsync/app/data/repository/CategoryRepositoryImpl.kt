package com.warungsync.app.data.repository

import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.dao.CategoryDao
import com.warungsync.app.data.local.dao.ItemDao
import com.warungsync.app.data.local.entity.CategoryEntity
import com.warungsync.app.data.mapper.toDomain
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val prefs: DevicePreferences
) : CategoryRepository {

    override fun getAllCategories(tokoId: String): Flow<List<Category>> {
        return categoryDao.getAllCategories(tokoId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(id)?.toDomain()
    }

    override suspend fun addCategory(tokoId: String, namaKategori: String, colorArgb: Int): Result<Category> {
        val trimmedName = namaKategori.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama kategori tidak boleh kosong"))
        }

        val existing = categoryDao.getCategoryByName(tokoId, trimmedName)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Kategori '$trimmedName' sudah ada di toko ini"))
        }

        val now = System.currentTimeMillis()
        val entity = CategoryEntity(
            id = UUID.randomUUID().toString(),
            tokoId = tokoId,
            namaKategori = trimmedName,
            colorArgb = colorArgb,
            updatedAt = now,
            updatedByDevice = prefs.deviceId,
            isDeleted = false
        )
        categoryDao.upsertCategory(entity)
        return Result.success(entity.toDomain())
    }

    override suspend fun updateCategory(tokoId: String, id: String, namaKategori: String, colorArgb: Int): Result<Category> {
        val trimmedName = namaKategori.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama kategori tidak boleh kosong"))
        }

        val current = categoryDao.getCategoryById(id)
            ?: return Result.failure(NoSuchElementException("Kategori tidak ditemukan"))

        val duplicate = categoryDao.getCategoryByName(tokoId, trimmedName)
        if (duplicate != null && duplicate.id != id) {
            return Result.failure(IllegalArgumentException("Kategori '$trimmedName' sudah ada di toko ini"))
        }

        val updated = current.copy(
            namaKategori = trimmedName,
            colorArgb = colorArgb,
            updatedAt = System.currentTimeMillis(),
            updatedByDevice = prefs.deviceId
        )
        categoryDao.upsertCategory(updated)
        return Result.success(updated.toDomain())
    }

    override suspend fun deleteCategory(tokoId: String, id: String): Result<Unit> {
        val itemCount = itemDao.countItemsByCategoryId(categoryId = id)
        if (itemCount > 0) {
            return Result.failure(
                IllegalStateException("Tidak dapat menghapus kategori karena masih ada $itemCount barang terkait")
            )
        }

        categoryDao.softDeleteCategory(id, prefs.deviceId)
        return Result.success(Unit)
    }
}
