package com.svoemesto.ivfx.modelsext

import com.svoemesto.ivfx.controllers.FileController
import com.svoemesto.ivfx.models.File

data class FileExt(val file: File, val projectExt: ProjectExt) {
//    var projectExt: ProjectExt = ProjectExt(file.project)
    var fileName: String = file.name
    var fileOrder: Int = file.order
    var fps: Double = FileController.getFps(file)
    var framesCount: Int = FileController.getFramesCount(file)
    var folderPreview: String = FileController.getFolderPreview(this)
    var folderLossless: String = FileController.getFolderLossless(this)
    var folderFavorites: String = FileController.getFolderFavorites(this)
    var folderShots: String = FileController.getFolderShots(this)
    var folderFramesSmall: String = FileController.getFolderFramesSmall(this)
    var folderFramesMedium: String = FileController.getFolderFramesMedium(this)
    var folderFramesFull: String = FileController.getFolderFramesFull(this)
    var pathToLosslessFile: String = FileController.getLossless(this)
    var pathToPreviewFile: String = FileController.getPreview(this)
    var hasPreview: Boolean = FileController.hasPreview(this)
    var hasPreviewString: String = if (hasPreview) "✓" else "✗"
    var hasLossless: Boolean = FileController.hasLossless(this)
    var hasLosslessString: String = if (hasLossless) "✓" else "✗"
    var hasFramesSmall: Boolean = FileController.hasFramesSmall(this)
    var hasFramesSmallString: String = if (hasFramesSmall) "✓" else "✗"
    var hasFramesMedium: Boolean = FileController.hasFramesMedium(this)
    var hasFramesMediumString: String = if (hasFramesMedium) "✓" else "✗"
    var hasFramesFull: Boolean = FileController.hasFramesFull(this)
    var hasFramesFullString: String = if (hasFramesFull) "✓" else "✗"
    var hasAnalyzedFrames: Boolean = FileController.hasAnalyzedFrames(file)
    var hasAnalyzedFramesString: String = if (hasAnalyzedFrames) "✓" else "✗"
    var hasDetectedFaces: Boolean = FileController.hasDetectedFaces(this)
    var hasDetectedFacesString: String = if (hasDetectedFaces) "✓" else "✗"
    var hasCreatedFaces: Boolean = FileController.hasCreatedFaces(file)
    var hasCreatedFacesString: String = if (hasCreatedFaces) "✓" else "✗"

}