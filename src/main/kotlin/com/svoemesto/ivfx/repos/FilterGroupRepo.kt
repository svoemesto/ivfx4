package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.FilterGroup
import com.svoemesto.ivfx.modelsext.FilterGroupExt
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FilterGroupRepo : CrudRepository<FilterGroup, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_groups WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters_groups WHERE id = ?", nativeQuery = true)
    fun delete(filterGroupId:Long)
    fun findByProjectId(projectId: Long): Iterable<FilterGroup>

}