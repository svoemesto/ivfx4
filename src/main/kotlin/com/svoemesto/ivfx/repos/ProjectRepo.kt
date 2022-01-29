package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Project
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ProjectRepo : CrudRepository<Project, Long> {
    fun findByOrderGreaterThanOrderByOrder(order: Int) : Iterable<Project>
    fun findByOrderLessThanOrderByOrderDesc(order: Int): Iterable<Project>

    @Query(value = "SELECT * FROM tbl_projects ORDER BY order_project DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder() : Iterable<Project>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_projects WHERE id = ?", nativeQuery = true)
    fun delete(projectId: Long)

    @Query(value = "SELECT distinct tbl_projects.* FROM tbl_projects inner join tbl_files on tbl_files.project_id = tbl_projects.id WHERE tbl_files.id = ?", nativeQuery = true)
    fun getProjectForFileId(fileId: Long): Iterable<Project>
//    fun findById(projectId: Long) : Iterable<Project>

}