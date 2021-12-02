package com.svoemesto.ivfx.utils

object FaceDetection {
    val FACE_DETECTOR_PATH = FaceDetection::class.java.getResource("FaceDetector")?.path?.substring(1)?:""
}