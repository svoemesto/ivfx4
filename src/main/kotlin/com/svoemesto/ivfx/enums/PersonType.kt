package com.svoemesto.ivfx.enums

enum class PersonType(val order: Int, val description: String, val comment: String) {
    PERSON(0, "Person","Персонаж"),
    NONPERSON(1, "NonPerson","Не персонаж"),
    EXTRAS(2, "Extras","Массовка"),
    UNDEFINDED(3, "Undefinded","Неопределенный персонаж")
}