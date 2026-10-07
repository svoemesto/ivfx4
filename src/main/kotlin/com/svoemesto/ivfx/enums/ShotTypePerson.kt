package com.svoemesto.ivfx.enums

import com.svoemesto.ivfx.utils.getResourceFilePath

enum class ShotTypePerson(val order: Int, val description: String, val comment: String, val pathToPicture: String) {
    NONE(0, "N/A", "Не определено", getResourceFilePath(ShotTypePerson::class.java, "shot_type_person_NONE.png")),
    SGN(1, "Single", "Один", getResourceFilePath(ShotTypePerson::class.java, "shot_type_person_SGN.png")),
    OTS(2, "Over The Shoulder", "Один через плечо", getResourceFilePath(ShotTypePerson::class.java, "shot_type_person_OTS.png")),
    TWO(3, "Two Shot", "Двое", getResourceFilePath(ShotTypePerson::class.java, "shot_type_person_TWO.png")),
    GRP(4, "Group Shot", "Трое и более", getResourceFilePath(ShotTypePerson::class.java, "shot_type_person_GRP.png")),
    MASS(5, "Massive Shot", "Очень много", getResourceFilePath(ShotTypePerson::class.java, "shot_type_person_MASS.png"))
}
