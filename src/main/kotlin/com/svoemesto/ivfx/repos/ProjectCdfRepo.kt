package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.ProjectCdf
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ProjectCdfRepo : CrudRepository<ProjectCdf, Long> {
    fun findByProjectId(projectId: Long): Iterable<ProjectCdf>
    fun findByProjectIdAndComputerId(projectId: Long, computerId: Int): Iterable<ProjectCdf>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_projects_cdf WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_projects_cdf WHERE id = ?", nativeQuery = true)
    fun delete(projectCdfId:Long)
}