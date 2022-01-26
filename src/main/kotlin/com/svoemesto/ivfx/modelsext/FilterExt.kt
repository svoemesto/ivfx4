package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Filter

class FilterExt(var filter: Filter) {
    val name: String get() = filter.name
    val isAndText: String get() = if (filter.isAnd) "&&" else "||"
}