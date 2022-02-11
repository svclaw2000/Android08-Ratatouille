package com.kdjj.ratatouille.di.domain

import com.kdjj.domain.di.FlowUseCaseModule
import com.kdjj.domain.di.ResultUseCaseModule
import com.kdjj.domain.di.UseCaseModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module(includes = [ResultUseCaseModule::class, FlowUseCaseModule::class, UseCaseModule::class])
@InstallIn(SingletonComponent::class)
class DomainLayerModule
