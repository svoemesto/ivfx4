package com.svoemesto.ivfx.models

import org.springframework.stereotype.Component
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_projects_cdf")
class ProjectCdf {

    @NotNull(message = "ID projects_cdf не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @Column(name = "computer_id", columnDefinition = "int default 0")
    var computerId: Int = 0

    @Column(name = "folder", columnDefinition = "varchar(255) default ''")
    var folder: String = ""

}