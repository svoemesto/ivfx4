package com.svoemesto.ivfx.repos

import com.svoemesto.ivfx.models.ShotTmpCdf
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
interface ShotTmpCdfRepo : CrudRepository<ShotTmpCdf, Long> {
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM tbl_shots_tmp_cdf WHERE computer_id = ?", nativeQuery = true)
    fun deleteAll(computerId:Int)

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO tbl_shots_tmp_cdf (computer_id, shot_id) select ?1, id from tbl_shots where tbl_shots.file_id = ?2", nativeQuery = true)
    fun addAllByFileId(computerId:Int, fileId:Long)

}