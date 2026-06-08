package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.HomeVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan

class GetHomeVariantUseCase(private val getUserPlanUseCase: GetUserPlanUseCase) {
    suspend operator fun invoke(): HomeVariant {
        val plan = getUserPlanUseCase.get()
        return when (plan) {
            UserPlan.Free -> HomeVariant(showsDailyMotivation = false, tone = Tone.Gentle)
            UserPlan.Premium -> HomeVariant(showsDailyMotivation = true, tone = Tone.Direct)
        }
    }
}
