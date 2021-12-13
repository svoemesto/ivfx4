package com.svoemesto.ivfx.enums

enum class TagType(val order: Int, val description: String, val comment: String) {
    NONE(0, "None","None"),
    PERSON(1, "Person","Персонаж"),
    SCENE(2, "Scene","Сцена"),
    EVENT(3, "Event","Событие"),
    DESCRIPTION_FOR_PERSON(3, "Description for person","Описание для персонажа"),
    DESCRIPTION_FOR_SCENE(3, "Description for scene","Описание для сцены"),
    DESCRIPTION_FOR_EVENT(3, "Description for event","Описание для события"),
    DESCRIPTION_FOR_SHOT(3, "Description for shot","Описание для плана"),
    SHOT_PERSON(3, "Shot person","Персонаж в плане"),
    SCENE_SHOT(3, "Scene shot","План в сцене"),
    EVENT_SHOT(3, "Event shot","План в событии"),
    PERSON_DESCRIPTION(3, "Person description","Описание персонажа"),
    SCENE_DESCRIPTION(3, "Scene description","Описание сцены"),
    EVENT_DESCRIPTION(3, "Event description","Описание события"),
    SHOT_DESCRIPTION(3, "Shot description","Описание плана")
}