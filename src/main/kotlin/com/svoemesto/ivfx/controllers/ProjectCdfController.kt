package com.svoemesto.ivfx.controllers

import com.svoemesto.ivfx.Main
import com.svoemesto.ivfx.models.File
import com.svoemesto.ivfx.models.Project
import com.svoemesto.ivfx.models.ProjectCdf
import org.springframework.stereotype.Controller

@Controller
//@Scope("prototype")
class ProjectCdfController() {

    companion object {

        fun getProjectCdf(project: Project): ProjectCdf {
            val cdf = Main.projectCdfRepo.findByProjectIdAndComputerId(project.id, Main.ccid).firstOrNull()
            if (cdf != null) cdf.project = project
            return cdf ?: create(project)
        }

        fun create(project: Project): ProjectCdf {
            val entity = ProjectCdf()
            entity.project = project
            entity.computerId = Main.ccid
            Main.projectCdfRepo.save(entity)
            return entity
        }

        fun save(projectCdf: ProjectCdf) {
            Main.projectCdfRepo.save(projectCdf)
        }

        fun delete(projectCdf: ProjectCdf) {
            PropertyController.deleteAll(projectCdf::class.java.simpleName, projectCdf.id)
            PropertyCdfController.deleteAll(projectCdf::class.java.simpleName, projectCdf.id)
            Main.projectCdfRepo.delete(projectCdf)
        }

        fun deleteAll(project: Project) {
            Main.projectCdfRepo.findByProjectId(project.id).forEach { projectCdf ->
                delete(projectCdf)
            }
        }
    }



}