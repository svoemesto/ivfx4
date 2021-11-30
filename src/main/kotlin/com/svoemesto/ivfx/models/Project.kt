package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.getCurrentComputerId
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import org.springframework.stereotype.Component
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Transient
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_projects")
class Project {

    @NotNull(message = "ID проекта не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long = 0L

    @Column(name = "order_project", nullable = false)
    var order: Int = 0

    @NotEmpty(message = "Имя проекта не может быть пустым")
    @NotNull(message = "Имя проекта не может быть NULL")
    @Column(name = "name")
    var name: String = ""

//    @NotEmpty(message = "Короткое имя проекта не может быть пустым")
    @NotNull(message = "Короткое имя проекта не может быть NULL")
    @Column(name = "short_name", length = 45)
    var shortName: String = ""

    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    var projectFiles: MutableList<File> = mutableListOf()

    @OneToMany(mappedBy = "project", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    var cdfs: MutableList<ProjectCdf> = mutableListOf()


//    @Transient
    var folder: String
        get() = cdfs.filter { it.computerId == Main.ccid }.firstOrNull()?.folder ?: ""
        set(value) {
            var cdf: ProjectCdf? = cdfs.filter { it.computerId == Main.ccid }.firstOrNull()
            if (cdf != null) {
                cdf.folder = value
            } else {
                cdf = ProjectCdf()
                cdf.project = this
                cdf.computerId = Main.ccid
                cdf.folder = value
                cdfs.add(cdf)
            }
        }
}