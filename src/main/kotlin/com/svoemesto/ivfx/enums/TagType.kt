package com.svoemesto.ivfx.enums

enum class TagType(val order: Int, val description: String, val comment: String) {
    DESCRIPTION(0, "Description","Описание"),
    PERSON(1, "Person","Персонаж"),
    OBJECT(2, "Object","Объект"),
    SCENE(3, "Scene","Сцена"),
    EVENT(4, "Event","Событие")
}