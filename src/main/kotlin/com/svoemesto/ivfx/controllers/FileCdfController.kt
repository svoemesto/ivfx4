package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.FileCdf
import com.svoemesto.ivfx.repos.FileCdfRepo
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class FileCdfController() {

    fun getFileCdf(file: File): FileCdf {
        val cdf = Main.fileCdfRepo.findByFileIdAndComputerId(file.id, Main.ccid).firstOrNull()
        if (cdf != null) cdf.file = file
        return cdf ?: create(file)
    }


    fun save(fileCdf: FileCdf) {
        Main.fileCdfRepo.save(fileCdf)
    }

    fun delete(fileCdf: FileCdf) {
        Main.propertyController.deleteAll(fileCdf::class.java.simpleName, fileCdf.id)
        Main.propertyCdfController.deleteAll(fileCdf::class.java.simpleName, fileCdf.id)
        Main.fileCdfRepo.delete(fileCdf)
    }

    fun deleteAll(file: File) {
        Main.fileCdfRepo.findByFileId(file.id).forEach { fileCdf ->
            delete(fileCdf)
        }
    }

    fun create(file: File): FileCdf {
        val entity = FileCdf()
        entity.file = file
        entity.computerId = Main.ccid
        save(entity)
        return entity
    }

}