package com.audiolemon.videogenerator

import com.audiolemon.videogenerator.analysis.FFT
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.*

/*
* Represents a waveform video task.
* Handles the video rendering.
* Instances are constructed with a lemonData object. Which contains the meta data needed.
* */
class LemonVideo(data: LemonData) {

    private val frameSize = 480
    private var data = data
    private var audioUrl: String = data.audioUrl
    private var videoUrl: String = LemonFileManager.createVideoContainer(data.id)
    private var writer = ToolFactory.makeWriter(videoUrl)

    private val MAX_VALUE = 1.0f / java.lang.Short.MAX_VALUE
    private val size = 1024
    private val sampleRate = 44100f
    private val monoSamples: FloatArray = FloatArray(size)

    private val fft = FFT(size, sampleRate)
    private var ampData: ArrayList<ArrayList<Float>> = ArrayList()

    init {
        writer.addVideoStream(
                0,
                0,
                ICodec.ID.CODEC_ID_MPEG4,
                IRational.make(24.0),
                frameSize,
                frameSize
        )
        writer.addAudioStream(1, 1, 2, 44100)

        if (data.meta.waveform.type == LemonWaveformType.FAD) {
            FADecoder()
        } else if (data.meta.waveform.type == LemonWaveformType.SAD) {
            SADecoder()
        }

    }

    suspend fun render() {
        LemonRenderer.start(data, writer, ampData,frameSize)
    }

    private fun FADecoder() {


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
            }
        }
    }

    private fun SADecoder() {

//        writer.encodeAudio(1, outputSamples)
    }


}


