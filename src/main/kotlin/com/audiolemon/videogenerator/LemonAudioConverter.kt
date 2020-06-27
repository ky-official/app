package com.audiolemon.videogenerator

import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

sealed class LemonAudioConverter {

    companion object {
        fun convert(path: String): String?{
            var source = LemonFileManager.getResource(path)
            var destination = LemonFileManager.getResource(path.substringBefore(".") + "_converted.wav")

            try{
                val inStream = AudioSystem.getAudioInputStream(source)
                val sourceFormat = inStream.format
                val convertFormat = AudioFormat(
                        44100f, 16,
                        sourceFormat.channels,
                        true,
                        false)

                println("source can be converted: ${AudioSystem.isConversionSupported(convertFormat, sourceFormat)}")

                val convStream = AudioSystem.getAudioInputStream(convertFormat, inStream)
                AudioSystem.write(convStream, AudioFileFormat.Type.WAVE, destination)
                return destination.absolutePath

            }
            catch (e: Exception){
                e.printStackTrace()
            }
            return null
        }
    }
}