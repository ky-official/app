package com.audiolemon.videogenerator

import com.google.gson.*
import javax.servlet.http.Part

/*
* Manages the data and objects that are received from the server.
* Stores blob objects in a temp storage.
* Handles get calls on data ( serves as a data object).
* */
class LemonData {

    lateinit var id: String
        private set
    lateinit var audioUrl: String
        private set
    lateinit var foregroudImageUrl: String
        private set
    lateinit var backgroundImageUrl: String
        private set
    lateinit var header: String
        private set
    lateinit var subHeader: String
        private set
    lateinit var meta: LemonMeta
        private set
     var trackLength: Double? = null


    fun initialize(data: Collection<Part>) {
        data.forEach {

            when (it.name) {
                "id" -> {
                    this.id = String(it.inputStream.readBytes())
                    LemonFileManager.createTaskDirectory(id)
                }
                "audio" -> {
                    val path = LemonAudioConverter.convert(LemonFileManager.saveResources("audio", this.id, it)!!)
                    this.audioUrl = path!!
                }
                "background" -> {
                    val path = LemonFileManager.saveResources("background", this.id, it)
                    this.backgroundImageUrl = path!!
                }
                "foreground" -> {
                    val path = LemonFileManager.saveResources("foreground", this.id, it)
                    this.foregroudImageUrl = path!!
                }
                "header" -> {
                    this.header = String(it.inputStream.readBytes())
                }
                "subHeader" -> {
                    this.subHeader = String(it.inputStream.readBytes())
                }
                "meta" -> {
                    val meta = Gson().fromJson(String(it.inputStream.readBytes()), LemonMeta().javaClass)
                    this.meta = meta

                }
            }
        }
        LemonDBManager.addTask(this.id)
    }

}