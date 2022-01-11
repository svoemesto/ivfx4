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

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 LIMIT 1", nativeQuery = true)
    fun getFirstByFileId(fileId: Long): Iterable<Face>

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

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id != ?2 LIMIT 1", nativeQuery = true)
    fun findFirstByFileIdAndPersonIdNotEqual(fileId: Long, personId: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND person_id = ?2", nativeQuery = true)
    fun findFacesToRecognize(fileId: Long, idPersonUnrecognized: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces INNER JOIN tbl_files ON tbl_faces.file_id = tbl_files.id WHERE tbl_files.project_id = ?1 AND tbl_faces.is_example = true", nativeQuery = true)
    fun getListFacesToTrain(projectId: Long): Iterable<Face>

    @Query(value = "SELECT * FROM tbl_faces WHERE file_id = ?1 AND frame_number = ?2", nativeQuery = true)
    fun getListFacesInFrame(fileId: Long, frameNumber: Int): Iterable<Face>

    @Query(value = "select tbl_faces.* from tbl_faces inner join tbl_files as tf on tbl_faces.file_id = tf.id " +
            "where tf.project_id = ?1 and tbl_faces.person_id = ?2 and " +
            "(tbl_faces.is_example != ?3 or tbl_faces.is_example = ?4) and  " +
            "(tbl_faces.is_manual != ?5 or tbl_faces.is_manual = ?6)", nativeQuery = true)
    fun findByProjectIdAndPersonId(
        projectId: Long,
        personId: Long,
        loadNotExample: Boolean,
        loadExample: Boolean,
        loadNotManual: Boolean,
        loadManual: Boolean
    ): Iterable<Face>

    @Query(value = "select tbl_faces.* from tbl_faces " +
            "where tbl_faces.file_id = ?1 and tbl_faces.person_id = ?2 and " +
            "(tbl_faces.is_example != ?3 or tbl_faces.is_example = ?4) and " +
            "(tbl_faces.is_manual != ?5 or tbl_faces.is_manual = ?6)", nativeQuery = true)
    fun findByFileIdAndPersonId(
        fileId: Long,
        personId: Long,
        loadNotExample: Boolean,
        loadExample: Boolean,
        loadNotManual: Boolean,
        loadManual: Boolean
    ): Iterable<Face>

    @Query(value = "select tbl_faces.* from tbl_faces inner join tbl_files as tf on tbl_faces.file_id = tf.id " +
            "inner join tbl_shots ts on tf.id = ts.file_id " +
            "where ts.id = ?1 and tbl_faces.person_id = ?2 and " +
            "(tbl_faces.is_example != ?3 or tbl_faces.is_example = ?4) and " +
            "(tbl_faces.is_manual != ?5 or tbl_faces.is_manual = ?6) and " +
            "(tbl_faces.frame_number >= ts.first_frame_number and " +
            "tbl_faces.frame_number <= ts.last_frame_number)", nativeQuery = true)
    fun findByShotIdAndPersonId(
        shotId: Long,
        personId: Long,
        loadNotExample: Boolean,
        loadExample: Boolean,
        loadNotManual: Boolean,
        loadManual: Boolean
    ): Iterable<Face>


}