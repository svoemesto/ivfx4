package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.FilterCondition
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FilterConditionRepo : CrudRepository<FilterCondition, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_conditions WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_conditions WHERE id = ?", nativeQuery = true)
    fun delete(filterConditionId:Long)

}