package com.svoemesto.ivfx.enums

enum class ShotTypePerson(val order: Int, val description: String, val comment: String, val pathToPicture: String) {
    NONE(0, "N/A","Не определено", ShotTypePerson::class.java.getResource("shot_type_person_NONE.png")!!.toString()),
    SGN(1, "Single","Один", ShotTypePerson::class.java.getResource("shot_type_person_SGN.png")!!.toString()),
    OTS(2, "Over The Shoulder","Один через плечо", ShotTypePerson::class.java.getResource("shot_type_person_OTS.png")!!.toString()),
    TWO(3, "Two Shot","Двое", ShotTypePerson::class.java.getResource("shot_type_person_TWO.png")!!.toString()),
    GRP(4, "Group Shot","Трое и более", ShotTypePerson::class.java.getResource("shot_type_person_GRP.png")!!.toString()),
    MASS(5, "Massive Shot","Очень много", ShotTypePerson::class.java.getResource("shot_type_person_MASS.png")!!.toString())
}