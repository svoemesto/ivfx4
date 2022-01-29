package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.Scene
import com.svoemesto.ivfx.models.Shot
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ShotRepo : CrudRepository<Shot, Long> {
    fun findByFileId(fileId: Long): Iterable<Shot>

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots WHERE file_id = ?", nativeQuery = true)
    fun deleteAll(fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots WHERE id = ?", nativeQuery = true)
    fun delete(frameId:Long)

    fun findByFileIdAndFirstFrameNumberGreaterThanOrderByFirstFrameNumber(fileId: Long, firstFrameNumber: Int): Iterable<Shot>

    fun findByFileIdAndFirstFrameNumberAndLastFrameNumber(
        fileId: Long,
        firstFrameNumber: Int,
        lastFrameNumber: Int
    ): Iterable<Shot>

    @Query(value = "select tsh.* from tbl_shots as tsh " +
            "inner join tbl_scenes as tsc on (tsh.file_id = tsc.file_id and tsh.first_frame_number >= tsc.first_frame_number and tsh.last_frame_number <= tsc.last_frame_number) " +
            "where tsc.id = ?", nativeQuery = true)
    fun getShotsForScenes(sceneId:Long) : Iterable<Shot>

    @Query(value = "select tsh.* from tbl_shots as tsh " +
            "inner join tbl_events as tev on (tsh.file_id = tev.file_id and tsh.first_frame_number >= tev.first_frame_number and tsh.last_frame_number <= tev.last_frame_number) " +
            "where tev.id = ?", nativeQuery = true)
    fun getShotsForEvents(eventId:Long) : Iterable<Shot>

    @Query(value = "select tsh.* from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "where tstc.computer_id = ?", nativeQuery = true)
    fun getShotsForShotsTmp(computerId:Int) : Iterable<Shot>

    @Query(value = "select distinct tsh.* from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "inner join tbl_frames as tf on (tsh.file_id = tf.file_id and tsh.first_frame_number <= tf.frame_number and tsh.last_frame_number >= tf.frame_number) " +
            "inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number) " +
            "where tstc.computer_id = ?1 and tfc.person_id = ?2", nativeQuery = true)
    fun getShotsForShotsTmpAndPerson(computerId:Int, personId: Long) : Iterable<Shot>

    @Query(value = "select distinct tsh.id from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "inner join tbl_frames as tf on (tsh.file_id = tf.file_id and tsh.first_frame_number <= tf.frame_number and tsh.last_frame_number >= tf.frame_number) " +
            "inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number) " +
            "where tstc.computer_id = ?1 and tfc.person_id = ?2", nativeQuery = true)
    fun getShotsIdsForShotsTmpAndPerson(computerId:Int, personId: Long) : Iterable<Long>

    @Query(value = "select distinct tsh.* from tbl_shots as tsh " +
            "inner join " +
            "(select distinct tsc.* from tbl_scenes as tsc " +
            "inner join " +
            "(select distinct tsh.* from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "inner join tbl_frames as tf on (tsh.file_id = tf.file_id and tsh.first_frame_number <= tf.frame_number and tsh.last_frame_number >= tf.frame_number) " +
            "inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number) " +
            "where tstc.computer_id = ?1 and tfc.person_id = ?2) " +
            "as sssh on (sssh.file_id = tsc.file_id and sssh.first_frame_number >= tsc.first_frame_number and sssh.last_frame_number <= tsc.last_frame_number)) " +
            "as sssc on (tsh.file_id = sssc.file_id and tsh.first_frame_number >= sssc.first_frame_number and tsh.last_frame_number <= sssc.last_frame_number)",
        nativeQuery = true)
    fun getShotsForScenesTmpAndPerson(computerId:Int, personId: Long) : Iterable<Shot>

    @Query(value = "select distinct tsh.id from tbl_shots as tsh " +
            "inner join " +
            "(select distinct tsc.* from tbl_scenes as tsc " +
            "inner join " +
            "(select distinct tsh.* from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "inner join tbl_frames as tf on (tsh.file_id = tf.file_id and tsh.first_frame_number <= tf.frame_number and tsh.last_frame_number >= tf.frame_number) " +
            "inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number) " +
            "where tstc.computer_id = ?1 and tfc.person_id = ?2) " +
            "as sssh on (sssh.file_id = tsc.file_id and sssh.first_frame_number >= tsc.first_frame_number and sssh.last_frame_number <= tsc.last_frame_number)) " +
            "as sssc on (tsh.file_id = sssc.file_id and tsh.first_frame_number >= sssc.first_frame_number and tsh.last_frame_number <= sssc.last_frame_number)",
        nativeQuery = true)
    fun getShotsIdsForScenesTmpAndPerson(computerId:Int, personId: Long) : Iterable<Long>


    @Query(value = "select distinct tsh.* from tbl_shots as tsh " +
            "inner join " +
            "(select distinct tev.* from tbl_events as tev " +
            "inner join " +
            "(select distinct tsh.* from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "inner join tbl_frames as tf on (tsh.file_id = tf.file_id and tsh.first_frame_number <= tf.frame_number and tsh.last_frame_number >= tf.frame_number) " +
            "inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number) " +
            "where tstc.computer_id = ?1 and tfc.person_id = ?2) " +
            "as sssh on (sssh.file_id = tev.file_id and sssh.first_frame_number >= tev.first_frame_number and sssh.last_frame_number <= tev.last_frame_number)) " +
            "as ssev on (tsh.file_id = ssev.file_id and tsh.first_frame_number >= ssev.first_frame_number and tsh.last_frame_number <= ssev.last_frame_number)",
        nativeQuery = true)
    fun getShotsForEventsTmpAndPerson(computerId:Int, personId: Long) : Iterable<Shot>

    @Query(value = "select distinct tsh.id from tbl_shots as tsh " +
            "inner join " +
            "(select distinct tev.* from tbl_events as tev " +
            "inner join " +
            "(select distinct tsh.* from tbl_shots as tsh " +
            "inner join tbl_shots_tmp_cdf as tstc on tsh.id = tstc.shot_id " +
            "inner join tbl_frames as tf on (tsh.file_id = tf.file_id and tsh.first_frame_number <= tf.frame_number and tsh.last_frame_number >= tf.frame_number) " +
            "inner join tbl_faces as tfc on (tsh.file_id = tfc.file_id and tf.frame_number = tfc.frame_number) " +
            "where tstc.computer_id = ?1 and tfc.person_id = ?2) " +
            "as sssh on (sssh.file_id = tev.file_id and sssh.first_frame_number >= tev.first_frame_number and sssh.last_frame_number <= tev.last_frame_number)) " +
            "as ssev on (tsh.file_id = ssev.file_id and tsh.first_frame_number >= ssev.first_frame_number and tsh.last_frame_number <= ssev.last_frame_number)",
        nativeQuery = true)
    fun getShotsIdsForEventsTmpAndPerson(computerId:Int, personId: Long) : Iterable<Long>

    @Query(value = "select * from tbl_shots where tbl_shots.id in ?1", nativeQuery = true)
    fun findByIds(shotsIds: Set<Long>): Iterable<Shot>

}