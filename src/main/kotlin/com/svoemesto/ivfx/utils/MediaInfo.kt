package com.svoemesto.ivfx.utils

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type


object MediaInfo {
    private val MEDIA_INFO_CLI_PATH = MediaInfo::class.java.getResource("MediaInfo_CLI/MediaInfo.exe")?.path?:""
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
        process.waitFor()
        val out = buffer.toString()
        return out.substring(0, out.length - 2)

    }

    @JvmStatic
    fun main(args: Array<String>) {
        println(MEDIA_INFO_CLI_PATH)
        val json = executeMediaInfo("E:\\GOT\\GOT.S01\\GOT.S01E01.BDRip.1080p.mkv", "--Output=JSON")
        val tracks = getFromMediaInfoTracks(json)
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
