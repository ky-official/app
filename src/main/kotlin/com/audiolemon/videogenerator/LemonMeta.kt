package com.audiolemon.videogenerator

import com.audiolemon.videogenerator.utility.TextAlignment

class LemonMeta {
    lateinit var video: Video
    lateinit var waveform: Waveform
    lateinit var audioTracker: AudioTracker


}
class LemonImage{
    var url: String? = null
    var frame: LemonFrameType? = null
    var frameColor: String? = null
    var width: Double? = null
    var height: Double? = null
    var mask: LemonMaskType? = null
    var transform: String? = null
    var posX: Double? = null
    var posY: Double? = null
    var zIndex: Int? = null
    var align: LemonImageAlign? = null
}

class LemonText{
    var value: String? = null
    var font: String? = null
    var fontSize: Int? = null
    var fontStyle: LemonFontStyle? = null
    var fontWeight: LemonFontWeight? = null
    var color: String? = null
    var posX: Int? = null
    var posY: Int? = null
    var zIndex: Int? = null
    var align: TextAlignment? = null
    var width: Int? = null
    var spacing: LemonSpacing? = null
}


class Video {
    val fill:String? = null
    var width: Double? = null
    var height: Double? = null
    var waterMark: Boolean? = null
    var waterMarkType: LemonWaterMark? = null
}
class Waveform {
    var type: LemonWaveformType? = null
    var fill: String? = null
    var stroke: String? = null
    var design: LemonWaveformDesign? = null
    var width: Double? = null
    var height: Double? = null
    var posX: Double? = null
    var posY: Double? = null
}

class AudioTracker {
    var display: Boolean? = null
    var type: LemonAudioTrackerType? = null
    var fill: String? = null
}

