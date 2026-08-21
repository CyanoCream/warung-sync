package com.warungsync.app.domain.usecase.category

import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.repository.CategoryRepository
import com.warungsync.app.domain.repository.TokoRepository
import kotlinx.coroutines.flow.Flow

class GetAllCategoriesUseCase(private val categoryRepository: CategoryRepository) {
    operator fun invoke(tokoId: String): Flow<List<Category>> =
        categoryRepository.getAllCategories(tokoId)
}

class AddCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val tokoRepository: TokoRepository
) {
    suspend operator fun invoke(tokoId: String, namaKategori: String): Result<Category> {
        val role = tokoRepository.getMyRole(tokoId)
        if (role != MemberRole.OWNER && role != MemberRole.ADMIN) {
            return Result.failure(IllegalStateException("Hanya Pemilik Toko atau Admin yang dapat menambah kategori"))
        }
        return categoryRepository.addCategory(tokoId, namaKategori)
    }
}

class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val tokoRepository: TokoRepository
) {
    suspend operator fun invoke(tokoId: String, id: String, namaKategori: String): Result<Category> {
        val role = tokoRepository.getMyRole(tokoId)
        if (role != MemberRole.OWNER && role != MemberRole.ADMIN) {
            return Result.failure(IllegalStateException("Hanya Pemilik Toko atau Admin yang dapat mengedit kategori"))
        }
        return categoryRepository.updateCategory(tokoId, id, namaKategori)
    }
}

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val tokoRepository: TokoRepository
) {
    suspend operator fun invoke(tokoId: String, id: String): Result<Unit> {
        val role = tokoRepository.getMyRole(tokoId)
        if (role != MemberRole.OWNER && role != MemberRole.ADMIN) {
            return Result.failure(IllegalStateException("Hanya Pemilik Toko atau Admin yang dapat menghapus kategori"))
        }
        return categoryRepository.deleteCategory(tokoId, id)
    }
}
