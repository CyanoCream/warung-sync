package com.warungsync.app.domain.usecase.toko

import com.warungsync.app.auth.TotpManager
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.domain.repository.TokoRepository
import kotlinx.coroutines.flow.Flow

class GetMyTokosUseCase(private val tokoRepository: TokoRepository) {
    operator fun invoke(): Flow<List<Toko>> = tokoRepository.getMyTokos()
}

class CreateTokoUseCase(
    private val tokoRepository: TokoRepository,
    private val prefs: DevicePreferences,
    private val totpManager: TotpManager
) {
    suspend operator fun invoke(namaToko: String, totpCode: String? = null): Result<Toko> {
        // Cek apakah ini toko ke-2 dst
        val createdCount = prefs.createdTokoCount
        if (createdCount >= 1) {
            if (totpCode.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Pembuatan toko ke-2 dst membutuhkan kode OTP lisensi"))
            }
            val isValid = totpManager.verifyMasterCode(totpCode)
            if (!isValid) {
                return Result.failure(IllegalArgumentException("Kode OTP lisensi tidak valid atau kadaluarsa"))
            }
        }
        return tokoRepository.createToko(namaToko)
    }
}

class UpdateNamaTokoUseCase(private val tokoRepository: TokoRepository) {
    suspend operator fun invoke(tokoId: String, namaToko: String): Result<Unit> =
        tokoRepository.updateNamaToko(tokoId, namaToko)
}

class DeleteTokoUseCase(private val tokoRepository: TokoRepository) {
    suspend operator fun invoke(tokoId: String): Result<Unit> =
        tokoRepository.deleteToko(tokoId)
}

class LeaveTokoUseCase(private val tokoRepository: TokoRepository) {
    suspend operator fun invoke(tokoId: String): Result<Unit> =
        tokoRepository.leaveToko(tokoId)
}

class GetTokoMembersUseCase(private val tokoRepository: TokoRepository) {
    operator fun invoke(tokoId: String): Flow<List<TokoMember>> =
        tokoRepository.getMembersForToko(tokoId)
}

class UpdateMemberRoleUseCase(private val tokoRepository: TokoRepository) {
    suspend operator fun invoke(tokoId: String, memberDeviceId: String, newRole: MemberRole): Result<Unit> =
        tokoRepository.updateMemberRole(tokoId, memberDeviceId, newRole)
}

class KickMemberUseCase(private val tokoRepository: TokoRepository) {
    suspend operator fun invoke(tokoId: String, memberDeviceId: String): Result<Unit> =
        tokoRepository.kickMember(tokoId, memberDeviceId)
}
