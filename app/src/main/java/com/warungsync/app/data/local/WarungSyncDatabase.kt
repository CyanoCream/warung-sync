package com.warungsync.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.warungsync.app.data.local.dao.CategoryDao
import com.warungsync.app.data.local.dao.ItemDao
import com.warungsync.app.data.local.dao.PriceHistoryDao
import com.warungsync.app.data.local.dao.TokoDao
import com.warungsync.app.data.local.dao.TokoMemberDao
import com.warungsync.app.data.local.entity.CategoryEntity
import com.warungsync.app.data.local.entity.ItemEntity
import com.warungsync.app.data.local.entity.PriceHistoryEntity
import com.warungsync.app.data.local.entity.TokoEntity
import com.warungsync.app.data.local.entity.TokoMemberEntity

@Database(
    entities = [
        TokoEntity::class,
        TokoMemberEntity::class,
        CategoryEntity::class,
        ItemEntity::class,
        PriceHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WarungSyncDatabase : RoomDatabase() {
    abstract fun tokoDao(): TokoDao
    abstract fun tokoMemberDao(): TokoMemberDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun priceHistoryDao(): PriceHistoryDao
}
