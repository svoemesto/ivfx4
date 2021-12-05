package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.Main
import org.hibernate.Hibernate
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
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_files")
@Transactional
class File {

    @NotNull(message = "ID файла не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @Column(name = "order_file", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "short_name", columnDefinition = "varchar(255) default ''")
    var shortName: String = ""

    @OneToMany(mappedBy = "file", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var tracks: MutableList<Track> = mutableListOf()

    @OneToMany(mappedBy = "file", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var frames: MutableList<Frame> = mutableListOf()

    @OneToMany(mappedBy = "file", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var faces: MutableList<Face> = mutableListOf()

    @OneToMany(mappedBy = "file", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var cdfs: MutableList<FileCdf> = mutableListOf()

    @OneToMany(mappedBy = "file", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var shots: MutableList<Shot> = mutableListOf()

    var path: String
        get() {
            return cdfs.firstOrNull { it.computerId == Main.ccid }?.path ?: ""
        }
        set(value) {
            var cdf: FileCdf? = cdfs.firstOrNull { it.computerId == Main.ccid }
            if (cdf != null) {
                cdf.path = value
            } else {
                cdf = FileCdf()
                cdf.file = this
                cdf.computerId = Main.ccid
                cdf.path = value
                cdfs.add(cdf)
            }
        }

    @Transient var folderPreview: String = ""
    @Transient var folderLossless: String = ""
    @Transient var folderFavorites: String = ""
    @Transient var folderShots: String = ""
    @Transient var folderFramesSmall: String = ""
    @Transient var folderFramesMedium: String = ""
    @Transient var folderFramesFull: String = ""
    @Transient var fps: Double = 23.976
    @Transient var framesCount: Int = 0
}