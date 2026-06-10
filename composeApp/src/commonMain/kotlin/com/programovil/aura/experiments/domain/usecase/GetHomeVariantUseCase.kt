package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.HomeVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetHomeVariantUseCase(
    private val getUserPlanUseCase: GetUserPlanUseCase,
    private val getMotivationPhraseUseCase: GetMotivationPhraseUseCase
) {
    operator fun invoke(): Flow<HomeVariant> = combine(
        getUserPlanUseCase(),
        getMotivationPhraseUseCase()
    ) { plan, phrase ->
        println("[GetHomeVariantUseCase] combine fired: plan=$plan, phrase='$phrase'")
        when (plan) {
            UserPlan.Free -> HomeVariant(showsDailyMotivation = false, tone = Tone.Gentle, motivationPhrase = phrase)
            UserPlan.Premium -> HomeVariant(showsDailyMotivation = true, tone = Tone.Direct, motivationPhrase = phrase)
        }
    }
}
