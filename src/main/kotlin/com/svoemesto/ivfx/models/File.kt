package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.Main
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
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_files")
class File {

    @NotNull(message = "ID файла не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    lateinit var project: Project

    @Column(name = "order_file", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

    @Column(name = "short_name", columnDefinition = "varchar(255) default ''")
    var shortName: String = ""

    //Список треков файла
    @OneToMany(mappedBy = "file", cascade = [CascadeType.ALL])
    @Fetch(value = FetchMode.SUBSELECT)
    var tracks: MutableList<Track> = mutableListOf()

    //Список фреймов файла

    @OneToMany(mappedBy = "file", cascade = [CascadeType.ALL])
    @Fetch(value = FetchMode.SUBSELECT)
    var frames: MutableList<Frame> = mutableListOf()

    @OneToMany(mappedBy = "file", cascade = [CascadeType.ALL])
    @Fetch(value = FetchMode.SUBSELECT)
    var cdfs: MutableList<FileCdf> = mutableListOf()

    var path: String
        get() = cdfs.firstOrNull { it.computerId == Main.ccid }?.path ?: ""
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
}