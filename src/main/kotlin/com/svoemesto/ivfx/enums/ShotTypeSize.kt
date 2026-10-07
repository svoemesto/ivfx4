package com.svoemesto.ivfx.enums

import com.svoemesto.ivfx.utils.getResourceFilePath

enum class ShotTypeSize(val order: Int, val description: String, val comment: String, val pathToPicture: String) {
    NONE(0, "N/A", "Не определено", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_NONE.png")),
    ECU(1, "Extreame Close-Up", "Деталь", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_ECU.png")),
    BCU(2, "Big Close-Up", "Крупный", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_BCU.png")),
    CU(3, "Close-Up", "Крупный (по плечи)", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_CU.png")),
    MCU(4, "Medium Close-Up", "Крупный (по грудь)", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_MCU.png")),
    MS(5, "Medium Shot", "Средний (по пояс)", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_MS.png")),
    MLS(6, "Medium Long Shot", "Средний (по колено)", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_MLS.png")),
    LS(7, "Long Shot", "Общий (видны ноги)", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_LS.png")),
    VLS(8, "Very Long Shot", "Общий", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_VLS.png")),
    XLS(9, "Extreme Long Shot", "Дальний", getResourceFilePath(ShotTypeSize::class.java, "shot_type_size_XLS.png"))
}
