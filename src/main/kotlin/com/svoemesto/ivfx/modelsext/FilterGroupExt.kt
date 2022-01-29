package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.models.Shot

class FilterGroupExt(var filterGroup: FilterGroup): Comparable<FilterGroupExt> {

    override fun compareTo(other: FilterGroupExt): Int {
        return this.filterGroup.order - other.filterGroup.order
    }

    val name: String get() = filterGroup.name
    val order: Int get() = filterGroup.order
    val isAndText: String get() = if (filterGroup.isAnd) "&&" else "||"

    fun shotsIds(): Set<Long> {
        val shotsIds: MutableSet<Long> = mutableSetOf()
        val shotsIdsFromChild: MutableSet<Long> = mutableSetOf()
        if (filterGroup.isAnd) {
            var firstIteration = true
            filterGroup.filterConditions.forEach { filterCondition->
                val filterConditionExt = FilterConditionExt(filterCondition)
                val shotsIdsInCondition = filterConditionExt.shotsIds()
                if (firstIteration) {
                    shotsIdsFromChild.addAll(shotsIdsInCondition)
                    firstIteration = false
                } else {
                    shotsIdsFromChild.retainAll(shotsIdsInCondition)
                }
            }
        } else {
            filterGroup.filterConditions.forEach { filterCondition ->
                val filterConditionExt = FilterConditionExt(filterCondition)
                val shotsIdsInCondition = filterConditionExt.shotsIds()
                shotsIdsFromChild.addAll(shotsIdsInCondition)
            }
        }

        shotsIds.addAll(shotsIdsFromChild)

        return shotsIds

    }

    fun shots(): Set<Shot> {
        val shots: MutableSet<Shot> = mutableSetOf()
        val shotsFromChild: MutableMap<Long, Shot> = mutableMapOf()
        if (filterGroup.isAnd) {
            var firstIteration = true
            filterGroup.filterConditions.forEach { filterCondition->
                val filterConditionExt = FilterConditionExt(filterCondition)
                val shotsInCondition = filterConditionExt.shots()
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
            filterGroup.filterConditions.forEach { filterCondition ->
                val filterConditionExt = FilterConditionExt(filterCondition)
                val shotsInCondition = filterConditionExt.shots()
                shotsFromChild.putAll(shotsInCondition.associateBy { it.id }.toMutableMap())
            }
        }

        shots.addAll(shotsFromChild.values)

        return shots
    }

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