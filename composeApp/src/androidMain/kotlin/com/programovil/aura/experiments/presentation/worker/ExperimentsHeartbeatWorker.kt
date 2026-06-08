package com.programovil.aura.experiments.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.experiments.domain.usecase.LogExperimentEventUseCase
import java.util.concurrent.TimeUnit
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class ExperimentsHeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    override suspend fun doWork(): Result {
        val getUserPlanUseCase: GetUserPlanUseCase = get()
        val logExperimentEventUseCase: LogExperimentEventUseCase = get()
        val plan = getUserPlanUseCase.get()
        val result = logExperimentEventUseCase(ExperimentEvent.SessionActive(plan))
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "experiments_heartbeat_work"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExperimentsHeartbeatWorker>(
                12, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
