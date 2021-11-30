package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.FileCdf
import com.svoemesto.ivfx.repos.FileCdfRepo
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class FileCdfController(val repo: FileCdfRepo) {

    fun getFileCdf(file: File): FileCdf {
        val cdf = repo.findByFileIdAndComputerId(file.id, Main.ccid).firstOrNull()
        return cdf ?: create(file)
    }

    fun create(file: File): FileCdf {
        val entity = FileCdf()
        entity.file = file
        entity.computerId = Main.ccid
        repo.save(entity)
        return entity
    }

}