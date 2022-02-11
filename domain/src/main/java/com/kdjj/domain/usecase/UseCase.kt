package com.kdjj.domain.usecase

import com.kdjj.domain.model.request.Request

interface UseCase<R: Request, T> {
    operator fun invoke(request: R): T
}
