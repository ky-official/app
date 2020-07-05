package com.audiolemon.app

import com.audiolemon.videogenerator.LemonDBManager
import com.audiolemon.videogenerator.LemonFileManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AppApplication

fun main(args: Array<String>) {

    var id = "TSKQKG042421"
	LemonFileManager.deleteTaskDirectory(id)
	LemonDBManager.removeTask(id)
	runApplication<AppApplication>(*args)

}
