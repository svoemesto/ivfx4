package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.FileCdf
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FileCdfRepo : CrudRepository<FileCdf, Long> {
    fun findByFileId(fileId: Long): Iterable<FileCdf>
    fun findByFileIdAndComputerId(projectId: Long, computerId: Int): Iterable<FileCdf>


    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_files_cdf WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_files_cdf WHERE id = ?", nativeQuery = true)
    fun delete(fileCdfId:Long)

}