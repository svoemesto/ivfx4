package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Filter
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FilterRepo : CrudRepository<Filter, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_filters WHERE id = ?", nativeQuery = true)
    fun delete(filterId:Long)

}