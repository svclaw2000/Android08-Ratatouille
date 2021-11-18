package com.kdjj.local.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.kdjj.domain.model.Recipe

internal data class RecipeDto(
    @Embedded val recipeMeta: RecipeMetaDto,
    @Relation(
        parentColumn = "recipeTypeId",
        entityColumn = "recipeTypeId"
    )
    val recipeType: RecipeTypeDto,
    @Relation(
        parentColumn = "recipeMetaId",
        entityColumn = "parentRecipeId"
    )
    val steps: List<RecipeStepDto>
)

internal fun RecipeDto.toDomain(): Recipe =
    Recipe(
        recipeId = recipeMeta.recipeMetaId,
        title = recipeMeta.title,
        type = recipeType.toDomain(),
        stuff = recipeMeta.stuff,
        imgPath = recipeMeta.imgPath,
        stepList = steps.map { it.toDomain() },
        authorId = recipeMeta.authorId,
        viewCount = 0,
        isFavorite = recipeMeta.isFavorite,
        createTime = recipeMeta.createTime,
        state = recipeMeta.state
    )

