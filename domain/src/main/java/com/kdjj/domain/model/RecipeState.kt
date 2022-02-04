package com.kdjj.domain.model

enum class RecipeState {
    CREATE,     // 작성중인 새 레시피
    LOCAL,      // 업로드하지 않은 내 레시피
    UPLOAD,     // 업로드된 내 레시피
    NETWORK,    // 다운로드하지 않은 남의 레시피
    DOWNLOAD    // 남의 레시피 다운로드
}
