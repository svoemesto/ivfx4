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
    var folderFacesFull: String = ProjectController.getFolderFacesFull(project)
    var folderFacesPreview: String = ProjectController.getFolderFacesPreview(project)
    var folderShotsCompressedWithAudio: String = ProjectController.getFolderShotsCompressedWithAudio(project)
    var folderShotsLosslessWithAudio: String = ProjectController.getFolderShotsLosslessWithAudio(project)
    var folderShotsLosslessWithoutAudio: String = ProjectController.getFolderShotsLosslessWithoutAudio(project)
    var folderPersons: String = ProjectController.getFolderPersons(project)
}