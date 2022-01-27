package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.FilterGroup

class FilterGroupExt(var filterGroup: FilterGroup): Comparable<FilterGroupExt> {

    override fun compareTo(other: FilterGroupExt): Int {
        return this.filterGroup.order - other.filterGroup.order
    }

    val name: String get() = filterGroup.name
    val order: Int get() = filterGroup.order
    val isAndText: String get() = if (filterGroup.isAnd) "&&" else "||"

    fun shotsExt(setOfShotsExt: Set<ShotExt>): Set<ShotExt> {
        val shotsExt: MutableSet<ShotExt> = mutableSetOf()

        if (filterGroup.isAnd) {
            var firstIteration = true
            filterGroup.filterConditions.forEach { filterCondition->
                val filterConditionExt = FilterConditionExt(filterCondition)
                val shotsExtInCondition = filterConditionExt.shotsExt(setOfShotsExt)
                if (firstIteration) {
                    shotsExt.addAll(shotsExtInCondition)
                    firstIteration = false
                } else {
                    val tmp: MutableSet<ShotExt> = mutableSetOf()
                    shotsExt.forEach { shotExt ->
                        if (shotsExtInCondition.contains(shotExt)) tmp.add(shotExt)
                    }
                    shotsExt.clear()
                    shotsExt.addAll(tmp)
                }
            }
        } else {
            filterGroup.filterConditions.forEach {
                val filterConditionExt = FilterConditionExt(it)
                shotsExt.addAll(filterConditionExt.shotsExt(setOfShotsExt))
            }
        }

        return shotsExt
    }
}