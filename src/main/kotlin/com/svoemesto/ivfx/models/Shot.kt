package com.svoemesto.ivfx.models

import com.svoemesto.ivfx.enums.ShotTypePerson
import com.svoemesto.ivfx.enums.ShotTypeSize
import com.svoemesto.ivfx.modelsext.ShotExt
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
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Component
@Entity
@Table(name = "tbl_shots")
@Transactional
class Shot: Comparable<Shot> {

    override fun compareTo(other: Shot): Int {
        return this.firstFrameNumber - other.firstFrameNumber
    }

    @NotNull(message = "ID плана не может быть NULL")
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    lateinit var file: File

    @Column(name = "shot_type_person", columnDefinition = "int default 0")
    var typePerson: ShotTypePerson = ShotTypePerson.NONE

    @Column(name = "first_frame_number", columnDefinition = "int default 0")
    var firstFrameNumber: Int = 0

    @Column(name = "last_frame_number", columnDefinition = "int default 0")
    var lastFrameNumber: Int = 0

    @Column(name = "nearest_i_frame", columnDefinition = "int default 0")
    var nearestIFrame: Int = 0

    @OneToMany(mappedBy = "scene", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var scenesShots: MutableSet<SceneShot> = mutableSetOf()

    @OneToMany(mappedBy = "event", cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @Fetch(value = FetchMode.SUBSELECT)
    var eventsShots: MutableSet<EventShot> = mutableSetOf()

    val isBodyScene: Boolean
        get() {
            scenesShots.forEach { sceneShotForShot->
                val list = sceneShotForShot.scene.scenesShots.map {it.shot}.toMutableList()
                list.sort()
                if (list.contains(this) && this != list.firstOrNull() && this != list.lastOrNull()) return true
            }
            return false
        }

    val isStartScene: Boolean
        get() {
            scenesShots.forEach { sceneShotForShot->
                val list = sceneShotForShot.scene.scenesShots.map {it.shot}.toMutableList()
                list.sort()
                if (this == list.firstOrNull()) return true
            }
            return false
        }

    val isEndScene: Boolean
        get() {
            scenesShots.forEach { sceneShotForShot->
                val list = sceneShotForShot.scene.scenesShots.map {it.shot}.toMutableList()
                list.sort()
                if (this == list.lastOrNull()) return true
            }
            return false
        }

    val isBodyEvent: Boolean
        get() {
            eventsShots.forEach { sceneShotForShot->
                val list = sceneShotForShot.event.eventsShots.map {it.shot}.toMutableList()
                list.sort()
                if (list.contains(this) && this != list.firstOrNull() && this != list.lastOrNull()) return true
            }
            return false
        }

    val isStartEvent: Boolean
        get() {
            eventsShots.forEach { sceneShotForShot->
                val list = sceneShotForShot.event.eventsShots.map {it.shot}.toMutableList()
                list.sort()
                if (this == list.firstOrNull()) return true
            }
            return false
        }

    val isEndEvent: Boolean
        get() {
            eventsShots.forEach { sceneShotForShot->
                val list = sceneShotForShot.event.eventsShots.map {it.shot}.toMutableList()
                list.sort()
                if (this == list.lastOrNull()) return true
            }
            return false
        }

}