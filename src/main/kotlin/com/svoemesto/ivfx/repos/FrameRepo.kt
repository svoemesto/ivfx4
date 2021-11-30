package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Frame
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FrameRepo : CrudRepository<Frame, Long> {
    fun findByFileId(fileId: Long): Iterable<Frame>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_frames WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_frames WHERE id = ?", nativeQuery = true)
    fun delete(frameId:Long)

}