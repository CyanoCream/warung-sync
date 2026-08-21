package com.warungsync.app.domain.repository

import com.warungsync.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(tokoId: String): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun addCategory(tokoId: String, namaKategori: String, colorArgb: Int): Result<Category>
    suspend fun updateCategory(tokoId: String, id: String, namaKategori: String, colorArgb: Int): Result<Category>
    suspend fun deleteCategory(tokoId: String, id: String): Result<Unit>
}
