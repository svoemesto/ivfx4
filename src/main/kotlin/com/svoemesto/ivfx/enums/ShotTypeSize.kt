package com.svoemesto.ivfx.enums

import com.svoemesto.ivfx.fxcontrollers.ProjectEditFXController

enum class ShotTypeSize(val order: Int, val description: String, val comment: String, val pathToPicture: String) {
    NONE(0, "N/A","Не определено", ShotTypeSize::class.java.getResource("shot_type_size_NONE.png")!!.toString()),
    ECU(1, "Extreame Close-Up","Деталь", ShotTypeSize::class.java.getResource("shot_type_size_ECU.png")!!.toString()),
    BCU(2, "Big Close-Up","Крупный", ShotTypeSize::class.java.getResource("shot_type_size_BCU.png")!!.toString()),
    CU(3, "Close-Up","Крупный (по плечи)", ShotTypeSize::class.java.getResource("shot_type_size_CU.png")!!.toString()),
    MCU(4, "Medium Close-Up","Крупный (по грудь)", ShotTypeSize::class.java.getResource("shot_type_size_MCU.png")!!.toString()),
    MS(5, "Medium Shot","Средний (по пояс)", ShotTypeSize::class.java.getResource("shot_type_size_MS.png")!!.toString()),
    MLS(6, "Medium Long Shot","Средний (по колено)", ShotTypeSize::class.java.getResource("shot_type_size_MLS.png")!!.toString()),
    LS(7, "Long Shot","Общий (видны ноги)", ShotTypeSize::class.java.getResource("shot_type_size_LS.png")!!.toString()),
    VLS(8, "Very Long Shot","Общий", ShotTypeSize::class.java.getResource("shot_type_size_VLS.png")!!.toString()),
    XLS(9, "Extreme Long Shot","Дальний", ShotTypeSize::class.java.getResource("shot_type_size_XLS.png")!!.toString())
}