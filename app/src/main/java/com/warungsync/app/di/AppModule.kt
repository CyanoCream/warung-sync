package com.warungsync.app.di

import androidx.room.Room
import com.warungsync.app.auth.TotpManager
import com.warungsync.app.data.local.DevicePreferences
import com.warungsync.app.data.local.WarungSyncDatabase
import com.warungsync.app.data.repository.CategoryRepositoryImpl
import com.warungsync.app.data.repository.ItemRepositoryImpl
import com.warungsync.app.data.repository.SyncRepositoryImpl
import com.warungsync.app.data.repository.TokoRepositoryImpl
import com.warungsync.app.domain.repository.CategoryRepository
import com.warungsync.app.domain.repository.ItemRepository
import com.warungsync.app.domain.repository.SyncRepository
import com.warungsync.app.domain.repository.TokoRepository
import com.warungsync.app.domain.usecase.category.AddCategoryUseCase
import com.warungsync.app.domain.usecase.category.DeleteCategoryUseCase
import com.warungsync.app.domain.usecase.category.GetAllCategoriesUseCase
import com.warungsync.app.domain.usecase.category.UpdateCategoryUseCase
import com.warungsync.app.domain.usecase.item.AddItemUseCase
import com.warungsync.app.domain.usecase.item.DeleteItemUseCase
import com.warungsync.app.domain.usecase.item.GetFilteredItemsUseCase
import com.warungsync.app.domain.usecase.item.GetItemPriceTrendUseCase
import com.warungsync.app.domain.usecase.item.GetPriceHistoryUseCase
import com.warungsync.app.domain.usecase.item.UpdateItemUseCase
import com.warungsync.app.domain.usecase.sync.SyncWithPeerUseCase
import com.warungsync.app.domain.usecase.toko.CreateTokoUseCase
import com.warungsync.app.domain.usecase.toko.DeleteTokoUseCase
import com.warungsync.app.domain.usecase.toko.GetMyTokosUseCase
import com.warungsync.app.domain.usecase.toko.GetTokoMembersUseCase
import com.warungsync.app.domain.usecase.toko.KickMemberUseCase
import com.warungsync.app.domain.usecase.toko.LeaveTokoUseCase
import com.warungsync.app.domain.usecase.toko.UpdateMemberRoleUseCase
import com.warungsync.app.domain.usecase.toko.UpdateNamaTokoUseCase
import com.warungsync.app.network.discovery.NsdDiscoveryManager
import com.warungsync.app.network.sync.SyncClient
import com.warungsync.app.network.sync.SyncOrchestrator
import com.warungsync.app.network.sync.SyncServer
import com.warungsync.app.presentation.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Preferences & Auth
    single { DevicePreferences(androidContext()) }
    single { TotpManager() }

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            WarungSyncDatabase::class.java,
            "warungsync_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    single { get<WarungSyncDatabase>().tokoDao() }
    single { get<WarungSyncDatabase>().tokoMemberDao() }
    single { get<WarungSyncDatabase>().categoryDao() }
    single { get<WarungSyncDatabase>().itemDao() }
    single { get<WarungSyncDatabase>().priceHistoryDao() }

    // Repositories
    single<TokoRepository> { TokoRepositoryImpl(get(), get(), get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get(), get(), get()) }
    single<ItemRepository> { ItemRepositoryImpl(get(), get(), get(), get()) }
    single<SyncRepository> { SyncRepositoryImpl(get(), get(), get(), get(), get(), get()) }

    // Use Cases Toko
    factory { GetMyTokosUseCase(get()) }
    factory { CreateTokoUseCase(get(), get(), get()) }
    factory { UpdateNamaTokoUseCase(get()) }
    factory { DeleteTokoUseCase(get()) }
    factory { LeaveTokoUseCase(get()) }
    factory { GetTokoMembersUseCase(get()) }
    factory { UpdateMemberRoleUseCase(get()) }
    factory { KickMemberUseCase(get()) }

    // Use Cases Item & Category & Sync & Trend
    factory { GetFilteredItemsUseCase(get()) }
    factory { GetItemPriceTrendUseCase(get()) }
    factory { AddItemUseCase(get(), get()) }
    factory { UpdateItemUseCase(get(), get()) }
    factory { DeleteItemUseCase(get(), get()) }
    factory { GetPriceHistoryUseCase(get()) }
    factory { GetAllCategoriesUseCase(get()) }
    factory { AddCategoryUseCase(get(), get()) }
    factory { UpdateCategoryUseCase(get(), get()) }
    factory { DeleteCategoryUseCase(get(), get()) }
    factory { SyncWithPeerUseCase(get(), get()) }

    // Network & Sync Components
    single { NsdDiscoveryManager(androidContext()) }
    single { SyncServer(get(), get(), get(), get()) }
    single { SyncClient(get(), get()) }
    single { SyncOrchestrator(androidContext(), get(), get(), get(), get()) }

    // ViewModel
    viewModel {
        MainViewModel(
            getMyTokosUseCase = get(),
            createTokoUseCase = get(),
            updateNamaTokoUseCase = get(),
            deleteTokoUseCase = get(),
            leaveTokoUseCase = get(),
            getTokoMembersUseCase = get(),
            updateMemberRoleUseCase = get(),
            kickMemberUseCase = get(),
            getFilteredItemsUseCase = get(),
            getItemPriceTrendUseCase = get(),
            addItemUseCase = get(),
            updateItemUseCase = get(),
            deleteItemUseCase = get(),
            getPriceHistoryUseCase = get(),
            getAllCategoriesUseCase = get(),
            addCategoryUseCase = get(),
            updateCategoryUseCase = get(),
            deleteCategoryUseCase = get(),
            syncOrchestrator = get(),
            nsdManager = get(),
            syncClient = get(),
            prefs = get()
        )
    }
}
