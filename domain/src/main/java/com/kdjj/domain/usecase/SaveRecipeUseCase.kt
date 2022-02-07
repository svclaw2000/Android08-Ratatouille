package com.kdjj.domain.usecase

import com.kdjj.domain.common.IdGenerator
import com.kdjj.domain.common.flatMap
import com.kdjj.domain.model.ImageInfo
import com.kdjj.domain.model.RecipeState
import com.kdjj.domain.model.request.SaveRecipeRequest
import com.kdjj.domain.repository.RecipeImageRepository
import com.kdjj.domain.repository.RecipeRepository
import javax.inject.Inject

internal class SaveRecipeUseCase @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val imageRepository: RecipeImageRepository,
    private val idGenerator: IdGenerator
) : ResultUseCase<SaveRecipeRequest, Unit> {

    override suspend fun invoke(request: SaveRecipeRequest): Result<Unit> {
        val recipe = request.recipe
        val imgPathList = listOf(recipe.imgPath) + recipe.stepList.map { step -> step.imgPath }

        val imgInfoList = imgPathList.filter { path ->
            path.isNotBlank()
        }.map { path ->
            ImageInfo(path, idGenerator.generateId())
        }

        return copyImageToInternal(imgInfoList).flatMap { changedImgPathList ->
            var i = 0
            val totalImgList = imgPathList.map { path ->
                if (path.isEmpty()) path else changedImgPathList[i++]
            }

            val stepList = recipe.stepList.mapIndexed { idx, step ->
                step.copy(
                    stepId = if (step.stepId.isBlank() || recipe.state == RecipeState.NETWORK) idGenerator.generateId() else step.stepId,
                    imgPath = totalImgList[idx + 1]
                )
            }

            when (recipe.state) {
                RecipeState.CREATE -> recipeRepository.saveMyRecipe(
                    recipe.copy(
                        recipeId = idGenerator.generateId(),
                        imgPath = totalImgList.first(),
                        stepList = stepList,
                        createTime = System.currentTimeMillis(),
                        state = RecipeState.LOCAL
                    )
                )
                RecipeState.LOCAL, RecipeState.UPLOAD, RecipeState.DOWNLOAD -> {
                    recipeRepository.getLocalRecipe(request.recipe.recipeId)
                        .flatMap { originRecipe ->
                            recipeRepository.updateMyRecipe(
                                recipe.copy(
                                    imgPath = totalImgList.first(),
                                    stepList = stepList,
                                    createTime = System.currentTimeMillis()
                                ),
                                originRecipe.stepList.map { it.imgPath } + originRecipe.imgPath
                            )
                        }
                }
                RecipeState.NETWORK -> recipeRepository.saveMyRecipe(
                    recipe.copy(
                        recipeId = idGenerator.generateId(),
                        imgPath = totalImgList.first(),
                        stepList = stepList,
                        createTime = System.currentTimeMillis(),
                        state = RecipeState.DOWNLOAD
                    )
                )
            }
        }
    }

    private suspend fun copyImageToInternal(imgInfoList: List<ImageInfo>): Result<List<String>> =
        when {
            imgInfoList.isEmpty() ->
                Result.success(listOf())
            imgInfoList.first().uri.startsWith("https://") || imgInfoList.first().uri.startsWith("gs://") ->
                imageRepository.copyRemoteImageToInternal(imgInfoList)
            else ->
                imageRepository.copyExternalImageToInternal(imgInfoList)
        }
}