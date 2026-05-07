package com.programovil.aura.di

import org.koin.core.module.Module
import com.programovil.aura.regionsync.di.regionSyncModule

actual fun getRegionSyncModule(): Module = regionSyncModule
