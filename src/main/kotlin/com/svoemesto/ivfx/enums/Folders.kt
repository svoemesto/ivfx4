package com.svoemesto.ivfx.enums

enum class Folders(val propertyCdfKey: String, val folderName: String) {
    LOSSLESS("folder_lossless", "Lossless"),
    PREVIEW("folder_preview", "Preview"),
    SHOTS("folder_shots", "Shots"),
    FAVORITES("folder_favorites", "Favorites"),
    FRAMES_SMALL("folder_frames_small", "Frames_Small"),
    FRAMES_MEDIUM("folder_frames_medium", "Frames_Medium"),
    FRAMES_FULL("folder_frames_full", "Frames_Full")
}