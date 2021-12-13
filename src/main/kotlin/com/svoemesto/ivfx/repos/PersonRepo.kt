package com.svoemesto.ivfx.repos

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

}