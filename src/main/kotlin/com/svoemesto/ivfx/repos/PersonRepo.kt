package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.enums.PersonType
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Person
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface PersonRepo : CrudRepository<Person, Long> {

    fun findByProjectId(projectId: Long): Iterable<Person>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_persons WHERE project_id = ?", nativeQuery = true)
    fun deleteAll(projectId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_persons WHERE id = ?", nativeQuery = true)
    fun delete(personId:Long)

    fun findByProjectIdAndNameInRecognizer(projectId: Long, nameFaceInRecognizer: String): Iterable<Person>

//    @Query(value = "select * FROM tbl_persons WHERE project_id = ?1 AND person_type = ?2", nativeQuery = true)
//    fun findByProjectIdAndPersonTypeId(project_id: Long, personTypeId: Int): Iterable<Person>
    fun findByProjectIdAndPersonType(projectId: Long, personType: PersonType): Iterable<Person>

    @Query(value = "select distinct tp.* from tbl_persons as tp inner join tbl_faces tf on tp.id = tf.person_id where tf.file_id = ?", nativeQuery = true)
    fun findByFileId(fileId: Long): Iterable<Person>

    @Query(value = "select distinct tp.* from tbl_persons as tp inner join tbl_faces tf on tp.id = tf.person_id " +
            "inner join tbl_shots ts on (tf.file_id = ts.file_id and tf.frame_number >= ts.first_frame_number and tf.frame_number <= ts.last_frame_number) " +
            "where ts.id = ?", nativeQuery = true)
    fun findByShotId(shotId: Long): Iterable<Person>

}