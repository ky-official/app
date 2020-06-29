package com.audiolemon.videogenerator

import com.xuggle.mediatool.IMediaWriter
import com.xuggle.xuggler.IPixelFormat
import com.xuggle.xuggler.video.ConverterFactory
import java.awt.*
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import kotlin.math.roundToInt
import kotlin.math.roundToLong

sealed class LemonRenderer {

    companion object {
        suspend fun start(data: LemonData, writer: IMediaWriter, ampDta: ArrayList<ArrayList<Float>>, frameSize: Int) {
            when (data.meta.waveform.design) {
                LemonWaveformDesign.DEFAULT -> {
                    LemonDBManager.updateStatus(data.id, "RUNNING")
                    default(data, writer, ampDta, frameSize)
                }
                else -> {
                    LemonDBManager.updateStatus(data.id, "RUNNING")
                    default(data, writer, ampDta, frameSize)
                }
            }

        }

        suspend fun default(data: LemonData, writer: IMediaWriter, ampData: ArrayList<ArrayList<Float>>, frameSize: Int) {

            val ampData = arraySampler(compressMatrix(ampData, data.trackLength!!.toFloat(), 24), 60)

            val bend = 10
            val bend2 = 4

            val width = 25.0

            val startx = data.meta.waveform.posX!!.toDouble() + width
            val starty = data.meta.waveform.posY!!.toDouble()

            val y = starty
            var x = startx


            var cpOneX: Double
            var cpOneY: Double
            var cpTwoX: Double
            var cpTwoY: Double

            val points = ampData[0].size
            var currentPoint = 0


            val headerColor = Color.decode(data.meta.header.color)
            val subHeaderColor = Color.decode(data.meta.subHeader.color)
            val waveformFill = Color.decode(data.meta.waveform.fill)


            val attributes = HashMap<TextAttribute, Any>()
            attributes[TextAttribute.TRACKING] = -0.02
            val headerFont = Font(data.meta.header.font, Font.BOLD, data.meta.header.fontSize!!).deriveFont(attributes)
            val subHeaderFont = Font(data.meta.subHeader.font, Font.PLAIN, data.meta.subHeader.fontSize!!).deriveFont(attributes)


            val transform = AffineTransform.getTranslateInstance(0.0, 1080.0)
            transform.scale(1.0, -1.0)


            val bufferedImage = BufferedImage(1080, 1080, BufferedImage.TYPE_3BYTE_BGR)
            val bg = bufferedImage.createGraphics()

            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            var back = Toolkit.getDefaultToolkit().getImage(data.backgroundImageUrl)
            var forg = Toolkit.getDefaultToolkit().getImage(data.foregroudImageUrl)
            var watermark = Toolkit.getDefaultToolkit().getImage("src/main/kotlin/com/audiolemon/videogenerator/resources/watermark.png")

            back = back.getScaledInstance(1080, 1080, Image.SCALE_SMOOTH)
            forg = forg.getScaledInstance(180, 180, Image.SCALE_SMOOTH)
            watermark = watermark.getScaledInstance(200, 200, Image.SCALE_SMOOTH)

            var index = 1
            var progress = 0
            val heightBuffer: Array<Float> = Array<Float>(ampData.size) { i: Int -> i * 0f }

            var imageLoaded = false
            val obs = object : ImageObserver {
                override fun imageUpdate(img: Image?, infoflags: Int, x: Int, y: Int, width: Int, height: Int): Boolean {

                    imageLoaded = infoflags == 32
                    return true

                }

            }

            //pre draws
            bg.drawImage(watermark, 750, 820, null)
            bg.drawImage(forg, data.meta.foreground.posX!!.roundToInt(), data.meta.foreground.posY!!.roundToInt(), null)
            bg.drawImage(back, null, obs)

            while (LemonTaskManager.taskIsRunning(data.id)) {
                //dummy logic for heroku
                Thread.sleep(0)

                if (imageLoaded) {

                    val trackProgress = (currentPoint / points.toDouble()) * 100
                    if (trackProgress.roundToInt() != progress) {
                        println("task with id:${data.id} at $progress%")
                        progress = trackProgress.roundToInt()
                        LemonDBManager.updateProgress(data.id, progress)
                    }

                    if (currentPoint < points) {


                        bg.drawImage(back, 0, 0, null)
                        bg.font = headerFont
                        bg.color = headerColor
                        bg.drawString(data.header, data.meta.header.posX!!.toFloat(), data.meta.header.posY!!.toFloat())

                        bg.font = subHeaderFont
                        bg.color = subHeaderColor
                        bg.drawString(data.subHeader, data.meta.subHeader.posX!!.toFloat(), data.meta.subHeader.posY!!.toFloat())


                        if (data.meta.branding.display!!) {
                            bg.drawImage(watermark, 750, 820, null)
                        }
                        bg.drawImage(forg, data.meta.foreground.posX!!.roundToInt(), data.meta.foreground.posY!!.roundToInt(), null)


                        if (data.meta.foreground.frameType == "solid") {
                            bg.stroke = BasicStroke(20f)
                            bg.drawRect(data.meta.foreground.posX!!.roundToInt(), data.meta.foreground.posY!!.roundToInt(), 180, 180)
                        }

                        bg.stroke = BasicStroke(0f)
                        val path = GeneralPath(Path2D.WIND_NON_ZERO, 5)
                        path.moveTo(x - width, y)


                        for (band in 0 until ampData.size) {

                            val cpx = x
                            var cpy: Double

                            if (ampData[band][currentPoint] > heightBuffer[band]) {
                                cpy = y + ampData[band][currentPoint]
                                heightBuffer[band] = ampData[band][currentPoint]
                            } else {
                                cpy = y + heightBuffer[band]
                            }

                            heightBuffer[band] = heightBuffer[band] - 5

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
                        path.closePath()
                        path.transform(transform)
                        bg.paint = waveformFill
                        bg.draw(path)
                        bg.fill(path)
                        x = startx
                        currentPoint++

                        val bgrScreen = resizeImage(bufferedImage, frameSize, frameSize)
                        val converter = ConverterFactory.createConverter(bufferedImage, IPixelFormat.Type.YUV420P)
                        val frame = converter.toPicture(bgrScreen, (41666.666 * index).roundToLong())

                        writer.encodeVideo(0, frame)

                        index++
                        bg.clearRect(0, 0, 1080, 1080)
                    } else {
                        break
                    }
                }
            }

            writer.close()
            LemonDBManager.updateStatus(data.id, "FINISHED")
            println("Video Created")
        }

        private fun compressMatrix(
                matrix: ArrayList<ArrayList<Float>>,
                sampleLength: Float,
                sampleRate: Int
        ): ArrayList<ArrayList<Float>> {

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

        private fun resizeImage(image: BufferedImage, width: Int, height: Int): BufferedImage {

            if (height != 1080) {
                val scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
                val type: Int = if (image.type == 0) BufferedImage.TYPE_INT_ARGB else image.type
                val resizedImage = BufferedImage(width, height, type)
                val g = resizedImage.createGraphics()
                g.drawImage(scaled, 0, 0, width, height, null)
                g.dispose()
                return resizedImage
            }
            return image
        }

    }
}