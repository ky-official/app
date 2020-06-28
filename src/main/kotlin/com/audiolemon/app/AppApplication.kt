package com.audiolemon.app

import com.audiolemon.videogenerator.LemonDBManager
import com.audiolemon.videogenerator.LemonFileManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AppApplication

fun main(args: Array<String>) {

	LemonFileManager.deleteTaskDirectory("")
	LemonDBManager.removeTask("")
	runApplication<AppApplication>(*args)
}
