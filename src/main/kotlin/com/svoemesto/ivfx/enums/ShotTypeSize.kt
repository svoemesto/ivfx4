package com.svoemesto.ivfx.enums

import com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController

enum class ShotTypeSize(val order: Int, val description: String, val comment: String, val pathToPicture: String) {
    NONE(0, "N/A","Не определено", ShotTypeSize::class.java.getResource("shot_type_size_NONE.png")!!.file.substring(1)),
    ECU(1, "Extreame Close-Up","Деталь", ShotTypeSize::class.java.getResource("shot_type_size_ECU.png")!!.file.substring(1)),
    BCU(2, "Big Close-Up","Крупный", ShotTypeSize::class.java.getResource("shot_type_size_BCU.png")!!.file.substring(1)),
    CU(3, "Close-Up","Крупный (по плечи)", ShotTypeSize::class.java.getResource("shot_type_size_CU.png")!!.file.substring(1)),
    MCU(4, "Medium Close-Up","Крупный (по грудь)", ShotTypeSize::class.java.getResource("shot_type_size_MCU.png")!!.file.substring(1)),
    MS(5, "Medium Shot","Средний (по пояс)", ShotTypeSize::class.java.getResource("shot_type_size_MS.png")!!.file.substring(1)),
    MLS(6, "Medium Long Shot","Средний (по колено)", ShotTypeSize::class.java.getResource("shot_type_size_MLS.png")!!.file.substring(1)),
    LS(7, "Long Shot","Общий (видны ноги)", ShotTypeSize::class.java.getResource("shot_type_size_LS.png")!!.file.substring(1)),
    VLS(8, "Very Long Shot","Общий", ShotTypeSize::class.java.getResource("shot_type_size_VLS.png")!!.file.substring(1)),
    XLS(9, "Extreme Long Shot","Дальний", ShotTypeSize::class.java.getResource("shot_type_size_XLS.png")!!.file.substring(1))
}