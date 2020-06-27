package com.audiolemon.videogenerator

class LemonMeta {
    lateinit var audio: Audio
    lateinit var foreground: Foreground
    lateinit var header: Header
    lateinit var subHeader: SubHeader
    lateinit var waveform: Waveform
    lateinit var date: Date
    lateinit var audioTracker: AudioTracker
    lateinit var branding: Branding


}

class Audio {
    var trackLimits: String? = null
}

class Foreground {
    var frameType: String? = null
    var posX: Double? = null
    var posY: Double? = null
}

class Header {
    var font: String? = null
    var fontSize: Int? = null
    var fontStyle: String? = null
    var fontWeight: String? = null
    var color: String? = null
    var posX: Double? = null
    var posY: Double? = null
}

class SubHeader {
    var font: String? = null
    var fontSize: Int? = null
    var fontStyle: String? = null
    var fontWeight: String? = null
    var color: String? = null
    var posX: Double? = null
    var posY: Double? = null
}

class Waveform {
    var type: LemonWaveformType? = null
    var fill: String? = null
    var stroke: String? = null
    var design: LemonWaveformDesign? = null
    var posX: Double? = null
    var posY: Double? = null
}

class Date {
    var display: Boolean? = null
    var format: Int? = null
    var font: String? = null
    var fontSize: Int? = null
    var fontStyle: String? = null
    var fontWeight: String? = null
}

class AudioTracker {
    var display: Boolean? = null
    var type: LemonAudioTrackerType? = null
    var fill: String? = null
}

class Branding {
    var display: Boolean? = null
}