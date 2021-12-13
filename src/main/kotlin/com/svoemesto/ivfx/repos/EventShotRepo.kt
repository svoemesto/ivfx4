package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.EventShot
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.SceneShot
import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface EventShotRepo : CrudRepository<EventShot, Long> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events_shots WHERE event_id = ?", nativeQuery = true)
    fun deleteAllForEvent(sceneId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events_shots WHERE shot_id = ?", nativeQuery = true)
    fun deleteAllForShot(shotId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_events_shots WHERE id = ?", nativeQuery = true)
    fun delete(eventShotId:Long)

    @Query(value = "SELECT * FROM tbl_events_shots WHERE shot_id = ?", nativeQuery = true)
    fun getEventsShotsForShot(shotId:Long) : Iterable<EventShot>

    @Query(value = "SELECT * FROM tbl_events_shots WHERE event_id = ?", nativeQuery = true)
    fun getEventsShotsForEvent(eventId:Long) : Iterable<EventShot>

    @Query(value = "SELECT tbl_events_shots.* FROM tbl_events_shots INNER JOIN tbl_events ON tbl_events.id = tbl_events_shots.event_id WHERE tbl_events.file_id = ?", nativeQuery = true)
    fun getEventsShotsForFile(fileId:Long) : Iterable<EventShot>

}