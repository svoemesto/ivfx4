package com.svoemesto.ivfx.utils

import oshi.SystemInfo

object ComputerIdentifier {
    @Throws(Exception::class)
    fun getComputerId(): Int {
        val systemInfo = SystemInfo()
        val operatingSystem = systemInfo.operatingSystem
        val hardwareAbstractionLayer = systemInfo.hardware
        val centralProcessor = hardwareAbstractionLayer.processor
        val vendor = operatingSystem.manufacturer
        val processorSerialNumber: String = centralProcessor.processorIdentifier.processorID
        val processorIdentifier: String = centralProcessor.processorIdentifier.identifier
        val processors = centralProcessor.logicalProcessorCount
        val delimiter = "#"
        return (vendor +
                delimiter +
                processorSerialNumber +
                delimiter +
                processorIdentifier +
                delimiter +
                processors).hashCode()
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(arguments: Array<String>) {
        val identifier = getComputerId()
        println(identifier)
    }
}