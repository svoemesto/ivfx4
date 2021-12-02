package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.ProjectCdf
import com.svoemesto.ivfx.repos.ProjectCdfRepo
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class ProjectCdfController(val repo: ProjectCdfRepo) {

    fun getProjectCdf(project: Project): ProjectCdf {
        val cdf = repo.findByProjectIdAndComputerId(project.id, Main.ccid).firstOrNull()
        return cdf ?: create(project)
    }

    fun create(project: Project): ProjectCdf {
        val entity = ProjectCdf()
        entity.project = project
        entity.computerId = Main.ccid
        repo.save(entity)
        return entity
    }

}