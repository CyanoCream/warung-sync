package com.warungsync.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.warungsync.app.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

data class ItemWithCategoryName(
    val id: String,
    val tokoId: String,
    val namaBarang: String,
    val deskripsi: String?,
    val harga: Double,
    val satuan: String,
    val unitQuantity: Double,
    val categoryId: String,
    val categoryIdsCsv: String,
    val categoryName: String?,
    val categoryColorArgb: Int,
    val updatedAt: Long,
    val updatedByDevice: String,
    val isDeleted: Boolean
)

@Dao
interface ItemDao {

    @Query("""
        SELECT i.id, i.tokoId, i.namaBarang, i.deskripsi, i.harga, i.satuan, i.unitQuantity,
               i.categoryId, i.categoryIdsCsv, c.namaKategori AS categoryName,
               COALESCE(c.colorArgb, -11581723) AS categoryColorArgb,
               i.updatedAt, i.updatedByDevice, i.isDeleted
        FROM items i
        LEFT JOIN categories c ON i.categoryId = c.id
        WHERE i.tokoId = :tokoId
          AND i.isDeleted = 0
          AND (
            :searchQuery = '' 
            OR LOWER(i.namaBarang) LIKE '%' || LOWER(:searchQuery) || '%'
            OR LOWER(COALESCE(i.deskripsi, '')) LIKE '%' || LOWER(:searchQuery) || '%'
          )
          AND (
            :categoryId IS NULL
            OR i.categoryId = :categoryId
            OR (',' || i.categoryIdsCsv || ',') LIKE ('%,' || :categoryId || ',%')
          )
          AND (:minHarga IS NULL OR i.harga >= :minHarga)
          AND (:maxHarga IS NULL OR i.harga <= :maxHarga)
        ORDER BY 
          CASE WHEN :sortBy = 'DATE_DESC' THEN i.updatedAt END DESC,
          CASE WHEN :sortBy = 'DATE_ASC' THEN i.updatedAt END ASC,
          CASE WHEN :sortBy = 'NAME_ASC' THEN LOWER(i.namaBarang) END ASC,
          CASE WHEN :sortBy = 'NAME_DESC' THEN LOWER(i.namaBarang) END DESC,
          CASE WHEN :sortBy = 'PRICE_ASC' THEN i.harga END ASC,
          CASE WHEN :sortBy = 'PRICE_DESC' THEN i.harga END DESC
    """)
    fun getFilteredItems(
        tokoId: String,
        searchQuery: String,
        categoryId: String?,
        minHarga: Double?,
        maxHarga: Double?,
        sortBy: String
    ): Flow<List<ItemWithCategoryName>>

    @Query("""
        SELECT i.id, i.tokoId, i.namaBarang, i.deskripsi, i.harga, i.satuan, i.unitQuantity,
               i.categoryId, i.categoryIdsCsv, c.namaKategori AS categoryName,
               COALESCE(c.colorArgb, -11581723) AS categoryColorArgb,
               i.updatedAt, i.updatedByDevice, i.isDeleted
        FROM items i
        LEFT JOIN categories c ON i.categoryId = c.id
        WHERE i.id = :id AND i.isDeleted = 0
    """)
    suspend fun getItemById(id: String): ItemWithCategoryName?

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getRawItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE tokoId = :tokoId AND LOWER(namaBarang) = LOWER(:nama) LIMIT 1")
    suspend fun getRawItemByName(tokoId: String, nama: String): ItemEntity?

    @Query("SELECT * FROM items WHERE tokoId = :tokoId AND updatedAt > :since")
    suspend fun getItemsModifiedSince(tokoId: String, since: Long): List<ItemEntity>

    @Query("SELECT * FROM items WHERE updatedAt > :since")
    suspend fun getAllItemsModifiedSinceGlobal(since: Long): List<ItemEntity>

    @Query("""
        SELECT COUNT(*) FROM items
        WHERE isDeleted = 0
          AND (
            categoryId = :categoryId
            OR (',' || categoryIdsCsv || ',') LIKE ('%,' || :categoryId || ',%')
          )
    """)
    suspend fun countItemsByCategoryId(categoryId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ItemEntity>)

    @Query("UPDATE items SET isDeleted = 1, updatedAt = :timestamp, updatedByDevice = :deviceId WHERE id = :id")
    suspend fun softDeleteItem(id: String, deviceId: String, timestamp: Long = System.currentTimeMillis())
}
