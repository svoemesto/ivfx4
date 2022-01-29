package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.File
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FileRepo : CrudRepository<File, Long> {
    fun findByProjectIdAndOrderGreaterThanOrderByOrder(projectId: Long, order: Int) : Iterable<File>
    fun findByProjectIdAndOrderLessThanOrderByOrderDesc(projectId: Long, order: Int): Iterable<File>

    @Query(value = "SELECT * FROM tbl_files WHERE project_id = ? ORDER BY order_file DESC LIMIT 1", nativeQuery = true)
    fun getEntityWithGreaterOrder(projectId:Long) : Iterable<File>

    fun findByProjectId(projectId: Long): Iterable<File>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_files WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_files WHERE id = ?", nativeQuery = true)
    fun delete(fileId:Long)

    @Query(value = "SELECT distinct tbl_files.* FROM tbl_files inner join tbl_shots on tbl_shots.file_id = tbl_files.id WHERE tbl_shots.id = ?", nativeQuery = true)
    fun getFileForShotId(shotId: Long): Iterable<File>


}