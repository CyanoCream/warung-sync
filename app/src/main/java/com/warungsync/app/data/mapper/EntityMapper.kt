package com.warungsync.app.data.mapper

import com.warungsync.app.data.local.dao.ItemWithCategoryName
import com.warungsync.app.data.local.dao.TokoWithRole
import com.warungsync.app.data.local.entity.CategoryEntity
import com.warungsync.app.data.local.entity.ItemEntity
import com.warungsync.app.data.local.entity.PriceHistoryEntity
import com.warungsync.app.data.local.entity.TokoEntity
import com.warungsync.app.data.local.entity.TokoMemberEntity
import com.warungsync.app.domain.model.Category
import com.warungsync.app.domain.model.Item
import com.warungsync.app.domain.model.MemberRole
import com.warungsync.app.domain.model.PriceHistory
import com.warungsync.app.domain.model.Toko
import com.warungsync.app.domain.model.TokoMember
import com.warungsync.app.network.dto.CategoryDto
import com.warungsync.app.network.dto.ItemDto
import com.warungsync.app.network.dto.PriceHistoryDto
import com.warungsync.app.network.dto.TokoDto
import com.warungsync.app.network.dto.TokoMemberDto

fun CategoryEntity.toDomain() = Category(
    id = id,
    tokoId = tokoId,
    namaKategori = namaKategori,
    colorArgb = colorArgb,
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    tokoId = tokoId,
    namaKategori = namaKategori,
    colorArgb = colorArgb,
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice
)

fun CategoryDto.toEntity() = CategoryEntity(
    id = id,
    tokoId = tokoId,
    namaKategori = namaKategori,
    colorArgb = colorArgb,
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice,
    isDeleted = isDeleted
)

fun CategoryEntity.toDto() = CategoryDto(
    id = id,
    tokoId = tokoId,
    namaKategori = namaKategori,
    colorArgb = colorArgb,
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice,
    isDeleted = isDeleted
)

fun ItemEntity.toDomain(categories: List<CategoryEntity> = emptyList()): Item {
    val ids = normalizedCategoryIds(categoryId, categoryIdsCsv)
    val categoryMap = categories.associateBy { it.id }
    val matchedCategories = ids.mapNotNull(categoryMap::get)
    return Item(
        id = id,
        tokoId = tokoId,
        namaBarang = namaBarang,
        deskripsi = deskripsi,
        harga = harga,
        satuan = satuan,
        unitQuantity = unitQuantity,
        categoryId = ids.first(),
        categoryName = matchedCategories.firstOrNull()?.namaKategori,
        categoryColorArgb = matchedCategories.firstOrNull()?.colorArgb ?: -11581723,
        categoryIds = ids,
        categoryNames = matchedCategories.map { it.namaKategori },
        categoryColorsArgb = matchedCategories.map { it.colorArgb },
        updatedAt = updatedAt,
        updatedByDevice = updatedByDevice
    )
}

fun ItemWithCategoryName.toDomain(categories: List<CategoryEntity> = emptyList()): Item {
    val ids = normalizedCategoryIds(categoryId, categoryIdsCsv)
    val categoryMap = categories.associateBy { it.id }
    val matchedCategories = ids.mapNotNull(categoryMap::get)
    val names = matchedCategories.map { it.namaKategori }
        .ifEmpty { listOfNotNull(categoryName) }
    val colors = matchedCategories.map { it.colorArgb }
        .ifEmpty { if (categoryName != null) listOf(categoryColorArgb) else emptyList() }
    return Item(
        id = id,
        tokoId = tokoId,
        namaBarang = namaBarang,
        deskripsi = deskripsi,
        harga = harga,
        satuan = satuan,
        unitQuantity = unitQuantity,
        categoryId = ids.first(),
        categoryName = names.firstOrNull(),
        categoryColorArgb = colors.firstOrNull() ?: categoryColorArgb,
        categoryIds = ids,
        categoryNames = names,
        categoryColorsArgb = colors,
        updatedAt = updatedAt,
        updatedByDevice = updatedByDevice
    )
}

fun Item.toEntity() = ItemEntity(
    id = id,
    tokoId = tokoId,
    namaBarang = namaBarang,
    deskripsi = deskripsi,
    harga = harga,
    satuan = satuan,
    unitQuantity = unitQuantity,
    categoryId = categoryId,
    categoryIdsCsv = normalizedCategoryIds(
        primaryId = categoryId,
        csv = categoryIds.joinToString(",")
    ).joinToString(","),
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice
)

fun ItemDto.toEntity() = ItemEntity(
    id = id,
    tokoId = tokoId,
    namaBarang = namaBarang,
    deskripsi = deskripsi,
    harga = harga,
    satuan = satuan,
    unitQuantity = unitQuantity,
    categoryId = categoryId,
    categoryIdsCsv = normalizedCategoryIds(
        primaryId = categoryId,
        csv = categoryIds.joinToString(",")
    ).joinToString(","),
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice,
    isDeleted = isDeleted
)

fun ItemEntity.toDto() = ItemDto(
    id = id,
    tokoId = tokoId,
    namaBarang = namaBarang,
    deskripsi = deskripsi,
    harga = harga,
    satuan = satuan,
    unitQuantity = unitQuantity,
    categoryId = categoryId,
    categoryIds = normalizedCategoryIds(categoryId, categoryIdsCsv),
    updatedAt = updatedAt,
    updatedByDevice = updatedByDevice,
    isDeleted = isDeleted
)

private fun normalizedCategoryIds(primaryId: String, csv: String): List<String> =
    csv.split(',')
        .map(String::trim)
        .filter(String::isNotBlank)
        .let { ids -> (listOf(primaryId) + ids).filter(String::isNotBlank).distinct() }

fun PriceHistoryEntity.toDomain() = PriceHistory(
    id = id,
    tokoId = tokoId,
    itemId = itemId,
    hargaLama = hargaLama,
    hargaBaru = hargaBaru,
    satuanLama = satuanLama,
    satuanBaru = satuanBaru,
    changedAt = changedAt,
    changedByDevice = changedByDevice
)

fun PriceHistory.toEntity() = PriceHistoryEntity(
    id = id,
    tokoId = tokoId,
    itemId = itemId,
    hargaLama = hargaLama,
    hargaBaru = hargaBaru,
    satuanLama = satuanLama,
    satuanBaru = satuanBaru,
    changedAt = changedAt,
    changedByDevice = changedByDevice
)

fun PriceHistoryDto.toEntity() = PriceHistoryEntity(
    id = id,
    tokoId = tokoId,
    itemId = itemId,
    hargaLama = hargaLama,
    hargaBaru = hargaBaru,
    satuanLama = satuanLama,
    satuanBaru = satuanBaru,
    changedAt = changedAt,
    changedByDevice = changedByDevice
)

fun PriceHistoryEntity.toDto() = PriceHistoryDto(
    id = id,
    tokoId = tokoId,
    itemId = itemId,
    hargaLama = hargaLama,
    hargaBaru = hargaBaru,
    satuanLama = satuanLama,
    satuanBaru = satuanBaru,
    changedAt = changedAt,
    changedByDevice = changedByDevice
)

fun TokoWithRole.toDomain() = Toko(
    id = id,
    namaToko = namaToko,
    ownerDeviceId = ownerDeviceId,
    ownerDeviceName = ownerDeviceName,
    myRole = MemberRole.fromString(myRole),
    memberCount = memberCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TokoEntity.toDto() = TokoDto(
    id = id,
    namaToko = namaToko,
    ownerDeviceId = ownerDeviceId,
    ownerDeviceName = ownerDeviceName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun TokoDto.toEntity() = TokoEntity(
    id = id,
    namaToko = namaToko,
    ownerDeviceId = ownerDeviceId,
    ownerDeviceName = ownerDeviceName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted
)

fun TokoMemberEntity.toDomain() = TokoMember(
    id = id,
    tokoId = tokoId,
    deviceId = deviceId,
    deviceName = deviceName,
    role = MemberRole.fromString(role),
    roleChangedAt = roleChangedAt,
    joinedAt = joinedAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun TokoMemberEntity.toDto() = TokoMemberDto(
    id = id,
    tokoId = tokoId,
    deviceId = deviceId,
    deviceName = deviceName,
    role = role,
    roleChangedAt = roleChangedAt,
    joinedAt = joinedAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun TokoMemberDto.toEntity() = TokoMemberEntity(
    id = id,
    tokoId = tokoId,
    deviceId = deviceId,
    deviceName = deviceName,
    role = role,
    roleChangedAt = roleChangedAt,
    joinedAt = joinedAt,
    updatedAt = updatedAt,
    isActive = isActive
)
