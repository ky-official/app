package com.audiolemon.videogenerator

import com.audiolemon.videogenerator.utility.TextFormat
import com.audiolemon.videogenerator.utility.TextRenderer
import com.xuggle.mediatool.IMediaWriter
import org.imgscalr.Scalr
import java.awt.*
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.roundToLong


class LemonRenderer {

    fun start(data: LemonData, writer: IMediaWriter, ampData: ArrayList<FloatArray>) {

        val width = 25.0
        var points: Int = 0
        if (data.meta.waveform.type == LemonWaveformType.FAD) {
            points = ampData[0].size
        } else if (data.meta.waveform.type == LemonWaveformType.SAD) {
            points = ampData.size
        }

        var index = 1
        var progress = 0
        var currentPoint = 0

        val heightBuffer = FloatArray(ampData.size)
        val staticImage = createStaticRenderedImage(data)
        val bufferedImage = BufferedImage(data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt(), BufferedImage.TYPE_3BYTE_BGR)
        val g2d = bufferedImage.createGraphics()
        applyQualityRenderingHints(g2d)

        var plotter = LemonPlotter().getPlotter(data.meta.waveform.type!!, data.meta.waveform.design!!)
        while (LemonTaskManager.taskIsRunning(data.id)) {

            Thread.sleep(0)
            if (currentPoint < points) {

                g2d.clearRect(0, 0, data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt())
                val trackProgress = (currentPoint / points.toDouble()) * 100
                if (trackProgress.roundToInt() != progress) {
                    println("task with id:${data.id} at $progress%")
                    progress = trackProgress.roundToInt()
                    LemonDBManager.updateProgress(data.id, progress)
                }

                g2d.drawRenderedImage(staticImage, null)
                val path = GeneralPath(Path2D.WIND_NON_ZERO, 5)
                path.moveTo(data.meta.waveform.posX!!.toDouble(), data.meta.waveform.posY!!.toDouble())

                plotter(data, ampData, currentPoint, path, heightBuffer, width, g2d)
                currentPoint++
                index++

                writer.encodeVideo(0, bufferedImage, (41666666.6666 * index).roundToLong(), TimeUnit.NANOSECONDS)
                bufferedImage.flush()
            } else break
        }
        writer.close()
        writer.flush()
        Runtime.getRuntime().gc()
        System.gc()
        println("writer closed")
        LemonDBManager.updateStatus(data.id, "RUNNING")

    }

    private fun createStaticRenderedImage(data: LemonData): BufferedImage {

        val bufferedImage = BufferedImage(data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt(), BufferedImage.TYPE_3BYTE_BGR)
        val bg = bufferedImage.createGraphics()
        applyQualityRenderingHints(bg)
        bg.color = Color.decode(data.meta.video.fill)
        bg.fillRect(0, 0, data.meta.video.width!!.toInt(), data.meta.video.height!!.toInt())

        val sortedImages = data.images.sortedWith(compareBy { it.zIndex })
        val sortedTexts = data.texts.sortedWith(compareBy { it.zIndex })

        for (image in sortedImages) {


            var source = ImageIO.read(LemonFileManager.getResource(image!!.url))
            if (image.width != 0.0 || image.height != 0.0) {
                source = Scalr.resize(source, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, image.width!!.toInt(), image.height!!.toInt(), Scalr.OP_ANTIALIAS)
            }

            when {
                image.align == LemonImageAlign.CENTER -> image.posX = (data.meta.video.width!! - source.width) / 2
                image.align == LemonImageAlign.RIGHT -> image.posX = (data.meta.video.width!! - source.width) * 3 / 4
                image.align == LemonImageAlign.RIGHT -> image.posX = (data.meta.video.width!! - source.width) / 4
            }

            if (image.mask != LemonMaskType.NONE) {
                when (image.mask) {
                    LemonMaskType.CIRCLE -> {
                        source = maskImageToCircle(source)
                    }
                    LemonMaskType.SQUARE -> {

                    }
                }
            }
            if (image.transform != null && image.transform != "none") {
                val degree = image.transform!!.substringAfterLast(":").trim().toDouble()
                source = rotateImageByDegrees(source, degree)
            }
            bg.drawImage(source, null, image.posX!!.toInt(), image.posY!!.toInt())
            if (image.frame != LemonFrameType.NONE && image.mask != LemonMaskType.CIRCLE) {
                var frameColor = Color.decode(image.frameColor)
                var frameWidth = 0f
                when (image.frame) {
                    LemonFrameType.THIN -> {
                        frameWidth = 2f
                    }
                    LemonFrameType.NORMAL -> {
                        frameWidth = 5f
                    }
                    LemonFrameType.SOLID -> {
                        frameWidth = 10f
                    }
                }
                bg.color = frameColor
                bg.stroke = BasicStroke(frameWidth)
                bg.drawRect(image.posX!!.roundToInt() + frameWidth.toInt() / 2, image.posY!!.roundToInt(), source.width, source.height)
            }
        }
        var font2: Font? = null
        try {
            val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
            font2 = Font.createFont(Font.TRUETYPE_FONT, LemonFileManager.getResource("src/main/kotlin/com/audiolemon/videogenerator/resources/Bebas-Regular.otf"))
            graphicsEnvironment.registerFont(font2)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        for (text in sortedTexts) {

            val attributes = HashMap<TextAttribute, Any>()
            attributes[TextAttribute.POSTURE] = if (text.fontStyle == LemonFontStyle.ITALIC) TextAttribute.POSTURE_OBLIQUE else TextAttribute.POSTURE_REGULAR
            when {
                text.fontWeight == LemonFontWeight.BOLD -> attributes[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_BOLD
                text.fontWeight == LemonFontWeight.NORMAL -> attributes[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_REGULAR
                text.fontWeight == LemonFontWeight.THIN -> attributes[TextAttribute.WEIGHT] = TextAttribute.WEIGHT_LIGHT
            }
            when {
                text.spacing == LemonSpacing.LOOSE -> attributes[TextAttribute.TRACKING] = 0.1
                text.spacing == LemonSpacing.NORMAL -> attributes[TextAttribute.TRACKING] = 0.0
                text.spacing == LemonSpacing.TIGHT -> attributes[TextAttribute.TRACKING] = -0.1
            }

            TextRenderer.drawString(
                    bg,
                    text.value,
                    Font(text.font!!, Font.PLAIN, text.fontSize!!).deriveFont(attributes),
                    Color.decode(text.color),
                    Rectangle(text.posX!!, text.posY!!, text.width!!, 100),
                    text.align,
                    TextFormat.FIRST_LINE_VISIBLE
            )
        }
        return bufferedImage
    }

    private fun rotateImageByDegrees(img: BufferedImage, angle: Double): BufferedImage {

        val rads = Math.toRadians(angle)
        val sin = Math.abs(Math.sin(rads))
        val cos = Math.abs(Math.cos(rads))
        val w = img.width
        val h = img.height
        val newWidth = Math.floor(w * cos + h * sin).toInt()
        val newHeight = Math.floor(h * cos + w * sin).toInt()

        val rotated = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)

        val at = AffineTransform()
        val x = w / 2
        val y = h / 2
        at.rotate(rads, x.toDouble(), y.toDouble())

        val g2d = rotated.createGraphics()
        applyQualityRenderingHints(g2d)
        g2d.transform = at
        g2d.drawRenderedImage(img, null)
        g2d.dispose()

        return rotated
    }

    private fun maskImageToCircle(img: BufferedImage): BufferedImage {

        var width = img.width
        var height = img.height
        var diameter = 0
        var oval: Area? = null
        diameter = if (width > height || width == height) {
            height
        } else {
            width
        }
        oval = if (width > height) {
            Area(Ellipse2D.Double((width - diameter.toDouble()) / 2, 0.0, diameter.toDouble(), diameter.toDouble()))
        } else {
            Area(Ellipse2D.Double((width - diameter.toDouble()) / 2, 0.0, diameter.toDouble(), diameter.toDouble()))
        }
        var masked = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = masked.createGraphics()
        applyQualityRenderingHints(g2d)
        g2d.clip(oval)
        g2d.drawRenderedImage(img, null)
        g2d.dispose()
        return masked
    }

    private fun applyQualityRenderingHints(g2d: Graphics2D) {

        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

}
