package com.svoemesto.ivfx.enums

enum class ShotTypeSize(val order: Int, val description: String, val comment: String) {
    NONE(0, "N/A","Не определено"),
    ECU(1, "Extreame Close-Up","Деталь"),
    BCU(2, "Big Close-Up","Крупный"),
    CU(3, "Close-Up","Крупный (по плечи)"),
    MCU(4, "Medium Close-Up","Крупный (по грудь)"),
    MS(5, "Medium Shot","Средний (по пояс)"),
    MLS(6, "Medium Long Shot","Средний (по колено)"),
    LS(7, "Long Shot","Общий (видны ноги)"),
    VLS(8, "Very Long Shot","Общий"),
    XLS(9, "Extreme Long Shot","Дальний")
}