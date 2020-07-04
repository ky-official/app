package com.audiolemon.videogenerator

import com.audiolemon.videogenerator.analysis.FFT
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.*
import kotlin.math.roundToInt

/*
* Represents a waveform video task.
* Handles the video rendering.
* Instances are constructed with a lemonData object. Which contains the meta data needed.
* */
class LemonVideo(data: LemonData) {

    private val frameWidth = data.meta.video.width!!.toInt()
    private val frameHeight = data.meta.video.height!!.toInt()
    private var data = data
    private var audioUrl: String = data.audioUrl
    private var videoUrl: String = LemonFileManager.createVideoContainer(data.id)
    private var writer = ToolFactory.makeWriter(videoUrl)

    private val MAX_VALUE = 1.0f / java.lang.Short.MAX_VALUE
    private val size = 1024
    private val sampleRate = 44100f

    private val fft = FFT(size, sampleRate)

    private val monoSamples: FloatArray = FloatArray(size)
    private var freqAmpData: ArrayList<FloatArray> = ArrayList()
    private var sigAmpData: ArrayList<FloatArray> = ArrayList()


    init {
        writer.addVideoStream(
                0,
                0,
                ICodec.ID.CODEC_ID_H264,
                IRational.make(24.0),
                frameWidth,
                frameHeight
        )
        writer.addAudioStream(1, 1, 2, 44100)

       this.decode()
        println("audio source decoded")

    }

    suspend fun render() {
        val render = LemonRenderer()
        if (data.meta.waveform.type == LemonWaveformType.FAD) {
            render.start(data, writer, freqAmpData)
        } else if (data.meta.waveform.type == LemonWaveformType.SAD) {
            render.start(data, writer, sigAmpData)
        }
    }

    private fun decode() {

        var ampData: ArrayList<ArrayList<Float>> = ArrayList()

        val audioContainer: IContainer = IContainer.make()
        audioContainer.open(this.audioUrl, IContainer.Type.READ, null)
        data.trackLength = audioContainer.duration / 1000000.0


        val stream = audioContainer.getStream(0)
        val coder: IStreamCoder = stream.streamCoder
        coder.open()

        val packet: IPacket = IPacket.make()

        for (i in 0..size / 2) {
            ampData.add(ArrayList<Float>())
        }
        var inputSamples = IAudioSamples.make(512, coder.channels.toLong(), IAudioSamples.Format.FMT_S32)
        while (audioContainer.readNextPacket(packet) >= 0) {

            var offset = 0
            while (offset < packet.size) {
                val bytesDecoded = coder.decodeAudio(inputSamples, packet, offset)
                if (bytesDecoded < 0) {
                    throw RuntimeException("could not detect audio")
                }
                offset += bytesDecoded
                if (inputSamples.isComplete) {

                    if (data.meta.waveform.type == LemonWaveformType.FAD) {
                        for (index in 0 until size) {
                            var amp1 = inputSamples.getSample(index.toLong(), 0, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var amp2 = inputSamples.getSample(index.toLong(), 1, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var monoAmp = (amp1 + amp2) / 2
                            monoSamples[index] = monoAmp
                        }
                        fft.forward(monoSamples)

                        for (i in 0 until ampData.size) {

                            var amp = fft.getBand(i)
                            if (amp > 35) {
                                amp /= 100
                            }
                            ampData[i].add(amp * 5)
                        }
                        writer.encodeAudio(1, inputSamples)
                    }
                    else if (data.meta.waveform.type == LemonWaveformType.SAD) {
                        for (index in 0 until size) {
                            var amp1 = inputSamples.getSample(index.toLong(), 0, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var amp2 = inputSamples.getSample(index.toLong(), 1, IAudioSamples.Format.FMT_S16) * MAX_VALUE
                            var monoAmp = (amp1 + amp2) / 2
                            monoSamples[index] =  Math.abs(monoAmp*100)
                        }
                        sigAmpData.add(arraySampler(monoSamples, 100))
                        writer.encodeAudio(1, inputSamples)
                    }




                }
            }
        }
        if (data.meta.waveform.type == LemonWaveformType.FAD) {
            ampData = arraySampler(compressMatrix(ampData, data.trackLength!!.toFloat(), 24), 60)
            //to float array
            for (band in 0 until  ampData.size){
                freqAmpData.add(ampData[band].toFloatArray())
            }
        }
        else if (data.meta.waveform.type == LemonWaveformType.SAD) {
            sigAmpData = arraySampler(sigAmpData, (24 * data.trackLength!!).roundToInt())

        }

    }



    private fun compressMatrix(matrix: ArrayList<ArrayList<Float>>, sampleLength: Float, sampleRate: Int): ArrayList<ArrayList<Float>> {

        val matrixSize = matrix.size
        val final = ArrayList<ArrayList<Float>>()
        val interval = (sampleRate * sampleLength).roundToInt()

        for (i in 0 until matrixSize) {
            final.add(ArrayList<Float>())
        }


        for (i in 0 until matrixSize) {
            val sourceBand = matrix[i]
            final[i] = arraySampler(sourceBand, interval)
        }
        return final
    }

    private inline fun <reified T> arraySampler(array: ArrayList<T>, sampleSize: Int): ArrayList<T> {

        if (sampleSize > array.size) {
            return array
        }
        val result: ArrayList<T> = ArrayList<T>()
        val totalItems = array.size
        val interval = totalItems.toDouble() / sampleSize

        for (i in 0 until sampleSize) {
            val evenIndex = Math.floor(i * interval + interval / 2).toInt()
            result.add(array[evenIndex])
        }
        return result
    }

    private fun arraySampler(array: FloatArray, sampleSize: Int): FloatArray {

        if (sampleSize > array.size) {
            return array
        }
        val result: FloatArray = FloatArray(sampleSize)
        val totalItems = array.size
        val interval = totalItems.toDouble() / sampleSize

        for (i in 0 until sampleSize) {
            val evenIndex = Math.floor(i * interval + interval / 2).toInt()
            result[i] = (array[evenIndex])
        }
        return result
    }


}


