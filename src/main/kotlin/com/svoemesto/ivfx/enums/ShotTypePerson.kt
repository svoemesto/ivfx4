package com.svoemesto.ivfx.enums

enum class ShotTypePerson(val order: Int, val description: String, val comment: String) {
    NONE(0, "N/A","Не определено"),
    SGN(1, "Single","Один"),
    OTS(2, "Over The Shoulder","Один через плечо"),
    TWO(3, "Two Shot","Двое"),
    GRP(4, "Group Shot","Трое и более"),
    MASS(5, "Massive Shot","Очень много")
}