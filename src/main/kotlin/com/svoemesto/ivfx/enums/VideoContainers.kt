package com.svoemesto.ivfx.enums

enum class VideoContainers(val extention: String, val default: Boolean) {
    MP4("mp4", true),
    MKV("mkv", false),
    MXF("mxf", false)
}

enum class LosslessContainers(val extention: String, val default: Boolean) {
    MP4("mp4", false),
    MKV("mkv", true),
    MXF("mxf", false)
}

enum class VideoCodecs(val codec: String, val default: Boolean) {
    X264("libx264", true),
    DNX("dnxhd", false)
}

enum class LosslessVideoCodecs(val codec: String, val default: Boolean) {
    RAW("rawvideo", true),
    DNX("dnxhd", false)
}

enum class AudioCodecs(val codec: String, val default: Boolean) {
    AAC("aac", true),
    AC3("ac3", false),
    MP3("mp3", false),
    PMC("pcm_s16le", false)
}