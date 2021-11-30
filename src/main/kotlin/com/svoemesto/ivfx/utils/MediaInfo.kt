package com.svoemesto.ivfx.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type


object MediaInfo {
    private val MEDIA_INFO_CLI_PATH = MediaInfo::class.java.getResource("MediaInfo_CLI/MediaInfo.exe").path
    @Throws(IOException::class, InterruptedException::class)
    fun getInfo(media: String): String {
        return executeMediaInfo(media)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun getInfoByParameter(media: String?, parameter: String?): String {
        return executeMediaInfo(media, parameter)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun getInfoBySectionAndParameter(media: String?, section: String, parameter: String): String {
        val param = "--Inform=\"$section;%$parameter%\""
        return executeMediaInfo(media, param)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun executeMediaInfo(media: String): String {
        val param: MutableList<String?> = ArrayList()
        param.add(media)
        return executeMediaInfo(param)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun executeMediaInfo(media: String?, parameter: String?): String {
        val param: MutableList<String?> = ArrayList()
        param.add(parameter)
        param.add(media)
        return executeMediaInfo(param)
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun executeMediaInfo(parameters: List<String?>): String {
        val exePath = MEDIA_INFO_CLI_PATH
        val param: MutableList<String?> = ArrayList()
        param.add(exePath)
        if (parameters.size > 0) {
            for (i in parameters.indices) {
                param.add(parameters[i])
            }
        }
        val builder = ProcessBuilder(param)
        builder.redirectErrorStream(true)
        val process = builder.start()
        val buffer = StringBuilder()
        InputStreamReader(process.inputStream).use { reader ->
            var i: Int
            while (reader.read().also { i = it } != -1) {
                buffer.append(i.toChar())
            }
        }
        val status = process.waitFor()
        val out = buffer.toString()
        return out.substring(0, out.length - 2)

    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(MEDIA_INFO_CLI_PATH)
        var json = executeMediaInfo("E:\\GOT\\GOT.S01\\GOT.S01E01.BDRip.1080p.mkv", "--Output=JSON")
        var tracks = getFromMediaInfoTracks(json)
        println(tracks)
    }

}

fun getFromMediaInfoTracks(json: String): List<Map<String, Any>>? {

    val mapType: Type = object : TypeToken<Map<String?, Map<*, *>?>?>() {}.getType()
    val son = Gson().fromJson<Map<String, Map<*, *>>>(json, mapType)
    val media: Map<String, Any> = son["media"] as Map<String, Any>
    val ref = media["@ref"].toString()
    return media["track"] as List<Map<String, Any>>?

}

@JsonIgnoreProperties(ignoreUnknown = true)
class MIFJCreatingLibrary {
    var name: String? = null
    var version: String? = null
    var url: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class MIFJExtra {
    var bsid: String? = null
    var dialnorm: String? = null
    var acmod: String? = null
    var lfeon: String? = null
    var dialnorm_Average: String? = null
    var dialnorm_Minimum: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class MIFJTrack{
    @JsonProperty("@type")
    var type: String? = null
    @JsonProperty("UniqueID")
    var uniqueID: String? = null
    @JsonProperty("VideoCount")
    var videoCount: String? = null
    @JsonProperty("AudioCount")
    var audioCount: String? = null
    @JsonProperty("TextCount")
    var textCount: String? = null
    @JsonProperty("FileExtension")
    var fileExtension: String? = null
    @JsonProperty("Format")
    var format: String? = null
    @JsonProperty("Format_Version")
    var format_Version: String? = null
    @JsonProperty("FileSize")
    var fileSize: String? = null
    @JsonProperty("Duration")
    var duration: String? = null
    @JsonProperty("OverallBitRate")
    var overallBitRate: String? = null
    @JsonProperty("FrameRate")
    var frameRate: String? = null
    @JsonProperty("FrameCount")
    var frameCount: String? = null
    @JsonProperty("StreamSize")
    var streamSize: String? = null
    @JsonProperty("IsStreamable")
    var isStreamable: String? = null
    @JsonProperty("Encoded_Date")
    var encoded_Date: String? = null
    @JsonProperty("File_Created_Date")
    var file_Created_Date: String? = null
    @JsonProperty("File_Created_Date_Local")
    var file_Created_Date_Local: String? = null
    @JsonProperty("File_Modified_Date")
    var file_Modified_Date: String? = null
    @JsonProperty("File_Modified_Date_Local")
    var file_Modified_Date_Local: String? = null
    @JsonProperty("Encoded_Application")
    var encoded_Application: String? = null
    @JsonProperty("Encoded_Library")
    var encoded_Library: String? = null
    @JsonProperty("StreamOrder")
    var streamOrder: String? = null
    @JsonProperty("ID")
    var iD: String? = null
    @JsonProperty("Format_Profile")
    var format_Profile: String? = null
    @JsonProperty("Format_Level")
    var format_Level: String? = null
    @JsonProperty("Format_Settings_CABAC")
    var format_Settings_CABAC: String? = null
    @JsonProperty("Format_Settings_RefFrames")
    var format_Settings_RefFrames: String? = null
    @JsonProperty("CodecID")
    var codecID: String? = null
    @JsonProperty("BitRate")
    var bitRate: String? = null
    @JsonProperty("Width")
    var width: String? = null
    @JsonProperty("Height")
    var height: String? = null
    @JsonProperty("Stored_Height")
    var stored_Height: String? = null
    @JsonProperty("Sampled_Width")
    var sampled_Width: String? = null
    @JsonProperty("Sampled_Height")
    var sampled_Height: String? = null
    @JsonProperty("PixelAspectRatio")
    var pixelAspectRatio: String? = null
    @JsonProperty("DisplayAspectRatio")
    var displayAspectRatio: String? = null
    @JsonProperty("FrameRate_Mode")
    var frameRate_Mode: String? = null
    @JsonProperty("FrameRate_Mode_Original")
    var frameRate_Mode_Original: String? = null
    @JsonProperty("ColorSpace")
    var colorSpace: String? = null
    @JsonProperty("ChromaSubsampling")
    var chromaSubsampling: String? = null
    @JsonProperty("BitDepth")
    var bitDepth: String? = null
    @JsonProperty("ScanType")
    var scanType: String? = null
    @JsonProperty("Delay")
    var delay: String? = null
    @JsonProperty("Encoded_Library_Name")
    var encoded_Library_Name: String? = null
    @JsonProperty("Encoded_Library_Version")
    var encoded_Library_Version: String? = null
    @JsonProperty("Encoded_Library_Settings")
    var encoded_Library_Settings: String? = null
    @JsonProperty("Default")
    var default: String? = null
    @JsonProperty("Forced")
    var forced: String? = null
    @JsonProperty("@typeorder")
    var typeorder: String? = null
    @JsonProperty("Format_Commercial_IfAny")
    var format_Commercial_IfAny: String? = null
    @JsonProperty("Format_Settings_Endianness")
    var format_Settings_Endianness: String? = null
    @JsonProperty("BitRate_Mode")
    var bitRate_Mode: String? = null
    @JsonProperty("Channels")
    var channels: String? = null
    @JsonProperty("ChannelPositions")
    var channelPositions: String? = null
    @JsonProperty("ChannelLayout")
    var channelLayout: String? = null
    @JsonProperty("SamplesPerFrame")
    var samplesPerFrame: String? = null
    @JsonProperty("SamplingRate")
    var samplingRate: String? = null
    @JsonProperty("SamplingCount")
    var samplingCount: String? = null
    @JsonProperty("Compression_Mode")
    var compression_Mode: String? = null
    @JsonProperty("Delay_Source")
    var delay_Source: String? = null
    @JsonProperty("StreamSize_Proportion")
    var streamSize_Proportion: String? = null
    @JsonProperty("Title")
    var title: String? = null
    @JsonProperty("Language")
    var language: String? = null
    @JsonProperty("ServiceKind")
    var serviceKind: String? = null
    var extra: MIFJExtra? = null
    @JsonProperty("ElementCount")
    var elementCount: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class MIFJMedia {
    @JsonProperty("@ref")
    var ref: String? = null
    var track: List<MIFJTrack>? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class MediaInfoFromJson {
    var creatingLibrary: MIFJCreatingLibrary? = null
    var media: MIFJMedia? = null
}
