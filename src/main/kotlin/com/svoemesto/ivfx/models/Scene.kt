package com.svoemesto.ivfx.models

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull


@Component
@Entity
@Table(name = "tbl_scenes")
@Transactional
class Scene {

    @NotNull(message = "ID Scene не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @OneToMany(mappedBy = "scene", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var scenesShots: MutableSet<SceneShot> = mutableSetOf()

}