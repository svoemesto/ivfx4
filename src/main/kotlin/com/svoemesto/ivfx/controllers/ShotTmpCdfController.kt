package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Shot
import com.svoemesto.ivfx.models.ShotTmpCdf
import com.svoemesto.ivfx.repos.ShotTmpCdfRepo
import org.springframework.stereotype.Controller

@Controller
class ShotTmpCdfController {
    companion object {

        fun save(shotTmpCdf: ShotTmpCdf) {
            Main.shotTmpCdfRepo.save(shotTmpCdf)
        }

        fun deleteAll() {
            Main.shotTmpCdfRepo.deleteAll(Main.ccid)
        }

        fun create(shot: Shot): ShotTmpCdf {
            val entity = ShotTmpCdf()
            entity.shotId = shot.id
            entity.computerId = Main.ccid
            save(entity)
            return entity
        }

    }
}