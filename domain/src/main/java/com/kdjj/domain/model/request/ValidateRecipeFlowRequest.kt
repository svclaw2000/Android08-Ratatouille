package com.kdjj.domain.model.request

import kotlinx.coroutines.flow.Flow

data class ValidateRecipeFlowRequest(
    val titleFlow: Flow<String>,
    val stuffFlow: Flow<String>
) : Request