package com.svoemesto.ivfx.enums

enum class Folders(val propertyCdfKey: String, val folderName: String, val forProject: Boolean, val forFile: Boolean) {
    LOSSLESS("folder_lossless", "Lossless", true, true),
    PREVIEW("folder_preview", "Preview", true, true),
    SHOTS("folder_shots", "Shots", true, true),
    FAVORITES("folder_favorites", "Favorites", true, true),
    FRAMES_SMALL("folder_frames_small", "Frames_Small", true, true),
    FRAMES_MEDIUM("folder_frames_medium", "Frames_Medium", true, true),
    FRAMES_FULL("folder_frames_full", "Frames_Full", true, true),
    PERSONS("folder_persons", "Persons", true, false)
}