package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Filter

class FilterExt(var filter: Filter): Comparable<FilterExt> {

    override fun compareTo(other: FilterExt): Int {
        return this.filter.order - other.filter.order
    }

    val name: String get() = filter.name
    val order: Int get() = filter.order
    val isAndText: String get() = if (filter.isAnd) "&&" else "||"


    fun shotsExt(setOfShotsExt: Set<ShotExt>): Set<ShotExt> {
        val shotsExt: MutableSet<ShotExt> = mutableSetOf()

        if (filter.isAnd) {
            var firstIteration = true
            filter.filterGroups.forEach { filterGroup->
                val filterGroupExt = FilterGroupExt(filterGroup)
                val shotsExtInGroup = filterGroupExt.shotsExt(setOfShotsExt)
                if (firstIteration) {
                    shotsExt.addAll(shotsExtInGroup)
                    firstIteration = false
                } else {
                    val tmp: MutableSet<ShotExt> = mutableSetOf()
                    shotsExt.forEach { shotExt ->
                        if (shotsExtInGroup.contains(shotExt)) tmp.add(shotExt)
                    }
                    shotsExt.clear()
                    shotsExt.addAll(tmp)
                }
            }
        } else {
            filter.filterGroups.forEach { filterGroup->
                val filterGroupExt = FilterGroupExt(filterGroup)
                shotsExt.addAll(filterGroupExt.shotsExt(setOfShotsExt))
            }
        }

        return shotsExt
    }

}