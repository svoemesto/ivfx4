package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ShotRepo : CrudRepository<Shot, Long> {
    fun findByFileId(fileId: Long): Iterable<Shot>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots WHERE id = ?", nativeQuery = true)
    fun delete(frameId:Long)

    fun findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileId: Long, firstFrameNumber: Int): Iterable<Shot>

    fun findByFileIdAndFirstFrameNumberAndLastFrameNumber(
        fileId: Long,
        firstFrameNumber: Int,
        lastFrameNumber: Int
    ): Iterable<Shot>

}