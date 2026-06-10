package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.shared.MotivationPhraseManager
import io.mockative.Mockable
import kotlinx.coroutines.flow.Flow

@Mockable
class GetMotivationPhraseUseCase(
    private val motivationPhraseManager: MotivationPhraseManager
) {
    operator fun invoke(): Flow<Map<String, String>> = motivationPhraseManager.phrase
}
