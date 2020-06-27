package com.audiolemon.app.controllers

import com.audiolemon.videogenerator.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest


@RestController
class MainController {

    @Autowired
    private val request: HttpServletRequest? = null


    @GetMapping("/progress/{id}")
    fun progressController(@PathVariable id: String): String {

        return LemonDBManager.getProgress(id).toString()
    }

    @GetMapping("/status/{id}")
    fun statusController(@PathVariable id: String): String {

        return LemonDBManager.getStatus(id)
    }

    @GetMapping("/cancel/{id}")
    fun cancelController(@PathVariable id: String): String {

        CoroutineScope(Dispatchers.IO).launch {
            LemonTaskManager.cancelTask(id)
        }
        return "canceled task with id:$id"
    }

    @DeleteMapping("/delete/{id}")
    fun deleteController(@PathVariable id: String): ResponseEntity<Any> {

        when {
            LemonDBManager.getStatus(id) in arrayOf("RUNNING", "QUEUED") -> CoroutineScope(Dispatchers.IO).launch {
                LemonTaskManager.cancelTask(id)
                LemonDBManager.removeTask(id)
                LemonFileManager.deleteTaskDirectory(id)
            }
            LemonDBManager.getStatus(id) in arrayOf("CANCELED", "FINISHED") -> {
                LemonDBManager.removeTask(id)
                LemonFileManager.deleteTaskDirectory(id)
            }
            else -> {
                return ResponseEntity(id, HttpStatus.NOT_FOUND)
            }
        }
        return ResponseEntity(id, HttpStatus.OK)
    }


    @PostMapping("/generate")
    @CrossOrigin
    fun postController() {

        try {
            var data = LemonData()
            data.initialize(request!!.parts)
            var lemonVideo = LemonVideo(data)

            var job = LemonTaskManager.newJob()
            LemonTaskManager.addTask(data.id, job)
            CoroutineScope(Dispatchers.IO + job).launch {
                lemonVideo.render()
            }
            println("controller returned")
        } catch (e: LemonException) {
            println(e.message)
        }

    }
}