package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.models.ShotTmp2Cdf
import com.svoemesto.ivfx.models.ShotTmpCdf
import org.springframework.stereotype.Controller

@Controller
class ShotTmp2CdfController {
    companion object {

        fun save(shotTmp2Cdf: ShotTmp2Cdf) {
            Main.shotTmp2CdfRepo.save(shotTmp2Cdf)
        }

        fun deleteAll() {
            Main.shotTmp2CdfRepo.deleteAll(Main.ccid)
        }

        fun create(shot: Shot): ShotTmp2Cdf {
            val entity = ShotTmp2Cdf()
            entity.shotId = shot.id
            entity.computerId = Main.ccid
            entity.fileId = shot.file.id
            entity.projectId = shot.file.project.id
            save(entity)
            return entity
        }

    }
}