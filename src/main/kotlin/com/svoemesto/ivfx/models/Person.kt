package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.PersonType
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_persons")
@Transactional
class Person: Comparable<Person> {

    override fun compareTo(other: Person): Int {
        return this.name.compareTo (other.name)
    }

    @NotNull(message = "ID Person не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @OneToMany(mappedBy = "person", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var faces: MutableSet<Face> = mutableSetOf()

    @Column(name = "person_type", nullable = false, columnDefinition = "int default 0")
    var personType: PersonType = PersonType.PERSON

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "name_in_recognizer", columnDefinition = "varchar(255) default ''")
    var nameInRecognizer: String = ""

    @Column(name = "file_id_for_preview", nullable = false, columnDefinition = "int default 0")
    var fileIdForPreview: Long = 0

    @Column(name = "frame_number_for_preview", nullable = false, columnDefinition = "int default 0")
    var frameNumberForPreview: Int = 0

    @Column(name = "face_number_for_preview", nullable = false, columnDefinition = "int default 0")
    var faceNumberForPreview: Int = 0

    @Column(name = "uuid", columnDefinition = "varchar(255) default ''")
    var uuid: String = UUID.randomUUID().toString()

}