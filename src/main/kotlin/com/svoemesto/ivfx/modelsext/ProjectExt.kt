package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.controllers.ProjectController
import com.svoemesto.ivfx.models.Project

data class ProjectExt(var project: Project) {
    var folderPreview: String = ProjectController.getFolderPreview(project)
    var folderLossless: String = ProjectController.getFolderLossless(project)
    var folderFavorites: String = ProjectController.getFolderFavorites(project)
    var folderShots: String = ProjectController.getFolderShots(project)
    var folderFramesSmall: String = ProjectController.getFolderFramesSmall(project)
    var folderFramesMedium: String = ProjectController.getFolderFramesMedium(project)
    var folderFramesFull: String = ProjectController.getFolderFramesFull(project)
}