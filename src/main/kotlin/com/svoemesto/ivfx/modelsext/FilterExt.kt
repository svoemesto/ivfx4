package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.Filter
import com.svoemesto.ivfx.models.Shot

class FilterExt(var filter: Filter): Comparable<FilterExt> {

    override fun compareTo(other: FilterExt): Int {
        return this.filter.order - other.filter.order
    }

    val name: String get() = filter.name
    val order: Int get() = filter.order
    val isAndText: String get() = if (filter.isAnd) "&&" else "||"


    fun shotsIds(): Set<Long> {
        val shotsIds: MutableSet<Long> = mutableSetOf()
        val shotsIdsFromChild: MutableSet<Long> = mutableSetOf()
        if (filter.isAnd) {
            var firstIteration = true
            filter.filterGroups.forEach { filterGroup->
                val filterGroupExt = FilterGroupExt(filterGroup)
                val shotsIdsInGroup = filterGroupExt.shotsIds()
                if (firstIteration) {
                    shotsIdsFromChild.addAll(shotsIdsInGroup)
                    firstIteration = false
                } else {
                    shotsIdsFromChild.retainAll(shotsIdsInGroup)
                }
            }
        } else {
            filter.filterGroups.forEach { filterGroup ->
                val filterGroupExt = FilterGroupExt(filterGroup)
                val shotsIdsInGroup = filterGroupExt.shotsIds()
                shotsIdsFromChild.addAll(shotsIdsInGroup)
            }
        }

        shotsIds.addAll(shotsIdsFromChild)

        return shotsIds

    }

    fun shots(): Set<Shot> {
        val shots: MutableSet<Shot> = mutableSetOf()
        val shotsFromChild: MutableMap<Long, Shot> = mutableMapOf()
        if (filter.isAnd) {
            var firstIteration = true
            filter.filterGroups.forEach { filterGroup->
                val filterGroupExt = FilterGroupExt(filterGroup)
                val shotsInCondition = filterGroupExt.shots()
                if (firstIteration) {
                    shotsFromChild.putAll(shotsInCondition.associateBy { it.id }.toMutableMap())
                    firstIteration = false
                } else {
                    val tmp: MutableMap<Long, Shot> = mutableMapOf()
                    shotsFromChild.forEach { (id, shot) ->
                        if (shotsInCondition.map { it.id }.contains(id)) tmp[id] = shot
                    }
                    shotsFromChild.clear()
                    shotsFromChild.putAll(tmp)
                }
            }
        } else {
            filter.filterGroups.forEach { filterGroup ->
                val filterGroupExt = FilterGroupExt(filterGroup)
                val shotsInCondition = filterGroupExt.shots()
                shotsFromChild.putAll(shotsInCondition.associateBy { it.id }.toMutableMap())
            }
        }

        shots.addAll(shotsFromChild.values)

        return shots
    }

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