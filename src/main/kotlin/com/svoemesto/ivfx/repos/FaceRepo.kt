package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Face
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface FaceRepo : CrudRepository<Face, Long> {
    fun findByFileId(fileId: Long): Iterable<Face>
    fun findByFileIdAndPersonId(fileId: Long, personId: Long): Iterable<Face>

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

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id != ?2", nativeQuery = true)
    fun findByFileIdAndPersonIdNotEqual(fileId: Long, personId: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ? AND is_confirmed = false", nativeQuery = true)
    fun findByFileIdAndNotConfirmed(fileId: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces INNER JOIN tbl_files ON tbl_faces.file_id = tbl_files.id WHERE tbl_files.project_id = ? AND tbl_faces.is_confirmed = true", nativeQuery = true)
    fun getListFacesToTrain(projectId: Long): Iterable<Face>

}