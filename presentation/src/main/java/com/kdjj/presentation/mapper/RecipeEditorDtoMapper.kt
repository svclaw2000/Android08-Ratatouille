package com.kdjj.presentation.mapper

import com.kdjj.domain.model.*
import com.kdjj.domain.model.request.ValidateRecipeFlowRequest
import com.kdjj.domain.model.request.ValidateRecipeStepFlowRequest
import com.kdjj.domain.model.response.ValidateRecipeFlowResponse
import com.kdjj.domain.model.response.ValidateRecipeStepFlowResponse
import com.kdjj.domain.usecase.UseCase
import com.kdjj.presentation.common.calculateSeconds
import com.kdjj.presentation.model.RecipeEditorDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

internal fun Recipe.toEditorDto(
    recipeTypeList: List<RecipeType>,
    validateRecipeUseCase: UseCase<ValidateRecipeFlowRequest, ValidateRecipeFlowResponse>,
    validateStepUseCase: UseCase<ValidateRecipeStepFlowRequest, ValidateRecipeStepFlowResponse>,
    scope: CoroutineScope
): Pair<RecipeEditorDto.RecipeMetaDto, List<RecipeEditorDto.RecipeStepDto>> {
    val titleFlow = MutableStateFlow(title)
    val stuffFlow = MutableStateFlow(stuff)

    val response = validateRecipeUseCase(ValidateRecipeFlowRequest(
        titleFlow = titleFlow,
        stuffFlow = stuffFlow
    ))

    return RecipeEditorDto.RecipeMetaDto(
        titleFlow = titleFlow,
        typeIntFlow = MutableStateFlow(recipeTypeList.indexOfFirst { it.id == type.id }),
        stuffFlow = stuffFlow,
        imgPathFlow = MutableStateFlow(imgPath),

        titleValidFlow = response.titleFlow.stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = false
        ),
        stuffValidFlow = response.stuffFlow.stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = false
        ),

        recipeId = recipeId,
        authorId = authorId,
        state = state
    ) to stepList.map { it.toEditorDto(validateStepUseCase, scope) }
}

internal fun RecipeStep.toEditorDto(
    validateStepUseCase: UseCase<ValidateRecipeStepFlowRequest, ValidateRecipeStepFlowResponse>,
    scope: CoroutineScope
): RecipeEditorDto.RecipeStepDto {
    val nameFlow = MutableStateFlow(name)
    val descriptionFlow = MutableStateFlow(description)

    val response = validateStepUseCase(ValidateRecipeStepFlowRequest(
        nameFlow = nameFlow,
        descriptionFlow = descriptionFlow
    ))

    return RecipeEditorDto.RecipeStepDto(
        nameFlow = nameFlow,
        imgPathFlow = MutableStateFlow(imgPath),
        typeIntFlow = MutableStateFlow(type.ordinal),
        descriptionFlow = descriptionFlow,
        minutesFlow = MutableStateFlow(seconds / 60),
        secondsFlow = MutableStateFlow(seconds % 60),

        nameValidFlow = response.nameFlow.stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = false
        ),
        descriptionValidFlow = response.descriptionFlow.stateIn(
            scope = scope,
            started = SharingStarted.Lazily,
            initialValue = false
        ),

        stepId = stepId
    )
}

internal fun RecipeEditorDto.RecipeMetaDto.toDomain(
    stepDtoList: List<RecipeEditorDto.RecipeStepDto>,
    recipeTypeList: List<RecipeType>
) = Recipe(
    recipeId = recipeId,
    title = titleFlow.value,
    type = recipeTypeList[typeIntFlow.value],
    stuff = stuffFlow.value,
    imgPath = imgPathFlow.value,
    stepList = stepDtoList.map { it.toDomain() },
    authorId = authorId,
    viewCount = viewCount,
    isFavorite = isFavorite,
    createTime = 0,
    state = state
)

internal fun RecipeEditorDto.RecipeStepDto.toDomain() = RecipeStep(
    stepId = stepId,
    name = nameFlow.value,
    type = RecipeStepType.values()[typeIntFlow.value],
    description = descriptionFlow.value,
    imgPath = imgPathFlow.value,
    seconds = calculateSeconds(minutesFlow.value, secondsFlow.value)
)