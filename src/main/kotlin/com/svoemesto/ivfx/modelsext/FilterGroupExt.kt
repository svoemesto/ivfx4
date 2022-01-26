package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.FilterGroup

class FilterGroupExt(var filterGroup: FilterGroup) {
    val name: String get() = filterGroup.name
    val isAndText: String get() = if (filterGroup.isAnd) "&&" else "||"
}