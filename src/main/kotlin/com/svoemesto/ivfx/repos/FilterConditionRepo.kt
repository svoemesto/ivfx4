package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.FilterCondition
import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.FilterConditionExt
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FilterConditionRepo : CrudRepository<FilterCondition, Long> {

    fun findByFilterGroupIdAndOrderGreaterThanOrderByOrder(projectId: Long, order: Int) : Iterable<FilterCondition>
    fun findByFilterGroupIdAndOrderLessThanOrderByOrderDesc(projectId: Long, order: Int): Iterable<FilterCondition>

    @Query(value = "SELECT * FROM tbl_filters_conditions WHERE filter_group_id = ? ORDER BY order_filter_condition DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(filterGroupId:Long) : Iterable<FilterCondition>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_conditions WHERE filter_group_id = ?", nativeQuery = true)
    fun deleteAll(filterGroupId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_conditions WHERE id = ?", nativeQuery = true)
    fun delete(filterConditionId:Long)
    fun findByFilterGroupId(filterGroupId: Long): Iterable<FilterCondition>

}