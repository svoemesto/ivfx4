package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.enums.AudioCodecs
import com.svoemesto.ivfx.enums.LosslessContainers
import com.svoemesto.ivfx.enums.LosslessVideoCodecs
import com.svoemesto.ivfx.enums.VideoCodecs
import com.svoemesto.ivfx.enums.VideoContainers
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
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_projects")
@Transactional
class Project {

    @NotNull(message = "ID проекта не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

    @Column(name = "order_project", nullable = false, columnDefinition = "int default 0")
    var order: Int = 0

    @Column(name = "name", columnDefinition = "varchar(255) default ''")
    var name: String = ""

//    @NotEmpty(message = "Короткое имя проекта не может быть пустым")
    @Column(name = "short_name", columnDefinition = "varchar(45) default ''")
    var shortName: String = ""

    @Column(name = "lossless_codec", columnDefinition = "varchar(255) default 'RAW'")
    var lossLessCodec: String = LosslessVideoCodecs.values().firstOrNull{it.default}?.name ?: ""

    @Column(name = "lossless_container", columnDefinition = "varchar(255) default 'MKV'")
    var lossLessContainer: String = LosslessContainers.values().firstOrNull{it.default}?.name ?: ""

    @Column(name = "container", columnDefinition = "varchar(255) default 'MP4'")
    var container: String = VideoContainers.values().firstOrNull{it.default}?.name ?: ""

    @Column(name = "video_codec", columnDefinition = "varchar(255) default 'X264'")
    var videoCodec: String = VideoCodecs.values().firstOrNull{it.default}?.name ?: ""

    @Column(name = "audio_codec", columnDefinition = "varchar(255) default 'AAC'")
    var audioCodec: String = AudioCodecs.values().firstOrNull{it.default}?.name ?: ""

    @Column(name = "width", columnDefinition = "int default 1920")
    var width: Int = 1920

    @Column(name = "height", columnDefinition = "int default 1080")
    var height: Int = 1080

    @Column(name = "fps")
    var fps: Double = 23.976

    @Column(name = "video_bitrate", columnDefinition = "int default 10000000")
    var videoBitrate: Int = 10_000_000

    @Column(name = "audio_bitrate", columnDefinition = "int default 320000")
    var audioBitrate: Int = 320_000

    @Column(name = "audio_frequency", columnDefinition = "int default 48000")
    var audioFrequency: Int = 48_000

    @OneToMany(mappedBy = "project", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var files: MutableList<File> = mutableListOf()

    @OneToMany(mappedBy = "project", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var persons: MutableList<Person> = mutableListOf()

    @OneToMany(mappedBy = "project", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var cdfs: MutableList<ProjectCdf> = mutableListOf()

    var folder: String
        get() {
            return cdfs.firstOrNull { it.computerId == Main.ccid }?.folder ?: ""
        }
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