package com.audiolemon.videogenerator

import com.google.gson.Gson
import javax.servlet.http.Part
import org.springframework.web.util.UriUtils

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
    lateinit var meta: LemonMeta
        private set
    var images: ArrayList<LemonImage> = ArrayList()
        private set
    var texts: ArrayList<LemonText> = ArrayList()
        private set
    var trackLength: Double? = null


    fun initialize(data: Collection<Part>) {


        data.forEach {
            if (it.name == "id") {
                this.id = String(it.inputStream.readBytes())
                LemonFileManager.createTaskDirectory(id)
                return@forEach
            }
        }
        data.forEach { it ->

            if (it.contentType != null && (it.contentType.substringBefore("/") == "audio" || it.name == "audio")) {
                val path = LemonAudioConverter.convert(LemonFileManager.saveResources("audio", this.id, it)!!)
                this.audioUrl = path!!
            }
            if (it.contentType != null && (it.contentType.substringBefore("/") == "image" || it.name.substringBefore("_") == "image")) {
                var string = it.getHeader("content-disposition")
                var json = UriUtils.decode(string.substring(string.indexOf("{"), string.indexOf("}") + 1), "UTF-8")
                val image = Gson().fromJson(json, LemonImage().javaClass)
                image.url = LemonFileManager.saveResources("image", this.id, it)
                this.images.add(image)
            }
            if (it.name.substringBefore("_") == "text") {
                val text = Gson().fromJson(String(it.inputStream.readBytes()), LemonText().javaClass)
                this.texts.add(text)
            }
            if (it.name == "meta") {
                val meta = Gson().fromJson(String(it.inputStream.readBytes()), LemonMeta().javaClass)
                this.meta = meta
            }

        }
        LemonDBManager.addTask(this.id)
    }

}