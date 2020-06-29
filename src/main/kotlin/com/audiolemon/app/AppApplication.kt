package com.audiolemon.app

import com.audiolemon.videogenerator.LemonDBManager
import com.audiolemon.videogenerator.LemonFileManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AppApplication

fun main(args: Array<String>) {

	//LemonFileManager.deleteTaskDirectory("TSKQKG042420")
	//LemonDBManager.removeTask("TSKQKG042420")
	runApplication<AppApplication>(*args)
}
