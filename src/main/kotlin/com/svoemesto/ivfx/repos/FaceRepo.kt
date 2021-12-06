package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Face
import com.svoemesto.ivfx.models.Frame
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FaceRepo : CrudRepository<Face, Long> {
    fun findByFileId(fileId: Long): Iterable<Face>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_faces WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_faces WHERE id = ?", nativeQuery = true)
    fun delete(frameId:Long)

    fun findByFileIdAndFrameNumber(fileId: Long, frameNumber: Int): Iterable<Face>
    fun findByFileIdAndFrameNumberAndFaceNumberInFrame(fileId: Long, frameNumber: Int, faceNumberInFrame: Int): Iterable<Face>

}