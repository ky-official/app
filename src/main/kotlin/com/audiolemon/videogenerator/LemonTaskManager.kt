package com.audiolemon.videogenerator

import kotlinx.coroutines.Job
import java.util.concurrent.CancellationException

sealed class LemonTaskManager {

    companion object {
        private var Tasks = HashMap<String, Job>()
        fun newJob(): Job {
            return Job()
        }

        fun addTask(id: String, job: Job) {
            this.Tasks[id] = job
            job.invokeOnCompletion {
                println("job was canceled. Too bad. ")
            }
            println("new job initiated")
        }

        suspend fun cancelTask(id: String) {
            try {
                this.Tasks[id]!!.cancel(CancellationException("job canceled by remote server"))
                this.Tasks.remove(id)
                LemonDBManager.connect()
                LemonDBManager.updateStatus(id, "CANCELED")
                LemonDBManager.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun taskIsRunning(id: String): Boolean {
            if (this.Tasks[id] == null) {
                return false
            }
            return true
        }

        fun isComplete(id: String): Boolean {
            if (this.Tasks[id]!!.isCompleted) {
                return true
            }
            return false
        }

    }
}