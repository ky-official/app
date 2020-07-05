package com.audiolemon.videogenerator

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.util.*


class LemonPlotter {

    fun getPlotter(type: LemonWaveformType, design: LemonWaveformDesign):
            (LemonData, ArrayList<FloatArray>, Int, GeneralPath, FloatArray, Double, Graphics2D) -> Unit {
        if (type == LemonWaveformType.SAD) {
            when (design) {
                LemonWaveformDesign.DEFAULT -> return ::sigAmpPlotterSpectralFlux
            }
        } else if (type == LemonWaveformType.FAD) {
            when (design) {
                LemonWaveformDesign.DEFAULT -> return ::freqAmpPlotterSpectralFlux
            }
        }
        return { _: LemonData, _: ArrayList<FloatArray>, _: Int, _: GeneralPath, _: FloatArray, _: Double, _: Graphics2D -> { println("invalid plotter parameters") } }
    }

    private fun freqAmpPlotterSpectralFlux(data: LemonData, ampData: ArrayList<FloatArray>, currentPoint: Int, path: GeneralPath, heightBuffer: FloatArray, width: Double, g2d: Graphics2D) {

        val bend = 10
        val bend2 = 4

        var x = data.meta.waveform.posX!!.toDouble() + width
        var y = data.meta.waveform.posY!!.toDouble()
        var cpOneX: Double
        var cpOneY: Double
        var cpTwoX: Double
        var cpTwoY: Double

        for (band in 0 until ampData.size) {

            val cpx = x
            var cpy: Double

            if (ampData[band][currentPoint] > heightBuffer[band]) {
                cpy = y + ampData[band][currentPoint]
                heightBuffer[band] = ampData[band][currentPoint]
            } else {
                cpy = y + heightBuffer[band]
            }

            heightBuffer[band] = heightBuffer[band] - 8

            if (band == 0) {

                val spx = x - width
                val spy = y

                val npx = x + width
                val npy = y + ampData[band + 1][currentPoint]

                cpOneX = spx + (cpx - spx) / bend2
                cpOneY = spy + (cpy - spy) / bend

                cpTwoX = cpx - (npx - spx) / bend2
                cpTwoY = cpy - (npy - spy) / bend

                path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

            } else if (band > 0 && band < ampData.size - 2) {

                var pp0x: Double
                var pp0y: Double

                if (band == 1) {
                    pp0x = x - (width * 2)
                    pp0y = y
                } else {
                    pp0x = x - (width * 2)
                    pp0y = y + ampData[band - 2][currentPoint]
                }

                val ppx = x - width
                val ppy = y + ampData[band - 1][currentPoint]

                val npx = x + width
                val npy = y + ampData[band + 1][currentPoint]

                cpOneX = ppx + (cpx - pp0x) / bend2
                cpOneY = ppy + (cpy - pp0y) / bend

                cpTwoX = cpx - (npx - ppx) / bend2
                cpTwoY = cpy - (npy - ppy) / bend

                path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

            } else {
                val pp0x = x - (width * 2)
                val pp0y = y + ampData[band - 2][currentPoint]

                val ppx = x - width
                val ppy = y + ampData[band - 1][currentPoint]



                cpOneX = ppx + (cpx - pp0x) / bend2
                cpOneY = ppy + (cpy - pp0y) / bend

                cpTwoX = cpx - (cpx - ppx) / bend2
                cpTwoY = cpy - (cpy - ppy) / bend

                path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

            }
            x += width
        }

        val scalex = data.meta.waveform.width!!/path.bounds.width
        val scaley = 1.0

        var flip = AffineTransform.getTranslateInstance(0.0, data.meta.video.height!!.toDouble());flip.scale(1.0, -1.0)
        var restorePosition = AffineTransform.getTranslateInstance(1.0, 2 * data.meta.waveform.posY!! - data.meta.video.height!!)
        var scale = AffineTransform.getScaleInstance(scalex, scaley)

        path.lineTo(x, y)
        path.closePath()
        path.transform(flip)
        path.transform(restorePosition)

        path.transform(AffineTransform.getTranslateInstance(-data.meta.waveform.posX!!, -data.meta.waveform.posY!!))
        path.transform(scale)
        path.transform(AffineTransform.getTranslateInstance(data.meta.waveform.posX!!, data.meta.waveform.posY!!))

        g2d.color = Color.decode(data.meta.waveform.fill)
        g2d.draw(path)
        g2d.fill(path)



    }

    private fun sigAmpPlotterSpectralFlux(data: LemonData, ampData: ArrayList<FloatArray>, currentPoint: Int, path: GeneralPath, heightBuffer: FloatArray, width: Double, g2d: Graphics2D) {


        val bend = 10
        val bend2 = 4

        var x = data.meta.waveform.posX!!.toDouble() + width
        var y = data.meta.waveform.posY!!.toDouble()
        var cpOneX: Double
        var cpOneY: Double
        var cpTwoX: Double
        var cpTwoY: Double

        for (band in 0 until ampData[0].size) {

            val cpx = x
            var cpy: Double

            if (ampData[currentPoint][band] > heightBuffer[band]) {
                cpy = y + ampData[currentPoint][band]
                heightBuffer[band] = ampData[currentPoint][band]
            } else {
                cpy = y + heightBuffer[band]
            }

            heightBuffer[band] = heightBuffer[band] - 5

            if (band == 0) {

                val spx = x - width
                val spy = y

                val npx = x + width
                val npy = y + ampData[currentPoint][band + 1]

                cpOneX = spx + (cpx - spx) / bend2
                cpOneY = spy + (cpy - spy) / bend

                cpTwoX = cpx - (npx - spx) / bend2
                cpTwoY = cpy - (npy - spy) / bend

                path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

            } else if (band > 0 && band < ampData[0].size - 2) {

                var pp0x: Double
                var pp0y: Double

                if (band == 1) {
                    pp0x = x - (width * 2)
                    pp0y = y
                } else {
                    pp0x = x - (width * 2)
                    pp0y = y + ampData[currentPoint][band - 2]
                }

                val ppx = x - width
                val ppy = y + ampData[currentPoint][band - 1]

                val npx = x + width
                val npy = y + ampData[currentPoint][band + 1]

                cpOneX = ppx + (cpx - pp0x) / bend2
                cpOneY = ppy + (cpy - pp0y) / bend

                cpTwoX = cpx - (npx - ppx) / bend2
                cpTwoY = cpy - (npy - ppy) / bend

                path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

            } else {
                val pp0x = x - (width * 2)
                val pp0y = y + ampData[currentPoint][band - 2]

                val ppx = x - width
                val ppy = y + ampData[currentPoint][band - 1]



                cpOneX = ppx + (cpx - pp0x) / bend2
                cpOneY = ppy + (cpy - pp0y) / bend

                cpTwoX = cpx - (cpx - ppx) / bend2
                cpTwoY = cpy - (cpy - ppy) / bend

                path.curveTo(cpOneX, cpOneY, cpTwoX, cpTwoY, cpx, cpy)

            }
            x += width

        }


        val scalex = data.meta.waveform.width!!/path.bounds.width
        val scaley = 1.0

        var flip = AffineTransform.getTranslateInstance(0.0, data.meta.video.height!!.toDouble()).also {it.scale(1.0, -1.0)  }
        var scale = AffineTransform.getScaleInstance(scalex, scaley)
        var restorePosition = AffineTransform.getTranslateInstance(0.0, 2 * data.meta.waveform.posY!! - data.meta.video.height!!)
        path.lineTo(x, y)
        path.closePath()

        var path2 = GeneralPath(path)

        path.transform(flip)
        path.transform(restorePosition)
        path.append(path2, false)

        path.transform(AffineTransform.getTranslateInstance(-data.meta.waveform.posX!!, -data.meta.waveform.posY!!))
        path.transform(scale)
        path.transform(AffineTransform.getTranslateInstance(data.meta.waveform.posX!!, data.meta.waveform.posY!!))

        g2d.color = Color.decode(data.meta.waveform.fill)
        g2d.draw(path)
        g2d.fill(path)
    }

}

