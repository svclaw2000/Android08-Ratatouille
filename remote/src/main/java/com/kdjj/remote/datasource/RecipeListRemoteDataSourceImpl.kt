package com.kdjj.remote.datasource

import com.kdjj.data.datasource.RecipeListRemoteDataSource
import com.kdjj.domain.model.Recipe
import com.kdjj.remote.dao.RecipeListDao
import javax.inject.Inject

class RecipeListRemoteDataSourceImpl @Inject constructor(
    private val recipeListDao: RecipeListDao,
) : RecipeListRemoteDataSource {
    override suspend fun fetchLatestRecipeList(lastVisibleCreateTime: Long): Result<List<Recipe>> =
        try {
            val recipeList = recipeListDao.fetchLatestRecipeList(lastVisibleCreateTime)
            Result.success(recipeList)
        } catch (e: Exception) {
            Result.failure(e)
        }
}