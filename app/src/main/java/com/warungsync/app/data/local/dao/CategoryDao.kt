package com.warungsync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.warungsync.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE tokoId = :tokoId AND isDeleted = 0 ORDER BY namaKategori ASC")
    fun getAllCategories(tokoId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE tokoId = :tokoId AND isDeleted = 0 ORDER BY namaKategori ASC")
    suspend fun getAllCategoriesList(tokoId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id AND isDeleted = 0")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE tokoId = :tokoId AND LOWER(namaKategori) = LOWER(:nama) AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryByName(tokoId: String, nama: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE tokoId = :tokoId AND updatedAt > :since")
    suspend fun getCategoriesModifiedSince(tokoId: String, since: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE updatedAt > :since")
    suspend fun getAllCategoriesModifiedSinceGlobal(since: Long): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET isDeleted = 1, updatedAt = :timestamp, updatedByDevice = :deviceId WHERE id = :id")
    suspend fun softDeleteCategory(id: String, deviceId: String, timestamp: Long = System.currentTimeMillis())
}
