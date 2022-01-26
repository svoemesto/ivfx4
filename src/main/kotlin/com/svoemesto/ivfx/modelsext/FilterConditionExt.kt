package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.FilterCondition

class FilterConditionExt(var filterCondition: FilterCondition) {
    val name: String get() = filterCondition.name
}