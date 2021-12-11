package com.svoemesto.ivfx.enums

enum class ShotTypePerson(val order: Int, val description: String, val comment: String, val pathToPicture: String) {
    NONE(0, "N/A","Не определено", ShotTypePerson::class.java.getResource("shot_type_person_NONE.png")!!.file.substring(1)),
    SGN(1, "Single","Один", ShotTypePerson::class.java.getResource("shot_type_person_SGN.png")!!.file.substring(1)),
    OTS(2, "Over The Shoulder","Один через плечо", ShotTypePerson::class.java.getResource("shot_type_person_OTS.png")!!.file.substring(1)),
    TWO(3, "Two Shot","Двое", ShotTypePerson::class.java.getResource("shot_type_person_TWO.png")!!.file.substring(1)),
    GRP(4, "Group Shot","Трое и более", ShotTypePerson::class.java.getResource("shot_type_person_GRP.png")!!.file.substring(1)),
    MASS(5, "Massive Shot","Очень много", ShotTypePerson::class.java.getResource("shot_type_person_MASS.png")!!.file.substring(1))
}