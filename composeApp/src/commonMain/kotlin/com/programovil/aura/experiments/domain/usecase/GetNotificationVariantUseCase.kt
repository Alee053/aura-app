package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.NotificationVariant
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan

class GetNotificationVariantUseCase(private val getUserPlanUseCase: GetUserPlanUseCase) {
    suspend operator fun invoke(): NotificationVariant {
        val plan = getUserPlanUseCase.get()
        return when (plan) {
            UserPlan.Free -> NotificationVariant(timesPerDay = 1, tone = Tone.Gentle, useDueDateChannel = false)
            UserPlan.Premium -> NotificationVariant(timesPerDay = 2, tone = Tone.Direct, useDueDateChannel = true)
        }
    }
}
