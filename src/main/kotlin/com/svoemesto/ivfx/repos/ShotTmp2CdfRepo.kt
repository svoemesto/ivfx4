package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.ShotTmp2Cdf
import com.svoemesto.ivfx.models.ShotTmpCdf
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ShotTmp2CdfRepo : CrudRepository<ShotTmp2Cdf, Long> {
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots_tmp2_cdf WHERE computer_id = ?", nativeQuery = true)
    fun deleteAll(computerId:Int)

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO tbl_shots_tmp2_cdf (computer_id, shot_id, file_id, project_id) select ?1, tbl_shots.id, tbl_shots.file_id, tbl_files.project_id from tbl_shots inner join tbl_files on tbl_shots.file_id = tbl_files.id where tbl_shots.file_id = ?2", nativeQuery = true)
    fun addAllByFileId(computerId:Int, fileId:Long)

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO tbl_shots_tmp2_cdf (computer_id, shot_id, file_id, project_id) select ?1, tbl_shots.id, tbl_shots.file_id, tbl_files.project_id from tbl_shots inner join tbl_files on tbl_shots.file_id = tbl_files.id where tbl_shots.id = ?2", nativeQuery = true)
    fun addByShotId(computerId:Int, shotId:Long)

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO tbl_shots_tmp2_cdf (computer_id, shot_id, file_id, project_id) select ?1, tbl_shots.id, tbl_shots.file_id, tbl_files.project_id from tbl_shots inner join tbl_files on tbl_shots.file_id = tbl_files.id where tbl_shots.id in ?2", nativeQuery = true)
    fun addByShotIds(computerId:Int, shotIds:Set<Long>)

    fun findByComputerId(computerId: Int): Iterable<ShotTmp2Cdf>

}